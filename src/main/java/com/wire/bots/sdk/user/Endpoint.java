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
import com.wire.bots.sdk.user.model.Access;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Web Socket when running the sdk as a regular user and not as a bot Service
 */
@ClientEndpoint
public class Endpoint {
    protected static final int RENEW_PERIOD_MINUTES = 5;

    protected final Client httpClient;
    protected final StorageFactory storageFactory;
    protected final CryptoFactory cryptoFactory;
    protected UserMessageResource userMessageResource;
    protected User user;
    protected ClientManager clientManager;
    protected Session session;

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

        String clientId = initDevice(user.getUserId(), password, accessToken);
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
                    case "user.update":
                        userMessageResource.onUpdate(payload);
                        break;
                    case "user.connection":
                        userMessageResource.onNewMessage(payload.connection.from, payload.connection.convId, payload);
                        break;
                    case "conversation.otr-message-add":
                    case "conversation.member-join":
                    case "conversation.create":
                        userMessageResource.onNewMessage(payload.from, payload.convId, payload);
                        break;
                    default:
                        Logger.info("Unknown type: %s, from: %s", payload.type, payload.from);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.error("Endpoint:onMessage: %s %s", payload.type, e);
            }
        }
    }

    @OnClose
    public void onClose(Session closed, CloseReason reason) throws Exception {
        //Logger.debug("Session closed: %s, %s", closed.getId(), reason);
        Access access = getAccess();
        String wss = Util.getWss(access.token, access.clientId);
        session = clientManager.connectToServer(this, new URI(wss));
        //Logger.debug("New Session %s", this.session.getId());
    }

    private NewBot getState(UUID userId) throws IOException {
        return storageFactory.create(userId.toString()).getState();
    }

    /**
     * @param password
     * @param token
     * @return ClientId
     * @throws Exception
     */
    private String initDevice(UUID userId, String password, String token) throws Exception {
        NewBot state = initState(userId, password, token);
        state.token = token;

        // save the state with new token
        State storage = storageFactory.create(userId.toString());
        storage.saveState(state);
        return state.client;
    }

    private NewBot initState(UUID userId, String password, String token)
            throws IOException, HttpException, CryptoException {
        String botId = userId.toString();
        NewBot state;
        try {
            state = getState(userId);
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

    public void initRenewal() {
        Timer timer = new Timer("RenewToken");
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    Access access = getAccess();

//                    Logger.debug("Old access user: %s token: %s, cookie: %s",
//                            access.userId,
//                            access.token,
//                            access.cookie);

                    API api = new API(httpClient, null, access.token);
                    Access newAccess = api.renewAccessToken(access.cookie);

                    newAccess.cookie = newAccess.cookie != null ? newAccess.cookie : access.cookie;

                    boolean persisted = persistAccess(newAccess);

//                    Logger.debug("New access persisted: %s, user: %s token: %s, cookie: %s",
//                            persisted,
//                            newAccess.userId,
//                            newAccess.token,
//                            newAccess.cookie);

                    if (session != null)
                        session.close();
                } catch (Exception e) {
                    Logger.error("Failed periodic access_token renewal: %s", e);
                }
            }
        }, TimeUnit.MINUTES.toMillis(RENEW_PERIOD_MINUTES), TimeUnit.MINUTES.toMillis(RENEW_PERIOD_MINUTES));
    }

    protected Access getAccess() throws IOException {
        UUID userId = user.getUserId();
        State storage = storageFactory.create(userId.toString());
        NewBot state = storage.getState();

        Access access = new Access();
        access.userId = userId;
        access.token = state.token;
        access.cookie = user.getCookie();
        access.clientId = user.getClientId();

        return access;
    }

    protected boolean persistAccess(Access access) throws IOException {
        UUID userId = access.userId;
        State storage = storageFactory.create(userId.toString());
        NewBot state = storage.getState();
        state.token = access.token;
        return storage.saveState(state);
    }
}
