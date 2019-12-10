package com.wire.bots.sdk.state;

import com.wire.bots.sdk.server.model.NewBot;

import java.io.IOException;

public interface State {

    boolean saveState(NewBot newBot) throws IOException;

    NewBot getState() throws IOException;

    boolean removeState() throws IOException;

}
