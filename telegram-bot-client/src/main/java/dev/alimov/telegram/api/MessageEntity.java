package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageEntity(
        String type, // mention, hashtag, bot_command, url, bold, italic, code, pre, text_link, text_mention, etc.
        Integer offset,
        Integer length,
        String url,
        User user,
        @JsonProperty("language") String language
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String type;
        private Integer offset;
        private Integer length;
        private String url;
        private User user;
        private String language;

        public Builder type(String type) { this.type = type; return this; }
        public Builder offset(Integer offset) { this.offset = offset; return this; }
        public Builder length(Integer length) { this.length = length; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder language(String language) { this.language = language; return this; }

        public MessageEntity build() {
            return new MessageEntity(type, offset, length, url, user, language);
        }
    }
}
