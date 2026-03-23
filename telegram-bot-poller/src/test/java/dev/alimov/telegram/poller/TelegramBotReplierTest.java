package dev.alimov.telegram.poller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.alimov.telegram.api.*;
import dev.alimov.telegram.core.BotResponse;
import dev.alimov.telegram.core.ReactiveChannel;
import dev.alimov.telegram.core.ReadableReactiveChannel;
import dev.alimov.telegram.core.WritableReactiveChannel;
import org.junit.jupiter.api.*;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

class TelegramBotReplierTest {

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
        return HttpResponse.response().withStatusCode(200)
                           .withContentType(MediaType.APPLICATION_JSON).withBody(body);
    }

    @BeforeEach
    void setUp() {
        mockServer.reset();
        String mockBaseUrl = "http://localhost:" + mockServer.getPort();
        botClient = new TelegramBotClient(mockBaseUrl, TOKEN, objectMapper, WebClient.builder().build());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private TelegramBotReplier createReplier(ReadableReactiveChannel<BotResponse> outbound) {
        return new TelegramBotReplier(botClient, outbound, Duration.ofMillis(100), Schedulers.immediate());
    }

    /**
     * Simple in-memory queue — no Mockito needed
     */
    static class TestChannel<T> implements ReadableReactiveChannel<T>, WritableReactiveChannel<T> {
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
    class DispatchText {
        @Test
        void sendMessage() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendMessage"))
                      .respond(ok(OK_MSG));

            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendMessage(123L, "reply", ParseMode.HTML)))
                        .expectNextCount(1).verifyComplete();

            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendMessage"));
        }
    }

    @Nested
    class DispatchMedia {
        @Test
        void sendPhoto() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendPhoto"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendPhoto(10L, "photo_id", "cap", ParseMode.HTML)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendPhoto"));
        }

        @Test
        void sendDocument() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendDocument"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendDocument(11L, "doc_id")))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendDocument"));
        }

        @Test
        void sendVideo() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendVideo"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendVideo(12L, "vid_id")))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendVideo"));
        }

        @Test
        void sendAudio() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendAudio"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendAudio(8L, "audio_id")))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendAudio"));
        }

        @Test
        void sendVoice() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendVoice"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendVoice(9L, "voice_id")))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendVoice"));
        }

        @Test
        void sendSticker() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendSticker"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendSticker(13L, "sticker_id")))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendSticker"));
        }

        @Test
        void sendLocation() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendLocation"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendLocation(14L, 51.5, -0.1)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendLocation"));
        }

        @Test
        void sendContact() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendContact"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendContact(15L, "+123", "John")))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendContact"));
        }

        @Test
        void sendMediaGroup() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendMediaGroup"))
                      .respond(ok(OK_MSG_LIST));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            var media = List.<InputMedia>of(new InputMediaPhoto("p1"), new InputMediaPhoto("p2"));
            StepVerifier.create(poller.dispatch(new BotResponse.SendMediaGroup(16L, media)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendMediaGroup"));
        }
    }

    @Nested
    class DispatchEdit {
        @Test
        void editMessageText() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/editMessageText"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.EditMessageText(18L, 50L, "edited")))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/editMessageText"));
        }

        @Test
        void editMessageCaption() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/editMessageCaption"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.EditMessageCaption(19L, 51L, "new cap", ParseMode.HTML)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/editMessageCaption"));
        }

        @Test
        void editMessageReplyMarkup() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/editMessageReplyMarkup"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.EditMessageReplyMarkup(20L, 52L, null)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/editMessageReplyMarkup"));
        }
    }

    @Nested
    class DispatchDeleteAndCallback {
        @Test
        void deleteMessage() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/deleteMessage"))
                      .respond(ok(OK_BOOL));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.DeleteMessage(21L, 53L)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/deleteMessage"));
        }

        @Test
        void answerCallbackQuery() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/answerCallbackQuery"))
                      .respond(ok(OK_BOOL));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.AnswerCallbackQuery("cb-1", "ok", true)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/answerCallbackQuery"));
        }

        @Test
        void sendChatAction() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendChatAction"))
                      .respond(ok(OK_BOOL));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendChatAction(17L, ChatAction.TYPING)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendChatAction"));
        }
    }

    @Nested
    class DispatchForwardAndPayment {
        @Test
        void forwardMessage() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/forwardMessage"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.ForwardMessage(22L, 23L, 99L)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/forwardMessage"));
        }

        @Test
        void copyMessage() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/copyMessage"))
                      .respond(ok(OK_MSG_ID));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.CopyMessage(24L, 25L, 100L)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/copyMessage"));
        }

        @Test
        void sendInvoice() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/sendInvoice"))
                      .respond(ok(OK_MSG));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.SendInvoice(26L, "Title", "Desc", "payload", 1000, "USD")))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/sendInvoice"));
        }

        @Test
        void answerPreCheckoutQuery() {
            mockServer.when(HttpRequest.request().withMethod("POST").withPath("/bot" + TOKEN + "/answerPreCheckoutQuery"))
                      .respond(ok(OK_BOOL));
            TelegramBotReplier poller = createReplier(new TestChannel<>());
            StepVerifier.create(poller.dispatch(new BotResponse.AnswerPreCheckoutQuery("pcq-1", true, null)))
                        .expectNextCount(1).verifyComplete();
            mockServer.verify(HttpRequest.request().withPath("/bot" + TOKEN + "/answerPreCheckoutQuery"));
        }
    }
}
