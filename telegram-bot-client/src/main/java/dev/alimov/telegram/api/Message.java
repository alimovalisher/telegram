package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Message(
        @JsonProperty("message_id") Long messageId,
        User from,
        @JsonProperty("sender_chat") Chat senderChat,
        Chat chat,
        Integer date, // Unix time
        String text,
        List<MessageEntity> entities,
        PhotoSize[] photo,
        Document document,
        Animation animation,
        Audio audio,
        Video video,
        Voice voice,
        Contact contact,
        Location location,
        Dice dice,
        Sticker sticker,
        String caption,
        @JsonProperty("caption_entities") List<MessageEntity> captionEntities,
        @JsonProperty("reply_to_message") Message replyToMessage,
        @JsonProperty("successful_payment" ) SuccessfulPayment successfulPayment
) {
    
}
