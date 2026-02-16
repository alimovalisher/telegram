package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReplyKeyboardMarkup(
        List<List<KeyboardButton>> keyboard,
        @JsonProperty("is_persistent") Boolean isPersistent,
        @JsonProperty("resize_keyboard") Boolean resizeKeyboard,
        @JsonProperty("one_time_keyboard") Boolean oneTimeKeyboard,
        Boolean selective,
        @JsonProperty("input_field_placeholder") String inputFieldPlaceholder
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<List<KeyboardButton>> keyboard;
        private Boolean isPersistent = true;
        private Boolean resizeKeyboard = false;
        private Boolean oneTimeKeyboard = false;
        private Boolean selective = false;
        private String inputFieldPlaceholder = "select action of write command";

        public Builder keyboard(List<List<KeyboardButton>> keyboard) {
            this.keyboard = keyboard;
            return this;
        }

        public Builder isPersistent(Boolean isPersistent) {
            this.isPersistent = isPersistent;
            return this;
        }

        public Builder resizeKeyboard(Boolean resizeKeyboard) {
            this.resizeKeyboard = resizeKeyboard;
            return this;
        }

        public Builder oneTimeKeyboard(Boolean oneTimeKeyboard) {
            this.oneTimeKeyboard = oneTimeKeyboard;
            return this;
        }

        public Builder selective(Boolean selective) {
            this.selective = selective;
            return this;
        }

        public Builder inputFieldPlaceholder(String inputFieldPlaceholder) {
            this.inputFieldPlaceholder = inputFieldPlaceholder;
            return this;
        }

        public ReplyKeyboardMarkup build() {
            return new ReplyKeyboardMarkup(keyboard, isPersistent, resizeKeyboard, oneTimeKeyboard, selective, inputFieldPlaceholder);
        }
    }
}
