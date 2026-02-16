package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
        Long id,
        @JsonProperty("is_bot") Boolean isBot,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String username,
        @JsonProperty("language_code") String languageCode
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Boolean isBot;
        private String firstName;
        private String lastName;
        private String username;
        private String languageCode;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder isBot(Boolean isBot) { this.isBot = isBot; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder languageCode(String languageCode) { this.languageCode = languageCode; return this; }

        public User build() { return new User(id, isBot, firstName, lastName, username, languageCode); }
    }
}
