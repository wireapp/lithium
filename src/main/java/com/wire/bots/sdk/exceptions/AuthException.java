package com.wire.bots.sdk.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthException extends HttpException {
    public AuthException(String message, int code) {
        super(message, code);
    }

    public AuthException(int code) {
        super(code);
    }

    @JsonCreator
    public AuthException(@JsonProperty("message") String message,
                         @JsonProperty("code") int code,
                         @JsonProperty("label") String label) {
        super(message, code, label);
    }
}
