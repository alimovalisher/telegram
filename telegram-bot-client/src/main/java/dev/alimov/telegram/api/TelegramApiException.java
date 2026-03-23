package dev.alimov.telegram.api;

import org.springframework.http.HttpStatus;

public class TelegramApiException extends RuntimeException {
    private final int errorCode;

    public TelegramApiException(int errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
