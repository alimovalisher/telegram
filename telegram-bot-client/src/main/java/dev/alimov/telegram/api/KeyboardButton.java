package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeyboardButton(
        String text,
        @JsonProperty("request_contact") Boolean requestContact,
        @JsonProperty("request_location") Boolean requestLocation
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String text;
        private Boolean requestContact;
        private Boolean requestLocation;

        public Builder text(String text) { this.text = text; return this; }
        public Builder requestContact(Boolean requestContact) { this.requestContact = requestContact; return this; }
        public Builder requestLocation(Boolean requestLocation) { this.requestLocation = requestLocation; return this; }

        public KeyboardButton build() { return new KeyboardButton(text, requestContact, requestLocation); }
    }
}
