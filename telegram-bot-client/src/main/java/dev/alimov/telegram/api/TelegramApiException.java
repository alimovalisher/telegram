package dev.alimov.telegram.api;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public class TelegramApiException extends RuntimeException {
    private final int errorCode;
    @Nullable
    private final Duration retryAfter;

    public TelegramApiException(int errorCode, String msg, @Nullable Duration retryAfter) {
        super(msg);
        this.errorCode = errorCode;
        this.retryAfter = retryAfter;
    }

    @Nullable
    public Duration getRetryAfter() {
        return retryAfter;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
