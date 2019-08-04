package com.wire.bots.sdk.factories;

import com.wire.bots.sdk.state.State;

import java.io.IOException;
import java.util.UUID;

public interface StorageFactory {
    State create(UUID botId) throws IOException;
}
