package dev.alimov.telegram.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import dev.alimov.telegram.api.ChatAction;
import dev.alimov.telegram.api.InputMedia;
import dev.alimov.telegram.api.MessageEntity;
import dev.alimov.telegram.api.ParseMode;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sealed interface representing all possible outbound responses a bot can send via the Telegram Bot API.
 * Each variant maps to a specific Telegram API method.
 * <p>
 * Uses Jackson polymorphic serialization so responses can be transported through a {@link ReactiveChannel}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BotResponse.SendMessage.class, name = "send_message"),
        @JsonSubTypes.Type(value = BotResponse.SendPhoto.class, name = "send_photo"),
        @JsonSubTypes.Type(value = BotResponse.SendDocument.class, name = "send_document"),
        @JsonSubTypes.Type(value = BotResponse.SendVideo.class, name = "send_video"),
        @JsonSubTypes.Type(value = BotResponse.SendAudio.class, name = "send_audio"),
        @JsonSubTypes.Type(value = BotResponse.SendVoice.class, name = "send_voice"),
        @JsonSubTypes.Type(value = BotResponse.SendSticker.class, name = "send_sticker"),
        @JsonSubTypes.Type(value = BotResponse.SendLocation.class, name = "send_location"),
        @JsonSubTypes.Type(value = BotResponse.SendContact.class, name = "send_contact"),
        @JsonSubTypes.Type(value = BotResponse.SendMediaGroup.class, name = "send_media_group"),
        @JsonSubTypes.Type(value = BotResponse.SendChatAction.class, name = "send_chat_action"),
        @JsonSubTypes.Type(value = BotResponse.SendInvoice.class, name = "send_invoice"),
        @JsonSubTypes.Type(value = BotResponse.ForwardMessage.class, name = "forward_message"),
        @JsonSubTypes.Type(value = BotResponse.CopyMessage.class, name = "copy_message"),
        @JsonSubTypes.Type(value = BotResponse.EditMessageText.class, name = "edit_message_text"),
        @JsonSubTypes.Type(value = BotResponse.EditMessageCaption.class, name = "edit_message_caption"),
        @JsonSubTypes.Type(value = BotResponse.EditMessageReplyMarkup.class, name = "edit_message_reply_markup"),
        @JsonSubTypes.Type(value = BotResponse.DeleteMessage.class, name = "delete_message"),
        @JsonSubTypes.Type(value = BotResponse.AnswerCallbackQuery.class, name = "answer_callback_query"),
        @JsonSubTypes.Type(value = BotResponse.AnswerPreCheckoutQuery.class, name = "answer_pre_checkout_query"),
})
public sealed interface BotResponse {

    // ── Text ─────────────────────────────────────────────────────────────

    @JsonTypeName("send_message")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendMessage(
            long chatId,
            String text,
            @Nullable ParseMode parseMode,
            @Nullable Object replyMarkup,
            @Nullable List<MessageEntity> entities,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendMessage(long chatId, String text) {
            this(chatId, text, null, null, null, null, null);
        }

        public SendMessage(long chatId, String text, @Nullable ParseMode parseMode) {
            this(chatId, text, parseMode, null, null, null, null);
        }

        public SendMessage(long chatId, String text, @Nullable ParseMode parseMode, @Nullable Object replyMarkup) {
            this(chatId, text, parseMode, replyMarkup, null, null, null);
        }
    }

    // ── Media ────────────────────────────────────────────────────────────

    @JsonTypeName("send_photo")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendPhoto(
            long chatId,
            String photo,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable List<MessageEntity> captionEntities,
            @Nullable Boolean showCaptionAboveMedia,
            @Nullable Boolean hasSpoiler,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendPhoto(long chatId, String photo) {
            this(chatId, photo, null, null, null, null, null, null, null, null);
        }

        public SendPhoto(long chatId, String photo, @Nullable String caption, @Nullable ParseMode parseMode) {
            this(chatId, photo, caption, parseMode, null, null, null, null, null, null);
        }
    }

    @JsonTypeName("send_document")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendDocument(
            long chatId,
            String document,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendDocument(long chatId, String document) {
            this(chatId, document, null, null, null, null, null);
        }

        public SendDocument(long chatId, String document, @Nullable String caption, @Nullable ParseMode parseMode) {
            this(chatId, document, caption, parseMode, null, null, null);
        }
    }

    @JsonTypeName("send_video")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendVideo(
            long chatId,
            String video,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Integer duration,
            @Nullable Integer width,
            @Nullable Integer height,
            @Nullable Boolean supportsStreaming,
            @Nullable Boolean hasSpoiler,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendVideo(long chatId, String video) {
            this(chatId, video, null, null, null, null, null, null, null, null, null, null);
        }

        public SendVideo(long chatId, String video, @Nullable String caption, @Nullable ParseMode parseMode) {
            this(chatId, video, caption, parseMode, null, null, null, null, null, null, null, null);
        }
    }

    @JsonTypeName("send_audio")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendAudio(
            long chatId,
            String audio,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Integer duration,
            @Nullable String performer,
            @Nullable String title,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendAudio(long chatId, String audio) {
            this(chatId, audio, null, null, null, null, null, null, null, null);
        }

        public SendAudio(long chatId, String audio, @Nullable String caption, @Nullable ParseMode parseMode) {
            this(chatId, audio, caption, parseMode, null, null, null, null, null, null);
        }
    }

