package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Chat(
        Long id,
        String type, // “private”, “group”, “supergroup” or “channel”
        @JsonProperty("title") String title,
        @JsonProperty("username") String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String type;
        private String title;
        private String username;
        private String firstName;
        private String lastName;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }

        public Chat build() { return new Chat(id, type, title, username, firstName, lastName); }
    }
}
