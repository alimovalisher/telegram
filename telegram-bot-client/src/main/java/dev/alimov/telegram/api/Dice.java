package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Dice(
        String emoji, // 🎲, 🎯, 🏀, ⚽, 🎳, 🎰
        Integer value
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String emoji;
        private Integer value;

        public Builder emoji(String emoji) { this.emoji = emoji; return this; }
        public Builder value(Integer value) { this.value = value; return this; }

        public Dice build() { return new Dice(emoji, value); }
    }
}
