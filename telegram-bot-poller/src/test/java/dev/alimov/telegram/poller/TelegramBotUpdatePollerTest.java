package dev.alimov.telegram.poller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.alimov.telegram.api.*;
import dev.alimov.telegram.core.ReactiveChannel;
import dev.alimov.telegram.core.ReadableReactiveChannel;
import dev.alimov.telegram.core.WritableReactiveChannel;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

class TelegramBotUpdatePollerTest {

    private static final String TOKEN = "123456:ABC-DEF";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OK_MSG = """
            {"ok":true,"result":{"message_id":1,"date":0,"chat":{"id":123,"type":"private"}}}""";
    private static final String OK_BOOL = """
            {"ok":true,"result":true}""";
    private static final String OK_MSG_ID = """
            {"ok":true,"result":{"message_id":1}}""";
    private static final String OK_MSG_LIST = """
            {"ok":true,"result":[]}""";

    private static ClientAndServer mockServer;
    private TelegramBotClient botClient;

    @BeforeAll
    static void startServer() {
        mockServer = startClientAndServer(0);
    }

    @AfterAll
    static void stopServer() {
        mockServer.stop();
    }

    private static HttpResponse ok(String body) {
        return httpResponse(HttpStatus.OK, body);
    }

    private static HttpResponse httpResponse(HttpStatus httpStatus, String body) {
        return HttpResponse.response()
                           .withStatusCode(httpStatus.value())
                           .withContentType(MediaType.APPLICATION_JSON)
                           .withBody(body);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        mockServer.reset();
        String mockBaseUrl = "http://localhost:" + mockServer.getPort();
        botClient = new TelegramBotClient(mockBaseUrl, TOKEN, objectMapper, WebClient.builder().build());
    }

    private TelegramBotUpdatePoller createPoller(WritableReactiveChannel<Update> inbound) {
        return new TelegramBotUpdatePoller(botClient, inbound, Duration.ofMillis(100), 100, 0, List.of(), Schedulers.immediate());
    }

    /**
     * Simple in-memory queue — no Mockito needed
     */
    static class TestChannel<T> implements WritableReactiveChannel<T>, ReadableReactiveChannel<T> {
        private final List<T> published = new CopyOnWriteArrayList<>();
        private final reactor.core.publisher.Sinks.Many<T> sink =
                reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer();

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

    // ── tests ────────────────────────────────────────────────────────────

    @Nested
    class PollUpdates {

        @Test
        void publishesUpdatesToInboundQueue() {
            mockServer.when(
                              HttpRequest.request().withMethod("GET").withPath("/bot" + TOKEN + "/getUpdates")
                      )
                      .respond(ok("""
                                          {"ok":true,"result":[{"update_id":100,"message":{"message_id":1,"date":1000,"chat":{"id":123,"type":"private"},"text":"hello"}}]}
                                          """));

            TestChannel<Update> inbound = new TestChannel<>();
            TelegramBotUpdatePoller poller = createPoller(inbound);

            StepVerifier.create(poller.pollUpdates(0))
                        .assertNext(u -> assertEquals(100L, u.updateId()))
                        .verifyComplete();

            assertEquals(1, inbound.getPublished().size());
            assertEquals("hello", inbound.getPublished().get(0).message().text());
        }

        @Test
        void emptyResult_publishesNothing() {
            mockServer.when(
                              HttpRequest.request().withMethod("GET").withPath("/bot" + TOKEN + "/getUpdates")
                      )
                      .respond(ok("{\"ok\":true,\"result\":[]}"));

            TestChannel<Update> inbound = new TestChannel<>();
            TelegramBotUpdatePoller poller = createPoller(inbound);

            StepVerifier.create(poller.pollUpdates(0)).verifyComplete();
            assertTrue(inbound.getPublished().isEmpty());
        }

        @Test
        void errorResponse_recoversGracefully() {
            mockServer.when(
                              HttpRequest.request().withMethod("GET").withPath("/bot" + TOKEN + "/getUpdates")
                      )
                      .respond(TelegramBotUpdatePollerTest.httpResponse(HttpStatus.CONFLICT, "{\"ok\":false,\"error_code\":409,\"description\":\"Conflict\"}"));

            TestChannel<Update> inbound = new TestChannel<>();
            TelegramBotUpdatePoller poller = createPoller(inbound);

            StepVerifier.create(poller.pollUpdates(0))
                        .verifyErrorMatches(e -> {
                            if (e instanceof TelegramApiException telegramApiException) {
                                return StringUtils.equals("Conflict", telegramApiException.getMessage());
                            }

                            return false;
                        });

            assertTrue(inbound.getPublished().isEmpty());
        }
    }


}
