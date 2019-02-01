package com.wire.bots.sdk.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.exceptions.MissingStateException;
import com.wire.bots.sdk.server.model.NewBot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

public class FileState implements State {

    private static final String STATE_FILENAME = "state.json";
    private final String path;
    private final String botId;

    public FileState(String path, String botId) {
        this.path = path;
        this.botId = botId;
        File dir = new File(String.format("%s/%s", path, botId));
        dir.mkdirs();
    }

    @Override
    public boolean saveState(NewBot newBot) throws IOException {
        File file = getStateFile();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(file, newBot);
        return true;
    }

    @Override
    public NewBot getState() throws IOException {
        File file = getStateFile();
        if (!file.exists())
            throw new MissingStateException(UUID.fromString(botId));

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, NewBot.class);
    }

    @Override
    public boolean removeState() {
        File file = getStateFile();
        return file.delete();
    }

    @Override
    public ArrayList<NewBot> listAllStates() throws IOException {
        ArrayList<NewBot> ret = new ArrayList<>();
        File dir = new File(path);
        for (String botId : dir.list()) {
            File stateFile = new File(String.format("%s/%s/%s", path, botId, STATE_FILENAME));
            if (stateFile.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                NewBot newBot = mapper.readValue(stateFile, NewBot.class);
                ret.add(newBot);
            }
        }
        return ret;
    }

    @Override
    public boolean saveFile(String filename, String content) throws IOException {
        File file = getFile(filename);
        Files.write(file.toPath(), content.getBytes());
        return true;
    }

    @Override
    public String readFile(String filename) throws IOException {
        File file = getFile(filename);
        return new String(Files.readAllBytes(file.toPath()));
    }

    @Override
    public boolean deleteFile(String filename) {
        File file = getFile(filename);
        return file.delete();
    }

    @Override
    public boolean saveGlobalFile(String filename, String content) throws IOException {
        return saveFile(filename, content);
    }

    @Override
    public String readGlobalFile(String filename) throws IOException {
        return readFile(filename);
    }

    @Override
    public boolean deleteGlobalFile(String filename) throws IOException {
        return deleteFile(filename);
    }

    public boolean hasState() {
        File stateFile = getStateFile();
        return stateFile.exists();
    }

    public boolean hasFile(String filename) {
        File stateFile = getFile(filename);
        return stateFile.exists();
    }

    private File getStateFile() {
        return getFile(STATE_FILENAME);
    }

    private File getFile(String filename) {
        return new File(String.format("%s/%s/%s", path, botId, filename));
    }
}
