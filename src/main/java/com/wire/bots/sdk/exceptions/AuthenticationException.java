package com.wire.bots.sdk.exceptions;

public class AuthenticationException extends HttpException {
    public AuthenticationException(String message) {
        super(message, 403);
    }
}
