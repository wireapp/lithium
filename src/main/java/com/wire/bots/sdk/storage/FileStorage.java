package com.wire.bots.sdk.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.server.model.NewBot;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

public class FileStorage implements Storage {

    private static final String STATE_FILENAME = "state.json";
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
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, NewBot.class);
    }

    @Override
    public boolean removeState() throws Exception {
        File file = getStateFile();
        return file.delete();
    }

    @Override
    public ArrayList<NewBot> listAllStates() throws Exception {
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
