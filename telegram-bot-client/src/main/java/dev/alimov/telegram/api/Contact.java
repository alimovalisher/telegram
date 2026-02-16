package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Contact(
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        Long userId,
        String vcard
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String phoneNumber;
        private String firstName;
        private String lastName;
        private Long userId;
        private String vcard;

        public Builder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder vcard(String vcard) { this.vcard = vcard; return this; }

        public Contact build() { return new Contact(phoneNumber, firstName, lastName, userId, vcard); }
    }
}
