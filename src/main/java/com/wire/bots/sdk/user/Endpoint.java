//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.sdk.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.crypto.CryptoFile;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.InboundMessage;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.storage.FileStorage;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.model.Message;
import com.wire.bots.sdk.user.model.User;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Web Socket when running the sdk as a regular user and not as a bot Service
 */
@ClientEndpoint
public class Endpoint {
    private static final String WSS = "wss://%s-nginz-ssl.%s/await?access_token=%s&client=%s";
    private static final String PROD = "prod";
    private static final String ENV = "env";
    private final String path;
    private UserMessageResource userMessageResource;
    private ClientManager client = null;
    private String token;
    private String cookie;
    private String clientId;
    private String botId;

    public Endpoint(String path) throws CryptoException {
        this.path = path;
    }

    public Session connectWebSocket(UserMessageResource userMessageResource) throws Exception {
        if (client == null) {
            client = ClientManager.createClient();
        }
        this.userMessageResource = userMessageResource;

        return client.connectToServer(this, getPath());
    }

    /**
     * Signin as a regular Wire user
     *
     * @param email     Email address
     * @param password  Plain text password
     * @param persisted True if you want to renew token every 15 mins
     * @return UserId
     * @throws Exception
     */
    public String signIn(String email, String password, boolean persisted) throws Exception {
        User login = LoginClient.login(email, password);
        token = login.getToken();
        cookie = login.getCookie();
        botId = login.extractUserId();
        clientId = initDevice(botId, password, token);

        if (persisted)
            initRenewal(15);

        return botId;
    }

    @OnMessage
    public void onMessage(InputStream rawInput) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Message message = mapper.readValue(rawInput, Message.class);

        for (InboundMessage payload : message.payload) {
            //Logger.info(payload.type);
            try {
                userMessageResource.onNewMessage(botId, payload.conversation, payload);
            } catch (Exception e) {
                Logger.warning(e.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) throws Exception {
        Logger.info(String.format("Session closed: %s, %s", session.getId(), reason));
        //   client.connectToServer(this, getPath());
    }

    private URI getPath() throws URISyntaxException {
        String env = System.getProperty(ENV, PROD);
        String domain = Util.getDomain();

        String url = String.format(WSS,
                env,
                domain,
                token,
                clientId);
        return new URI(url);
    }

    /**
     * @param userId
     * @param password
     * @param token
     * @return ClientId
     * @throws Exception
     */
    private String initDevice(String userId, String password, String token) throws Exception {
        FileStorage storage = new FileStorage(path, userId);

        try {
            NewBot state = storage.getState();
            Logger.info("initDevice: Existing ClientID: %s", state.client);
            state.token = token;

            storage.saveState(state);
            return state.client;
        } catch (IOException ex) {
            // register new device
            try (CryptoFile cryptoFile = new CryptoFile(path, userId)) {
                PreKey key = cryptoFile.newLastPreKey();

                NewBot state = new NewBot();
                state.id = userId;
                state.client = LoginClient.registerClient(key, token, password);
                state.token = token;

                storage.saveState(state);
                Logger.info("initDevice: New ClientID: %s", state.client);
                return state.client;
            }
        }
    }

    private void initRenewal(int periodMinutes) {
        Timer timer = new Timer("RenewToken");
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    token = API.renewAccessToken(cookie, token);
                } catch (Exception e) {
                    Logger.warning("Failed periodic access_token renewal: " + e.getMessage());
                    e.printStackTrace();

                }
            }
        }, TimeUnit.MINUTES.toMillis(periodMinutes), TimeUnit.MINUTES.toMillis(periodMinutes));
    }

    public String getToken() {
        return token;
    }

    public String getClientId() {
        return clientId;
    }

    public String getBotId() {
        return botId;
    }

}
