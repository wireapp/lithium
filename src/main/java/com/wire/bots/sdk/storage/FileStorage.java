package com.wire.bots.sdk.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.server.model.NewBot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileStorage implements Storage {

    private final String path;
    private final String botId;

    public FileStorage(String path, String botId) {
        this.path = path;
        this.botId = botId;
        File dir = new File(String.format("%s/%s", path, botId));
        dir.mkdirs();
    }

    @Override
    public boolean saveState(NewBot newBot) throws Exception {
        File file = getStateFile();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(file, newBot);
        return true;
    }

    @Override
    public NewBot getState() throws Exception {
        File file = getStateFile();
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, NewBot.class);
    }

    @Override
    public boolean removeState() throws Exception {
        File file = getStateFile();
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }
        return file.delete();
    }

    @Override
    public boolean saveFile(String filename, String content) throws Exception {
        File file = getFile(filename);
        Files.write(file.toPath(), content.getBytes());
        return true;
    }

    @Override
    public String readFile(String filename) throws Exception {
        File file = getFile(filename);
        return new String(Files.readAllBytes(file.toPath()));
    }

    @Override
    public boolean deleteFile(String filename) throws Exception {
        File file = getFile(filename);
        return file.delete();
    }

    private File getStateFile() {
        return getFile("state.json");
    }

    private File getFile(String filename) {
        return new File(String.format("%s/%s/%s", path, botId, filename));
    }
}
