package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CallbackQuery(
        String id,
        User from,
        Message message,
        @JsonProperty("inline_message_id") String inlineMessageId,
        String chatInstance,
        String data,
        @JsonProperty("game_short_name") String gameShortName
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id;
        private User from;
        private Message message;
        private String inlineMessageId;
        private String chatInstance;
        private String data;
        private String gameShortName;

        public Builder id(String id) { this.id = id; return this; }
        public Builder from(User from) { this.from = from; return this; }
        public Builder message(Message message) { this.message = message; return this; }
        public Builder inlineMessageId(String inlineMessageId) { this.inlineMessageId = inlineMessageId; return this; }
        public Builder chatInstance(String chatInstance) { this.chatInstance = chatInstance; return this; }
        public Builder data(String data) { this.data = data; return this; }
        public Builder gameShortName(String gameShortName) { this.gameShortName = gameShortName; return this; }

        public CallbackQuery build() { return new CallbackQuery(id, from, message, inlineMessageId, chatInstance, data, gameShortName); }
    }
}
