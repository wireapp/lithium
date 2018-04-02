package com.wire.bots.sdk.factories;

import com.wire.bots.sdk.state.State;

public interface StorageFactory {
    State create(String botId) throws Exception;
}
