package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Update(
        @JsonProperty("update_id") Long updateId,
        Message message,
        @JsonProperty("edited_message") Message editedMessage,
        @JsonProperty("channel_post") Message channelPost,
        @JsonProperty("edited_channel_post") Message editedChannelPost,
        @JsonProperty("callback_query") CallbackQuery callbackQuery,
        @JsonProperty("pre_checkout_query") PreCheckoutQuery preCheckoutQuery
) {
    
}
