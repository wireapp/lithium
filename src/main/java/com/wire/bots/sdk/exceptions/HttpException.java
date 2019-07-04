package com.wire.bots.sdk.exceptions;

public class HttpException extends Exception {
    private int statusCode;

    public HttpException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpException(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        String clazz = getClass().getSimpleName();
        return String.format("%s: %s, status: %d", clazz, getMessage(), getStatusCode());
    }
}
