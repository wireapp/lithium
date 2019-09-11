package com.wire.bots.sdk.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpException extends Exception {
    private int code;
    private String message;
    private String label;

    public HttpException(String message,
                         int code) {
        super(message);
        this.code = code;
        this.message = message;
    }

    @JsonCreator
    public HttpException(@JsonProperty("message") String message,
                         @JsonProperty("code") int code,
                         @JsonProperty("label") String label) {
        super(message);
        this.code = code;
        this.message = message;
        this.label = label;
    }

    public HttpException(int code) {
        this.code = code;
    }

    public HttpException() {
    }

    @Override
    public String toString() {
        String clazz = getClass().getSimpleName();
        return String.format("%s: code: %d, msg: %s, label: %s", clazz, code, message, label);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
