package com.wire.bots.sdk.storage;

import com.wire.bots.sdk.server.model.NewBot;

public interface Storage {

    boolean saveState(NewBot newBot) throws Exception;

    NewBot getState() throws Exception;

    boolean removeState() throws Exception;

    boolean saveFile(String filename, String content) throws Exception;

    String readFile(String filename) throws Exception;

    boolean deleteFile(String filename) throws Exception;

}
