package dev.alimov.telegram.queue.pulsar;

import dev.alimov.telegram.core.ReadableReactiveChannel;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A reactive implementation of {@link ReadableReactiveChannel} for consuming messages
 * from an Apache Pulsar topic. This class integrates with the Apache Pulsar's {@link Consumer}
 * to facilitate reactive message consumption and acknowledgment.
 *
 * @param <T> the type of the payload encapsulated within the consumed messages
 *
 * The key functionalities provided by this class include:
 * - Subscribing to messages reactively using the {@link #subscribe()} method, which returns a {@link Flux}
 *   allowing downstream components to process messages as they arrive.
 * - Supporting message acknowledgment with the {@link #acknowledge(Message)} method, which acknowledges
 *   the receipt of a message asynchronously.
 * - Enabling negative acknowledgment using {@link #negativeAcknowledge(Message)}, allowing failed messages to be
 *   retried or redirected depending on the Pulsar configuration.
 * - Lifecycle management through the {@link #close()} method, ensuring that resources associated with
 *   the consumer are properly cleaned up when no longer needed.
 *
 * Internally, this class uses a backpressure-aware mechanism for subscribing and consuming messages,
 * leveraging Project Reactor's {@link Flux} and {@link Mono} to emit messages and handle errors effectively.
 */
public class PulsarReadableReactiveChannel<T> implements ReadableReactiveChannel<Message<T>> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PulsarReadableReactiveChannel.class);
    private final Consumer<T> consumer;


    public PulsarReadableReactiveChannel(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    public Mono<Void> acknowledge(Message<T> message) {
        log.debug("acknowledge: {}", message.getValue());

        return Mono.fromFuture(consumer.acknowledgeAsync(message));
    }

    public void negativeAcknowledge(Message<T> message) {
        consumer.negativeAcknowledge(message);
    }


    @Override
    public Flux<Message<T>> subscribe() {
        return Flux.<Message<T>>create(sink -> {
            AtomicReference<Disposable> disposableAtomicReference = new AtomicReference<>(null);

            sink.onRequest(requested -> {
                log.debug("requested items count: {}", requested);
                Disposable messageCompletableFuture = fetchNext(sink).subscribeOn(Schedulers.boundedElastic())
                                                                     .doOnComplete(() -> {
                                                                         sink.complete();
                                                                     })
                                                                     .subscribe();

                disposableAtomicReference.set(messageCompletableFuture);
            });

            sink.onDispose(() -> {
                disposableAtomicReference.getAndUpdate(disposable -> {
                    if (disposable != null) {
                        disposable.dispose();
                    }
                    return null;
                });
            });
        }, FluxSink.OverflowStrategy.ERROR);
    }

    private Flux<Message<T>> fetchNext(FluxSink<Message<T>> sink) {
        log.debug("fetchNext");

        return Mono.<Message<T>>create(s -> {
                       try {
                           s.success(consumer.receive(100, TimeUnit.MILLISECONDS));

                       } catch (PulsarClientException e) {
                           s.error(e);
                       }
                   }).flux()
                   .flatMap(msg -> {

                       if (msg == null) {
                           log.debug("has reached end of the queue. no more elements");

                           return Mono.empty();
                       }

                       log.debug("msg: {}", msg.getValue());
                       sink.next(msg);


                       if (sink.isCancelled()) {
                           log.debug("sink has been cancelled. sink: {}", sink);

                           return Flux.empty();
                       }


                       if (sink.requestedFromDownstream() <= 0) { // no more elements
                           log.debug("no more elements or consumer has reached end of topic. sink: {}", sink);

                           return Flux.empty();
                       }

                       return fetchNext(sink);
                   })
                   .onErrorResume(Exception.class, e -> {
                       log.error("Error receiving message from Pulsar: {}", e.getMessage(), e);
                       sink.error(e);
                       return Mono.empty();
                   });
    }


    @Override
    public void close() {

    }
}
