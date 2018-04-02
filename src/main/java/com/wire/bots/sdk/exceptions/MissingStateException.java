package com.wire.bots.sdk.exceptions;

import java.util.UUID;

public class MissingStateException extends Exception {
    public MissingStateException(UUID botId) {
        super("Unknown botId: " + botId);
    }
}
