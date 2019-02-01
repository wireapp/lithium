package com.wire.bots.sdk.factories;

import com.wire.bots.sdk.state.State;

import java.io.IOException;

public interface StorageFactory {
    State create(String botId) throws IOException;
}
