package com.wire.bots.sdk.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorMessage {
    @JsonProperty
    public String message;

    public ErrorMessage(String message) {
        this.message = message;
    }
}
