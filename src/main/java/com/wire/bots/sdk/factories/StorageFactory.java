package com.wire.bots.sdk.factories;

import com.wire.bots.sdk.storage.Storage;

public interface StorageFactory {
    Storage create(String botId) throws Exception;
}
