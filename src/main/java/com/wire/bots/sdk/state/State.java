package com.wire.bots.sdk.state;

import com.wire.bots.sdk.server.model.NewBot;

import java.util.ArrayList;

public interface State {

    boolean saveState(NewBot newBot) throws Exception;

    NewBot getState() throws Exception;

    boolean removeState() throws Exception;

    ArrayList<NewBot> listAllStates() throws Exception;

    boolean saveFile(String filename, String content) throws Exception;

    String readFile(String filename) throws Exception;

    boolean deleteFile(String filename) throws Exception;

    boolean saveGlobalFile(String filename, String content) throws Exception;

    String readGlobalFile(String filename) throws Exception;

    boolean deleteGlobalFile(String filename) throws Exception;

}