    @JsonTypeName("send_voice")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendVoice(
            long chatId,
            String voice,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Integer duration,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendVoice(long chatId, String voice) {
            this(chatId, voice, null, null, null, null, null, null);
        }

        public SendVoice(long chatId, String voice, @Nullable String caption, @Nullable ParseMode parseMode) {
            this(chatId, voice, caption, parseMode, null, null, null, null);
        }
    }

    @JsonTypeName("send_sticker")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendSticker(
            long chatId,
            String sticker,
            @Nullable String emoji,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendSticker(long chatId, String sticker) {
            this(chatId, sticker, null, null, null, null);
        }
    }

    @JsonTypeName("send_location")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendLocation(
            long chatId,
            double latitude,
            double longitude,
            @Nullable Integer livePeriod,
            @Nullable Integer heading,
            @Nullable Integer proximityAlertRadius,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendLocation(long chatId, double latitude, double longitude) {
            this(chatId, latitude, longitude, null, null, null, null, null, null);
        }
    }

    @JsonTypeName("send_contact")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendContact(
            long chatId,
            String phoneNumber,
            String firstName,
            @Nullable String lastName,
            @Nullable String vcard,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendContact(long chatId, String phoneNumber, String firstName) {
            this(chatId, phoneNumber, firstName, null, null, null, null, null);
        }
    }

    @JsonTypeName("send_media_group")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendMediaGroup(
            long chatId,
            List<InputMedia> media,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public SendMediaGroup(long chatId, List<InputMedia> media) {
            this(chatId, media, null, null);
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────

    @JsonTypeName("send_chat_action")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendChatAction(
            long chatId,
            ChatAction action
    ) implements BotResponse {
    }

    // ── Payments ─────────────────────────────────────────────────────────

    @JsonTypeName("send_invoice")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendInvoice(
            long chatId,
            String title,
            String description,
            String payload,
            int price,
            String currency
    ) implements BotResponse {
    }

    @JsonTypeName("answer_pre_checkout_query")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnswerPreCheckoutQuery(
            String preCheckoutQueryId,
            boolean ok,
            @Nullable String errorMessage
    ) implements BotResponse {
    }

    // ── Forward / Copy ───────────────────────────────────────────────────

    @JsonTypeName("forward_message")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForwardMessage(
            long chatId,
            long fromChatId,
            long messageId,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public ForwardMessage(long chatId, long fromChatId, long messageId) {
            this(chatId, fromChatId, messageId, null, null);
        }
    }

    @JsonTypeName("copy_message")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CopyMessage(
            long chatId,
            long fromChatId,
            long messageId,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) implements BotResponse {
        public CopyMessage(long chatId, long fromChatId, long messageId) {
            this(chatId, fromChatId, messageId, null, null, null, null, null);
        }
    }

    // ── Edit ─────────────────────────────────────────────────────────────

    @JsonTypeName("edit_message_text")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record EditMessageText(
            long chatId,
            long messageId,
            String text,
            @Nullable ParseMode parseMode,
            @Nullable List<MessageEntity> entities,
            @Nullable Object replyMarkup
    ) implements BotResponse {
        public EditMessageText(long chatId, long messageId, String text) {
            this(chatId, messageId, text, null, null, null);
        }

        public EditMessageText(long chatId, long messageId, String text, @Nullable ParseMode parseMode) {
            this(chatId, messageId, text, parseMode, null, null);
        }
    }

    @JsonTypeName("edit_message_caption")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record EditMessageCaption(
            long chatId,
            long messageId,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable List<MessageEntity> captionEntities,
            @Nullable Object replyMarkup
    ) implements BotResponse {
        public EditMessageCaption(long chatId, long messageId) {
            this(chatId, messageId, null, null, null, null);
        }

        public EditMessageCaption(long chatId, long messageId, @Nullable String caption, @Nullable ParseMode parseMode) {
            this(chatId, messageId, caption, parseMode, null, null);
        }
    }

    @JsonTypeName("edit_message_reply_markup")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record EditMessageReplyMarkup(
            long chatId,
            long messageId,
            @Nullable Object replyMarkup
    ) implements BotResponse {
    }

    // ── Delete ───────────────────────────────────────────────────────────

    @JsonTypeName("delete_message")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DeleteMessage(
            long chatId,
            long messageId
    ) implements BotResponse {
    }

    // ── Callback ─────────────────────────────────────────────────────────

    @JsonTypeName("answer_callback_query")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnswerCallbackQuery(
            String callbackQueryId,
            @Nullable String text,
            @Nullable Boolean showAlert,
            @Nullable String url,
            @Nullable Integer cacheTime
    ) implements BotResponse {
        public AnswerCallbackQuery(String callbackQueryId) {
            this(callbackQueryId, null, null, null, null);
        }

        public AnswerCallbackQuery(String callbackQueryId, @Nullable String text, @Nullable Boolean showAlert) {
            this(callbackQueryId, text, showAlert, null, null);
        }
    }
}
