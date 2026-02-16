package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InlineKeyboardButton(
        @JsonProperty("text")
        String text,
        @JsonProperty("url")
        String url,
        @JsonProperty("callback_data") String callbackData,
        @JsonProperty("web_app") WebAppInfo webApp,
        @JsonProperty("login_url") LoginUrl loginUrl
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String text;
        private String url;
        private String callbackData;
        private WebAppInfo webApp;
        private LoginUrl loginUrl;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder callbackData(String callbackData) {
            this.callbackData = callbackData;
            return this;
        }

        public Builder webApp(WebAppInfo webApp) {
            this.webApp = webApp;
            return this;
        }

        public Builder loginUrl(LoginUrl loginUrl) {
            this.loginUrl = loginUrl;
            return this;
        }

        public InlineKeyboardButton build() {
            return new InlineKeyboardButton(text, url, callbackData, webApp, loginUrl);
        }
    }
}
