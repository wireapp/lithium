package com.wire.bots.sdk.storage;

import com.wire.bots.sdk.server.model.NewBot;

public interface Storage {
    NewBot getState() throws Exception;

    boolean saveState(NewBot newBot) throws Exception;

    boolean status() throws Exception;

    boolean removeState() throws Exception;

    String getPath();
}
