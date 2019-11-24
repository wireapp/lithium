package com.wire.bots.sdk.user;

import com.wire.bots.sdk.tools.Logger;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.CloseReason;

public class SocketReconnectHandler extends ClientManager.ReconnectHandler {

    private final int delay;    // seconds

    public SocketReconnectHandler(int delay) {
        this.delay = delay;
    }

    @Override
    public boolean onDisconnect(CloseReason closeReason) {
        Logger.debug("Websocket onDisconnect: reason: %s", closeReason.getCloseCode());
        return true;
    }

    @Override
    public boolean onConnectFailure(Exception e) {
        Logger.warning("Websocket onConnectFailure: reason: %s", e);
        return true;
    }

    @Override
    public long getDelay() {
        return delay;
    }
}
