package dev.alimov.telegram.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.alimov.telegram.api.ChatAction;
import dev.alimov.telegram.api.InputMediaPhoto;
import dev.alimov.telegram.api.ParseMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotResponseTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    private <T extends BotResponse> T roundTrip(BotResponse response, Class<T> expectedType) throws Exception {
        String json = mapper.writeValueAsString(response);
        BotResponse deserialized = mapper.readValue(json, BotResponse.class);
        assertInstanceOf(expectedType, deserialized);
        return expectedType.cast(deserialized);
    }

    @Nested
    class SendMessageTests {
        @Test
        void fullRoundTrip() throws Exception {
            var original = new BotResponse.SendMessage(123L, "hello", ParseMode.HTML, null, null, true, false);
            var result = roundTrip(original, BotResponse.SendMessage.class);
            assertEquals(123L, result.chatId());
            assertEquals("hello", result.text());
            assertEquals(ParseMode.HTML, result.parseMode());
            assertTrue(result.disableNotification());
            assertFalse(result.protectContent());
        }

        @Test
        void convenienceConstructor() throws Exception {
            var original = new BotResponse.SendMessage(42L, "hi");
            var result = roundTrip(original, BotResponse.SendMessage.class);
            assertEquals(42L, result.chatId());
            assertEquals("hi", result.text());
            assertNull(result.parseMode());
            assertNull(result.replyMarkup());
        }

        @Test
        void jsonContainsTypeDiscriminator() throws Exception {
            String json = mapper.writeValueAsString(new BotResponse.SendMessage(1L, "x"));
            assertTrue(json.contains("\"@type\":\"send_message\""));
        }
    }

    @Nested
    class MediaTests {
        @Test
        void sendPhoto() throws Exception {
            var original = new BotResponse.SendPhoto(10L, "photo_id", "caption", ParseMode.MarkdownV2,
                    null, true, false, null, null, null);
            var result = roundTrip(original, BotResponse.SendPhoto.class);
            assertEquals("photo_id", result.photo());
            assertEquals("caption", result.caption());
            assertTrue(result.showCaptionAboveMedia());
        }

        @Test
        void sendDocument() throws Exception {
            var original = new BotResponse.SendDocument(5L, "doc_id", "readme.txt", ParseMode.HTML);
            var result = roundTrip(original, BotResponse.SendDocument.class);
            assertEquals("doc_id", result.document());
            assertEquals("readme.txt", result.caption());
        }

        @Test
        void sendVideo() throws Exception {
            var original = new BotResponse.SendVideo(7L, "vid_id", "clip", ParseMode.HTML,
                    120, 1920, 1080, true, false, null, null, null);
            var result = roundTrip(original, BotResponse.SendVideo.class);
            assertEquals("vid_id", result.video());
            assertEquals(120, result.duration());
            assertEquals(1920, result.width());
            assertTrue(result.supportsStreaming());
        }

        @Test
        void sendAudio() throws Exception {
            var original = new BotResponse.SendAudio(8L, "audio_id", "song", null,
                    240, "Artist", "Title", null, null, null);
            var result = roundTrip(original, BotResponse.SendAudio.class);
            assertEquals("Artist", result.performer());
            assertEquals("Title", result.title());
        }

        @Test
        void sendVoice() throws Exception {
            var original = new BotResponse.SendVoice(9L, "voice_id", "note", ParseMode.HTML, 30, null, null, null);
            var result = roundTrip(original, BotResponse.SendVoice.class);
            assertEquals("voice_id", result.voice());
            assertEquals(30, result.duration());
        }

        @Test
        void sendSticker() throws Exception {
            var result = roundTrip(new BotResponse.SendSticker(11L, "sticker_id"), BotResponse.SendSticker.class);
            assertEquals("sticker_id", result.sticker());
        }

        @Test
        void sendMediaGroup_serialization() throws Exception {
            var original = new BotResponse.SendMediaGroup(14L, List.of(
                    new InputMediaPhoto("photo1"), new InputMediaPhoto("photo2")));
            String json = mapper.writeValueAsString(original);
            assertTrue(json.contains("\"@type\":\"send_media_group\""));
            assertTrue(json.contains("photo1"));
            assertTrue(json.contains("photo2"));
        }
    }

    @Nested
    class LocationAndContactTests {
        @Test
        void sendLocation() throws Exception {
            var result = roundTrip(new BotResponse.SendLocation(12L, 51.5074, -0.1278), BotResponse.SendLocation.class);
            assertEquals(51.5074, result.latitude(), 0.0001);
            assertEquals(-0.1278, result.longitude(), 0.0001);
        }

        @Test
        void sendContact() throws Exception {
            var original = new BotResponse.SendContact(13L, "+1234567890", "John", "Doe",
                    null, null, null, null);
            var result = roundTrip(original, BotResponse.SendContact.class);
            assertEquals("+1234567890", result.phoneNumber());
            assertEquals("Doe", result.lastName());
        }
    }

    @Nested
    class ActionAndPaymentTests {
        @Test
        void sendChatAction() throws Exception {
            var result = roundTrip(new BotResponse.SendChatAction(15L, ChatAction.TYPING), BotResponse.SendChatAction.class);
            assertEquals(ChatAction.TYPING, result.action());
        }

        @Test
        void sendInvoice() throws Exception {
            var result = roundTrip(new BotResponse.SendInvoice(16L, "Product", "Desc", "payload", 1000, "USD"),
                    BotResponse.SendInvoice.class);
            assertEquals("Product", result.title());
            assertEquals(1000, result.price());
        }

        @Test
        void answerPreCheckoutQuery() throws Exception {
            var result = roundTrip(new BotResponse.AnswerPreCheckoutQuery("pq-1", true, null),
                    BotResponse.AnswerPreCheckoutQuery.class);
            assertTrue(result.ok());
        }
    }

    @Nested
    class ForwardAndCopyTests {
        @Test
        void forwardMessage() throws Exception {
            var result = roundTrip(new BotResponse.ForwardMessage(17L, 18L, 99L), BotResponse.ForwardMessage.class);
            assertEquals(17L, result.chatId());
            assertEquals(18L, result.fromChatId());
            assertEquals(99L, result.messageId());
        }

        @Test
        void copyMessage() throws Exception {
            var original = new BotResponse.CopyMessage(19L, 20L, 100L, "new caption", ParseMode.HTML,
                    null, null, null);
            var result = roundTrip(original, BotResponse.CopyMessage.class);
            assertEquals("new caption", result.caption());
        }
    }

    @Nested
    class EditTests {
        @Test
        void editMessageText() throws Exception {
            var result = roundTrip(new BotResponse.EditMessageText(21L, 50L, "edited", ParseMode.HTML),
                    BotResponse.EditMessageText.class);
            assertEquals(50L, result.messageId());
            assertEquals("edited", result.text());
        }

        @Test
        void editMessageCaption() throws Exception {
            var result = roundTrip(new BotResponse.EditMessageCaption(22L, 51L, "new cap", ParseMode.MarkdownV2),
                    BotResponse.EditMessageCaption.class);
            assertEquals("new cap", result.caption());
        }

        @Test
        void editMessageReplyMarkup() throws Exception {
            var result = roundTrip(new BotResponse.EditMessageReplyMarkup(23L, 52L, null),
                    BotResponse.EditMessageReplyMarkup.class);
            assertEquals(52L, result.messageId());
        }
    }

    @Nested
    class DeleteAndCallbackTests {
        @Test
        void deleteMessage() throws Exception {
            var result = roundTrip(new BotResponse.DeleteMessage(24L, 53L), BotResponse.DeleteMessage.class);
            assertEquals(24L, result.chatId());
            assertEquals(53L, result.messageId());
        }

        @Test
        void answerCallbackQuery() throws Exception {
            var result = roundTrip(new BotResponse.AnswerCallbackQuery("cb-1", "Done!", true, null, 30),
                    BotResponse.AnswerCallbackQuery.class);
            assertEquals("cb-1", result.callbackQueryId());
            assertTrue(result.showAlert());
            assertEquals(30, result.cacheTime());
        }

        @Test
        void answerCallbackQuery_convenienceConstructor() throws Exception {
            var result = roundTrip(new BotResponse.AnswerCallbackQuery("cb-2"),
                    BotResponse.AnswerCallbackQuery.class);
            assertNull(result.text());
        }
    }
}
