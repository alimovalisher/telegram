package dev.alimov.telegram.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

class TelegramBotClientTest {

    private static final String TOKEN = "123456:ABC-DEF";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final long CHAT_ID = 123L;
    public static final long ANOTHER_CHAT_ID = 456L;

    private static ClientAndServer mockServer;
    private TelegramBotClient client;

    @BeforeAll
    static void startServer() {
        mockServer = startClientAndServer(0);
    }

    @AfterAll
    static void stopServer() {
        mockServer.stop();
    }

    @BeforeEach
    void setUp() {
        mockServer.reset();

        String mockBaseUrl = "http://localhost:" + mockServer.getPort();

        WebClient webClient = WebClient.builder()
                                       .build();

        client = new TelegramBotClient(mockBaseUrl, TOKEN, objectMapper, webClient);
    }

    // ========== Authentication Tests ==========

    @Test
    void getUpdates_includesTokenInUrl() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("GET")
                           .withPath("/bot" + TOKEN + "/getUpdates")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":true,\"result\":[]}")
        );

        StepVerifier.create(client.getUpdates(0, 10, 0, List.of()))
                    .verifyComplete();

        mockServer.verify(
                HttpRequest.request()
                           .withPath("/bot" + TOKEN + "/getUpdates")
        );
    }

    @Test
    void sendMessage_includesTokenInUrl() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMessage")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":true,\"result\":{\"message_id\":1,\"date\":0,\"chat\":{\"id\":123,\"type\":\"private\"}}}")
        );

        StepVerifier.create(client.sendMessage(123L, "hello", null, null))
                    .assertNext(response -> assertTrue(response.isOk()))
                    .verifyComplete();

        mockServer.verify(
                HttpRequest.request()
                           .withPath("/bot" + TOKEN + "/sendMessage")
        );
    }

    @Test
    void getUpdates_unauthorizedToken_returnsError() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("GET")
                           .withPath("/bot" + TOKEN + "/getUpdates")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":false,\"error_code\":401,\"description\":\"Unauthorized\"}")
        );

        StepVerifier.create(client.getUpdates(0, 10, 0, List.of()))
                    .expectErrorMatches(e -> e instanceof RuntimeException && "Unauthorized".equals(e.getMessage()))
                    .verify();
    }

    // ========== Polling (getUpdates) Tests ==========

    @Test
    void getUpdates_returnsSingleUpdate() {
        String body = """
                {
                  "ok": true,
                  "result": [
                    {
                      "update_id": 100,
                      "message": {
                        "message_id": 1,
                        "date": 1000,
                        "chat": {"id": 123, "type": "private"},
                        "text": "hello"
                      }
                    }
                  ]
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("GET")
                           .withPath("/bot" + TOKEN + "/getUpdates")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(body)
        );

        StepVerifier.create(client.getUpdates(0, 10, 0, List.of()))
                    .assertNext(update -> {
                        assertEquals(100L, update.updateId());
                        assertNotNull(update.message());
                        assertEquals("hello", update.message().text());
                        assertEquals(123L, update.message().chat().id());
                    })
                    .verifyComplete();
    }

    @Test
    void getUpdates_returnsMultipleUpdates() {
        String body = """
                {
                  "ok": true,
                  "result": [
                    {"update_id": 1, "message": {"message_id": 1, "date": 0, "chat": {"id": 10, "type": "private"}, "text": "first"}},
                    {"update_id": 2, "message": {"message_id": 2, "date": 0, "chat": {"id": 10, "type": "private"}, "text": "second"}},
                    {"update_id": 3, "message": {"message_id": 3, "date": 0, "chat": {"id": 10, "type": "private"}, "text": "third"}}
                  ]
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("GET")
                           .withPath("/bot" + TOKEN + "/getUpdates")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(body)
        );

        StepVerifier.create(client.getUpdates(0, 10, 0, List.of()))
                    .assertNext(u -> assertEquals("first", u.message().text()))
                    .assertNext(u -> assertEquals("second", u.message().text()))
                    .assertNext(u -> assertEquals("third", u.message().text()))
                    .verifyComplete();
    }

    @Test
    void getUpdates_emptyResult() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("GET")
                           .withPath("/bot" + TOKEN + "/getUpdates")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":true,\"result\":[]}")
        );

        StepVerifier.create(client.getUpdates(0, 10, 0, List.of()))
                    .verifyComplete();
    }

    @Test
    void getUpdates_passesQueryParameters() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("GET")
                           .withPath("/bot" + TOKEN + "/getUpdates")
                           .withQueryStringParameter("offset", "5")
                           .withQueryStringParameter("limit", "20")
                           .withQueryStringParameter("timeout", "30")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":true,\"result\":[]}")
        );

        StepVerifier.create(client.getUpdates(5, 20, 30, List.of("message")))
                    .verifyComplete();

        mockServer.verify(
                HttpRequest.request()
                           .withPath("/bot" + TOKEN + "/getUpdates")
                           .withQueryStringParameter("offset", "5")
                           .withQueryStringParameter("limit", "20")
                           .withQueryStringParameter("timeout", "30")
        );
    }

    @Test
    void getUpdates_errorResponse_propagatesError() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("GET")
                           .withPath("/bot" + TOKEN + "/getUpdates")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":false,\"error_code\":409,\"description\":\"Conflict: terminated by other getUpdates request\"}")
        );

        StepVerifier.create(client.getUpdates(0, 10, 0, List.of()))
                    .expectErrorMatches(e ->
                                                e instanceof RuntimeException
                                                        && e.getMessage().contains("Conflict"))
                    .verify();
    }

    @Test
    void getUpdates_withCallbackQuery() {
        String body = """
                {
                  "ok": true,
                  "result": [
                    {
                      "update_id": 200,
                      "callback_query": {
                        "id": "cb-1",
                        "from": {"id": 42, "is_bot": false, "first_name": "Ali"},
                        "data": "action_1",
                        "chat_instance": "inst-1"
                      }
                    }
                  ]
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("GET")
                           .withPath("/bot" + TOKEN + "/getUpdates")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(body)
        );

        StepVerifier.create(client.getUpdates(0, 10, 0, List.of()))
                    .assertNext(update -> {
                        assertEquals(200L, update.updateId());
                        assertNull(update.message());
                        assertNotNull(update.callbackQuery());
                        assertEquals("cb-1", update.callbackQuery().id());
                        assertEquals("action_1", update.callbackQuery().data());
                    })
                    .verifyComplete();
    }

    // ========== Send Message Tests ==========

    @Test
    void sendMessage_basicMessage() {
        String responseBody = """
                {
                  "ok": true,
                  "result": {
                    "message_id": 42,
                    "date": 1700000000,
                    "chat": {"id": 123, "type": "private"},
                    "text": "Hello World"
                  }
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMessage")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        StepVerifier.create(client.sendMessage(123L, "Hello World", null, null))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertNotNull(response.getResult());
                        assertEquals(42L, response.getResult().messageId());
                        assertEquals("Hello World", response.getResult().text());
                        assertEquals(123L, response.getResult().chat().id());
                    })
                    .verifyComplete();
    }

    @Test
    void sendMessage_withParseMode() {
        String responseBody = """
                {
                  "ok": true,
                  "result": {
                    "message_id": 43,
                    "date": 1700000000,
                    "chat": {"id": 123, "type": "private"},
                    "text": "<b>bold</b>"
                  }
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMessage")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        StepVerifier.create(client.sendMessage(123L, "<b>bold</b>", ParseMode.HTML, null))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertEquals(43L, response.getResult().messageId());
                    })
                    .verifyComplete();
    }

    @Test
    void sendMessage_withReplyKeyboard() {
        String responseBody = """
                {
                  "ok": true,
                  "result": {
                    "message_id": 44,
                    "date": 1700000000,
                    "chat": {"id": 123, "type": "private"},
                    "text": "Choose:"
                  }
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMessage")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
                                                          .keyboard(List.of(
                                                                  List.of(new KeyboardButton("Option A", null, null),
                                                                          new KeyboardButton("Option B", null, null))
                                                          ))
                                                          .build();

        StepVerifier.create(client.sendMessage(123L, "Choose:", null, keyboard))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertEquals(44L, response.getResult().messageId());
                    })
                    .verifyComplete();
    }

    @Test
    void sendMessage_httpErrorResponse() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMessage")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(400)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}")
        );

        StepVerifier.create(client.sendMessage(999L, "test", null, null))
                    .expectError()
                    .verify();
    }

    // ========== Send Photo Tests ==========

    @Test
    void sendPhoto_basicPhoto() {
        String responseBody = """
                {
                  "ok": true,
                  "result": {
                    "message_id": 50,
                    "date": 1700000000,
                    "chat": {"id": 123, "type": "private"},
                    "photo": [
                      {"file_id": "photo_small", "file_unique_id": "unique1", "width": 90, "height": 90, "file_size": 1000},
                      {"file_id": "photo_large", "file_unique_id": "unique2", "width": 800, "height": 600, "file_size": 50000}
                    ]
                  }
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendPhoto")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        StepVerifier.create(client.sendPhoto(CHAT_ID, "https://live.staticflickr.com/3465/3717404335_a5472e3368_h.jpg",
                                             null, null, null, null, null, null, null, null, null, null, null, null, null))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertNotNull(response.getResult());
                        assertEquals(50L, response.getResult().messageId());
                        assertNotNull(response.getResult().photo());
                        assertEquals(2, response.getResult().photo().length);
                    })
                    .verifyComplete();

        mockServer.verify(
                HttpRequest.request()
                           .withPath("/bot" + TOKEN + "/sendPhoto")
        );
    }

    @Test
    void sendPhoto_withCaptionAndParseMode() {
        String responseBody = """
                {
                  "ok": true,
                  "result": {
                    "message_id": 51,
                    "date": 1700000000,
                    "chat": {"id": 123, "type": "private"},
                    "photo": [
                      {"file_id": "photo1", "file_unique_id": "u1", "width": 320, "height": 240}
                    ],
                    "caption": "<b>Nice photo</b>"
                  }
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendPhoto")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        StepVerifier.create(client.sendPhoto(CHAT_ID, "AgACAgIAAxkBAAI", null, null,
                                             "<b>Nice photo</b>", ParseMode.HTML, null, null, null, null, null, null, null, null, null))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertEquals(51L, response.getResult().messageId());
                        assertEquals("<b>Nice photo</b>", response.getResult().caption());
                    })
                    .verifyComplete();
    }

    @Test
    void sendPhoto_withReplyMarkup() {
        String responseBody = """
                {
                  "ok": true,
                  "result": {
                    "message_id": 52,
                    "date": 1700000000,
                    "chat": {"id": 456, "type": "group", "title": "Test Group"},
                    "photo": [
                      {"file_id": "p1", "file_unique_id": "u1", "width": 100, "height": 100}
                    ]
                  }
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendPhoto")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                                                          .inlineKeyboard(List.of(
                                                                  List.of(InlineKeyboardButton.builder().text("Like").callbackData("like").build())
                                                          ))
                                                          .build();

        StepVerifier.create(client.sendPhoto(ANOTHER_CHAT_ID, "https://example.com/img.png",
                                             null, null, null, null, null, null, null, null, null, null, null, null, markup))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertEquals(52L, response.getResult().messageId());
                    })
                    .verifyComplete();
    }

    @Test
    void sendPhoto_httpError() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendPhoto")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(400)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: wrong file identifier/HTTP URL specified\"}")
        );

        StepVerifier.create(client.sendPhoto(CHAT_ID, "invalid_photo",
                                             null, null, null, null, null, null, null, null, null, null, null, null, null))
                    .expectError()
                    .verify();
    }

    // ========== Send Media Group Tests ==========

    @Test
    void sendMediaGroup_withPhotos() {
        String responseBody = """
                {
                  "ok": true,
                  "result": [
                    {
                      "message_id": 60,
                      "date": 1700000000,
                      "chat": {"id": 123, "type": "private"},
                      "photo": [{"file_id": "p1", "file_unique_id": "u1", "width": 100, "height": 100}]
                    },
                    {
                      "message_id": 61,
                      "date": 1700000000,
                      "chat": {"id": 123, "type": "private"},
                      "photo": [{"file_id": "p2", "file_unique_id": "u2", "width": 200, "height": 200}]
                    }
                  ]
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMediaGroup")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        List<InputMedia> media = List.of(
                new InputMediaPhoto("https://example.com/photo1.jpg"),
                new InputMediaPhoto("https://example.com/photo2.jpg")
        );

        StepVerifier.create(client.sendMediaGroup(CHAT_ID, media, null, null, null, null, null, null, null))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertNotNull(response.getResult());
                        assertEquals(2, response.getResult().size());
                        assertEquals(60L, response.getResult().get(0).messageId());
                        assertEquals(61L, response.getResult().get(1).messageId());
                    })
                    .verifyComplete();

        mockServer.verify(
                HttpRequest.request()
                           .withPath("/bot" + TOKEN + "/sendMediaGroup")
        );
    }

    @Test
    void sendMediaGroup_withMixedMedia() {
        String responseBody = """
                {
                  "ok": true,
                  "result": [
                    {
                      "message_id": 70,
                      "date": 1700000000,
                      "chat": {"id": 123, "type": "private"},
                      "photo": [{"file_id": "p1", "file_unique_id": "u1", "width": 100, "height": 100}]
                    },
                    {
                      "message_id": 71,
                      "date": 1700000000,
                      "chat": {"id": 123, "type": "private"},
                      "video": {"file_id": "v1", "file_unique_id": "vu1", "width": 1920, "height": 1080, "duration": 30}
                    }
                  ]
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMediaGroup")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        List<InputMedia> media = List.of(
                new InputMediaPhoto("https://example.com/photo.jpg", "Photo caption"),
                new InputMediaVideo("https://example.com/video.mp4", "Video caption")
        );

        StepVerifier.create(client.sendMediaGroup(CHAT_ID, media, null, null, null, null, null, null, null))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertEquals(2, response.getResult().size());
                        assertEquals(70L, response.getResult().get(0).messageId());
                        assertEquals(71L, response.getResult().get(1).messageId());
                    })
                    .verifyComplete();
    }

    @Test
    void sendMediaGroup_httpError() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMediaGroup")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(400)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: invalid media\"}")
        );

        List<InputMedia> media = List.of(new InputMediaPhoto("invalid"));

        StepVerifier.create(client.sendMediaGroup(CHAT_ID, media, null, null, null, null, null, null, null))
                    .expectError()
                    .verify();
    }

    // ========== Send Chat Action Tests ==========

    @Test
    void sendChatAction_typing() {
        String responseBody = """
                {
                  "ok": true,
                  "result": true
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendChatAction")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        StepVerifier.create(client.sendChatAction(CHAT_ID, ChatAction.TYPING, null, null))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertTrue(response.getResult());
                    })
                    .verifyComplete();

        mockServer.verify(
                HttpRequest.request()
                           .withPath("/bot" + TOKEN + "/sendChatAction")
        );
    }

    @Test
    void sendChatAction_uploadPhoto() {
        String responseBody = """
                {
                  "ok": true,
                  "result": true
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendChatAction")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        StepVerifier.create(client.sendChatAction(CHAT_ID, ChatAction.UPLOAD_PHOTO, null, null))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertTrue(response.getResult());
                    })
                    .verifyComplete();
    }

    @Test
    void sendChatAction_withMessageThreadId() {
        String responseBody = """
                {
                  "ok": true,
                  "result": true
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendChatAction")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        StepVerifier.create(client.sendChatAction(CHAT_ID, ChatAction.RECORD_VIDEO, null, 42))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertTrue(response.getResult());
                    })
                    .verifyComplete();
    }

    @Test
    void sendChatAction_httpError() {
        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendChatAction")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(400)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}")
        );

        StepVerifier.create(client.sendChatAction(CHAT_ID, ChatAction.TYPING, null, null))
                    .expectError()
                    .verify();
    }

    @Test
    void sendMessage_withParseModeAndKeyboard() {
        String responseBody = """
                {
                  "ok": true,
                  "result": {
                    "message_id": 45,
                    "date": 1700000000,
                    "chat": {"id": 456, "type": "group", "title": "Test Group"},
                    "text": "**bold text**"
                  }
                }
                """;

        mockServer.when(
                HttpRequest.request()
                           .withMethod("POST")
                           .withPath("/bot" + TOKEN + "/sendMessage")
        ).respond(
                HttpResponse.response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseBody)
        );

        ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
                                                          .keyboard(List.of(
                                                                  List.of(new KeyboardButton("Yes", null, null),
                                                                          new KeyboardButton("No", null, null))
                                                          ))
                                                          .build();

        StepVerifier.create(client.sendMessage(456L, "**bold text**", ParseMode.MarkdownV2, keyboard))
                    .assertNext(response -> {
                        assertTrue(response.isOk());
                        assertEquals(45L, response.getResult().messageId());
                        assertEquals("group", response.getResult().chat().type());
                    })
                    .verifyComplete();
    }
}
