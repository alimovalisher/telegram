package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseParameters(@JsonProperty("retry_after") Integer retryAfter) {


}
