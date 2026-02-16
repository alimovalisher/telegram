package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginUrl(
        String url,
        @JsonProperty("forward_text") String forwardText,
        @JsonProperty("bot_username") String botUsername,
        @JsonProperty("request_write_access") Boolean requestWriteAccess
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String url;
        private String forwardText;
        private String botUsername;
        private Boolean requestWriteAccess;

        public Builder url(String url) { this.url = url; return this; }
        public Builder forwardText(String forwardText) { this.forwardText = forwardText; return this; }
        public Builder botUsername(String botUsername) { this.botUsername = botUsername; return this; }
        public Builder requestWriteAccess(Boolean requestWriteAccess) { this.requestWriteAccess = requestWriteAccess; return this; }

        public LoginUrl build() { return new LoginUrl(url, forwardText, botUsername, requestWriteAccess); }
    }
}
