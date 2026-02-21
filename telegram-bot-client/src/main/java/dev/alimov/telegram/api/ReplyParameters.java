package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReplyParameters(
        @JsonProperty("message_id") long messageId,
        @JsonProperty("chat_id") String chatId,
        @JsonProperty("allow_sending_without_reply") Boolean allowSendingWithoutReply,
        @JsonProperty("quote") String quote,
        @JsonProperty("quote_parse_mode") String quoteParseMode,
        @JsonProperty("quote_entities") List<MessageEntity> quoteEntities,
        @JsonProperty("quote_position") Integer quotePosition
) {
    public ReplyParameters(long messageId) {
        this(messageId, null, null, null, null, null, null);
    }
}
