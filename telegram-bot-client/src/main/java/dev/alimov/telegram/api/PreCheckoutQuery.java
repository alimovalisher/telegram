package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PreCheckoutQuery(String id,
                               User from,
                               String currency,
                               @JsonProperty("total_amount") Integer totalAmount,
                               @JsonProperty("invoice_payload") String invoicePayload) {
}
