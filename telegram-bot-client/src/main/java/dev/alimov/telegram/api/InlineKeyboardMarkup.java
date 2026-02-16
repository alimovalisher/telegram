package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InlineKeyboardMarkup(
        @JsonProperty("inline_keyboard")
        List<List<InlineKeyboardButton>> inlineKeyboard // Array of button rows, each represented by an Array of InlineKeyboardButton objects
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<List<InlineKeyboardButton>> inlineKeyboard;

        public Builder inlineKeyboard(List<List<InlineKeyboardButton>> inlineKeyboard) {
            this.inlineKeyboard = inlineKeyboard;
            return this;
        }

        public InlineKeyboardMarkup build() {
            return new InlineKeyboardMarkup(inlineKeyboard);
        }
    }
}
