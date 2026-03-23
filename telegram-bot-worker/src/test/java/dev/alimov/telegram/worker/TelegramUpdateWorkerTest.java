package dev.alimov.telegram.worker;

import dev.alimov.telegram.api.Chat;
import dev.alimov.telegram.api.Message;
import dev.alimov.telegram.api.Update;
import dev.alimov.telegram.core.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TelegramUpdateWorkerTest {

    private static final Update SAMPLE_UPDATE = new Update(
            100L,
            new Message(1L, null, null, new Chat(123L, "private", null, null, null, null), 1000, "hello",
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
            null, null, null, null, null
    );

    private static final BotResponse.SendMessage SAMPLE_RESPONSE = new BotResponse.SendMessage(123L, "reply");

    /**
     * Simple in-memory queue — no Mockito needed
     */
    static class TestChannel<T> implements ReadableReactiveChannel<T>, WritableReactiveChannel<T> {
        private final List<T> published = new CopyOnWriteArrayList<>();
        private final Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer();

        @Override
        public Mono<Void> publish(T message) {
            published.add(message);
            sink.tryEmitNext(message);
            return Mono.empty();
        }

        @Override
        public Flux<T> subscribe() {
            return sink.asFlux();
        }

        @Override
        public void close() {

        }

        public List<T> getPublished() {
            return published;
        }
    }

    private TelegramUpdateWorker createWorker(
            ReadableReactiveChannel<Update> inbound,
            WritableReactiveChannel<BotResponse> outbound,
            UpdateHandler handler
    ) {
        return new TelegramUpdateWorker(inbound, outbound, handler, Schedulers.immediate(), 4);
    }

    @Nested
    class ProcessUpdate {

        @Test
        void callsHandlerAndPublishesResponse() {
            var outbound = new TestChannel<BotResponse>();
            UpdateHandler handler = update -> Flux.just(SAMPLE_RESPONSE);
            var worker = createWorker(new TestChannel<>(), outbound, handler);

            StepVerifier.create(worker.processUpdate(SAMPLE_UPDATE))
                        .expectNext(SAMPLE_RESPONSE)
                        .verifyComplete();

            assertEquals(1, outbound.getPublished().size());
            assertInstanceOf(BotResponse.SendMessage.class, outbound.getPublished().get(0));
        }

        @Test
        void multipleResponses() {
            var response1 = new BotResponse.SendMessage(123L, "first");
            var response2 = new BotResponse.SendMessage(123L, "second");

            var outbound = new TestChannel<BotResponse>();
            UpdateHandler handler = update -> Flux.just(response1, response2);
            var worker = createWorker(new TestChannel<>(), outbound, handler);

            StepVerifier.create(worker.processUpdate(SAMPLE_UPDATE))
                        .expectNext(response1)
                        .expectNext(response2)
                        .verifyComplete();

            assertEquals(2, outbound.getPublished().size());
        }

        @Test
        void handlerReturnsEmpty() {
            var outbound = new TestChannel<BotResponse>();
            UpdateHandler handler = update -> Flux.empty();
            var worker = createWorker(new TestChannel<>(), outbound, handler);

            StepVerifier.create(worker.processUpdate(SAMPLE_UPDATE))
                        .verifyComplete();

            assertTrue(outbound.getPublished().isEmpty());
        }

        @Test
        void handlerError_isSwallowed() {
            var outbound = new TestChannel<BotResponse>();
            UpdateHandler handler = update -> Flux.error(new RuntimeException("handler error"));
            var worker = createWorker(new TestChannel<>(), outbound, handler);

            StepVerifier.create(worker.processUpdate(SAMPLE_UPDATE))
                        .verifyComplete();

            assertTrue(outbound.getPublished().isEmpty());
        }

        @Test
        void publishError_isSwallowed() {
            WritableReactiveChannel<BotResponse> failingOutbound = new WritableReactiveChannel<BotResponse>() {
                @Override
                public Mono<Void> publish(BotResponse m) {
                    return Mono.error(new RuntimeException("queue error"));
                }


                @Override
                public void close() {
                }
            };

            UpdateHandler handler = update -> Flux.just(SAMPLE_RESPONSE);
            var worker = createWorker(new TestChannel<>(), failingOutbound, handler);

            StepVerifier.create(worker.processUpdate(SAMPLE_UPDATE))
                        .verifyComplete();
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void start_andDispose() {
            var inbound = new TestChannel<Update>();
            UpdateHandler handler = update -> Flux.empty();
            var worker = createWorker(inbound, new TestChannel<>(), handler);

            worker.start();
            assertFalse(worker.isDisposed());

            worker.dispose();
            assertTrue(worker.isDisposed());
        }

        @Test
        void start_processesUpdatesFromQueue() throws InterruptedException {
            var inbound = new TestChannel<Update>();
            var outbound = new TestChannel<BotResponse>();
            UpdateHandler handler = update -> Flux.just(SAMPLE_RESPONSE);

            // Use a real scheduler so the subscription runs on another thread
            var worker = new TelegramUpdateWorker(inbound, outbound, handler, Schedulers.single(), 4);
            worker.start();

            // Push an update into the inbound queue
            inbound.publish(SAMPLE_UPDATE).block();

            // Wait for async processing
            Thread.sleep(500);

            assertEquals(1, outbound.getPublished().size());
            assertInstanceOf(BotResponse.SendMessage.class, outbound.getPublished().get(0));

            worker.dispose();
        }
    }
}
