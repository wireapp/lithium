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
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.state.State;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.model.Message;
import com.wire.bots.sdk.user.model.User;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Web Socket when running the sdk as a regular user and not as a bot Service
 */
@ClientEndpoint
public class Endpoint {
    private static final int RENEW_PERIOD_MINUTES = 15;

    private final Client httpClient;
    private final StorageFactory storageFactory;
    private final CryptoFactory cryptoFactory;
    private UserMessageResource userMessageResource;
    private User user;
    private ClientManager clientManager;
    private Session session;

    public Endpoint(Client httpClient, CryptoFactory cryptoFactory, StorageFactory storageFactory) {
        this.httpClient = httpClient;
        this.storageFactory = storageFactory;
        this.cryptoFactory = cryptoFactory;
        this.clientManager = ClientManager.createClient();
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
    public User signIn(String email, String password, boolean persisted) throws Exception {
        LoginClient loginClient = new LoginClient(httpClient);
        user = loginClient.login(email, password, persisted);

        String accessToken = user.getToken();

        String clientId = initDevice(password, accessToken);
        user.setClientId(clientId);

        if (persisted)
            initRenewal();

        return user;
    }

    public void connectWebSocket(UserMessageResource userMessageResource, URI wss)
            throws IOException, DeploymentException {
        this.userMessageResource = userMessageResource;
        this.session = clientManager.connectToServer(this, wss);
    }

    @OnMessage
    public void onMessage(InputStream rawInput) throws Exception {
        byte[] bytes = Util.toByteArray(rawInput);
        Logger.debug("Endpoint:onMessage %s", new String(bytes));

        ObjectMapper mapper = new ObjectMapper();
        Message message = mapper.readValue(bytes, Message.class);

        for (Payload payload : message.payload) {
            try {
                switch (payload.type) {
                    case "team.member-join":
                        userMessageResource.onUpdate(payload);
                        break;
                    case "user.update":
                        userMessageResource.onUpdate(payload);
                        break;
                    case "user.connection":
                        userMessageResource.onNewMessage(user.getUserId(), payload.connection.conversation, payload);
                        break;
                    default:
                        userMessageResource.onNewMessage(user.getUserId(), payload.convId, payload);
                        break;
                }
            } catch (Exception e) {
                Logger.error("Endpoint:onMessage: %s", e);
            }
        }
    }

    @OnClose
    public void onClose(Session closed, CloseReason reason) throws Exception {
        Logger.debug("Session closed: %s, %s", closed.getId(), reason);
        NewBot state = getState();
        String wss = Util.getWss(state.token, state.client);
        session = clientManager.connectToServer(this, new URI(wss));
        Logger.debug("New Session %s", this.session.getId());
    }

    private NewBot getState() throws IOException {
        return storageFactory.create(user.getUserId().toString()).getState();
    }

    /**
     * @param password
     * @param token
     * @return ClientId
     * @throws Exception
     */
    private String initDevice(String password, String token) throws Exception {
        NewBot state = initState(password, token);
        state.token = token;

        // save the state with new token
        State storage = storageFactory.create(user.getUserId().toString());
        storage.saveState(state);
        return state.client;
    }

    private NewBot initState(String password, String token) throws IOException, HttpException, CryptoException {
        String botId = user.getUserId().toString();
        NewBot state;
        try {
            state = getState();
            Logger.info("initDevice: Existing ClientID: %s", state.client);
        } catch (IOException ex) {
            // register new device
            try (Crypto crypto = cryptoFactory.create(botId)) {
                LoginClient loginClient = new LoginClient(httpClient);

                state = new NewBot();
                state.id = botId;
                state.client = loginClient.registerClient(crypto.newLastPreKey(), token, password);

                Logger.info("initDevice: New ClientID: %s", state.client);
            }
        }
        return state;
    }

    private void initRenewal() {
        Timer timer = new Timer("RenewToken");
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    State storage = storageFactory.create(user.getUserId().toString());
                    NewBot state = storage.getState();

                    API api = new API(httpClient, null, state.token);
                    User newUser = api.renewAccessToken(user.getCookie());
                    state.token = newUser.getToken();
                    storage.saveState(state);

                    Logger.debug("New access token: %s", newUser.getToken());

                    session.close();
                } catch (Exception e) {
                    Logger.error("Failed periodic access_token renewal: " + e.getMessage());
                }
            }
        }, TimeUnit.MINUTES.toMillis(RENEW_PERIOD_MINUTES), TimeUnit.MINUTES.toMillis(RENEW_PERIOD_MINUTES));
    }
}
