package com.wire.bots.sdk.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.server.model.NewBot;

import java.io.File;
import java.io.IOException;

public class FileStorage implements Storage {

    private final String path;
    private final String botId;

    public FileStorage(String path, String botId) {
        this.path = path;
        this.botId = botId;
        File f = new File(String.format("%s/%s", path, botId));
        f.mkdirs();
    }

    @Override
    public boolean saveState(NewBot newBot) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(getPath());
        mapper.writeValue(file, newBot);
        return true;
    }

    @Override
    public NewBot getState() throws Exception {
        File file = new File(getPath());
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, NewBot.class);
    }

    @Override
    public boolean removeState() throws Exception {
        File f = new File(getPath());
        return f.delete();
    }

    @Override
    public boolean status() throws Exception {
        File dir = new File(String.format("%s/%s", path, botId));
        return dir.exists();
    }

    @Override
    public String getPath() {
        return String.format("%s/%s/state.json", path, botId);
    }
}
