package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response<T> {
    private boolean ok;
    private String description;
    @JsonProperty("error_code")
    private int errorCode;

    private T result;

    public boolean isOk() {
        return ok;
    }

    public Response<T> setOk(boolean ok) {
        this.ok = ok;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Response<T> setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Response<T> setErrorCode(int errorCode) {
        this.errorCode = errorCode;
        return this;
    }

//    public ResponseParameters getResponseParameters() {
//        return responseParameters;
//    }
//
//    public Response<T> setResponseParameters(ResponseParameters responseParameters) {
//        this.responseParameters = responseParameters;
//        return this;
//    }

    public T getResult() {
        return result;
    }

    public Response<T> setResult(T result) {
        this.result = result;
        return this;
    }
}
