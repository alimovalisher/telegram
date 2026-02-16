package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SuccessfulPayment(String currency,
                                @JsonProperty("total_amount") int totalAmount,
                                @JsonProperty("invoice_payload") String invoicePayload,
                                @JsonProperty("shipping_option_id") String shippingOptionId,
                                @JsonProperty("order_info") String orderInfo,
                                @JsonProperty("telegram_payment_charge_id") String telegramPaymentChargeId,
                                @JsonProperty("provider_payment_charge_id") String providerPaymentChargeId) {
}
