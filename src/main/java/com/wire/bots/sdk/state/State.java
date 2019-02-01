package com.wire.bots.sdk.state;

import com.wire.bots.sdk.server.model.NewBot;

import java.io.IOException;
import java.util.ArrayList;

public interface State {

    boolean saveState(NewBot newBot) throws IOException;

    NewBot getState() throws IOException;

    boolean removeState() throws IOException;

    ArrayList<NewBot> listAllStates() throws IOException;

    boolean saveFile(String filename, String content) throws IOException;

    String readFile(String filename) throws IOException;

    boolean deleteFile(String filename) throws IOException;

    boolean saveGlobalFile(String filename, String content) throws IOException;

    String readGlobalFile(String filename) throws IOException;

    boolean deleteGlobalFile(String filename) throws IOException;

}
