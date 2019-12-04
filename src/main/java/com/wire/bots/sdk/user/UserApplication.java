package com.wire.bots.sdk.user;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.state.State;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.model.Access;
import com.wire.bots.sdk.user.model.Event;
import com.wire.bots.sdk.user.model.NotificationList;
import io.dropwizard.setup.Environment;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

import javax.annotation.Nullable;
import javax.websocket.*;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ClientEndpoint(decoders = EventDecoder.class)
public class UserApplication {
    private static final int SIZE = 30;
    private final ScheduledExecutorService renewal;

    private StorageFactory storageFactory;
    private CryptoFactory cryptoFactory;
    private Client client;
    private MessageHandlerBase handler;
    private Configuration config;
    private UserMessageResource userMessageResource;
    private ClientManager container;
    private UUID userId;
    private Session session;

    public UserApplication(Environment env) {
        renewal = env.lifecycle().scheduledExecutorService("access renewal").build();
    }

    public void run() throws Exception {
        String email = config.userMode.email;
        String password = config.userMode.password;

        LoginClient loginClient = new LoginClient(client);
        Access access = loginClient.login(email, password);

        userId = access.getUserId();

        String clientId = getDeviceId(userId);
        if (clientId == null) {
            clientId = newDevice(userId, password, access.getToken());
            Logger.info("Created new device. clientId: %s", clientId);
        }

        NewBot state = updateState(userId, clientId, access.getToken(), null);

        Logger.info("Logged in as: %s, userId: %s, clientId: %s", email, state.id, state.client);

        final String deviceId = state.client;
        renewal.scheduleAtFixedRate(() -> {
            try {
                Access newAccess = loginClient.renewAccessToken(access.getCookie());
                updateState(userId, deviceId, newAccess.getToken(), null);
                Logger.info("Updated access token. Exp in: %d sec, cookie: %s", newAccess.expire, newAccess.getCookie() != null);
            } catch (Exception e) {
                Logger.warning("Token renewal error: %s", e);
            }
        }, access.expire - 10, access.expire, TimeUnit.SECONDS);

        renewal.scheduleAtFixedRate(() -> {
            try {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap("ping".getBytes("UTF-8")));
            } catch (Exception e) {
                Logger.warning("Ping error: %s", e);
            }
        }, 10, 10, TimeUnit.SECONDS);

        userMessageResource = new UserMessageResource(handler)
                .addUserId(userId)
                .addClient(client)
                .addCryptoFactory(cryptoFactory)
                .addStorageFactory(storageFactory);

        // Pull from notification stream
        NotificationList notificationList = loginClient.retrieveNotifications(state.client, since(state), state.token, SIZE);
        while (!notificationList.notifications.isEmpty()) {
            for (Event notification : notificationList.notifications) {
                onMessage(notification);
                state = updateState(userId, state.client, state.token, notification.id);
            }
            notificationList = loginClient.retrieveNotifications(state.client, since(state), state.token, SIZE);
        }

        // connect the Websocket
        container = ClientManager.createClient();
        container.getProperties().put(ClientProperties.RECONNECT_HANDLER, new SocketReconnectHandler(5));
        container.setDefaultMaxSessionIdleTimeout(-1);

        session = connectSocket();
        Logger.info("Websocket %s uri: %s", session.isOpen(), session.getRequestURI());
    }

    @Nullable
    private UUID since(NewBot state) {
        return state.locale != null ? UUID.fromString(state.locale) : null;
    }

    @OnMessage
    public void onMessage(Event event) {
        if (event == null)
            return;

        for (Payload payload : event.payload) {
            try {
                switch (payload.type) {
                    case "team.member-join":
                    case "user.update":
                        userMessageResource.onUpdate(event.id, payload);
                        break;
                    case "user.connection":
                        userMessageResource.onNewMessage(
                                event.id,
                                /* payload.connection.from, */ //todo check this!!
                                payload.connection.convId,
                                payload);
                        break;
                    case "conversation.otr-message-add":
                    case "conversation.member-join":
                    case "conversation.create":
                        userMessageResource.onNewMessage(
                                event.id,
                                payload.convId,
                                payload);
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

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) throws IOException {
        Logger.debug("Session opened: %s", session.getId());
    }

    @OnClose
    public void onClose(Session closed, CloseReason reason) throws IOException, DeploymentException {
        Logger.debug("Session closed: %s, %s", closed.getId(), reason);
        session = connectSocket();
    }

    private Session connectSocket() throws IOException, DeploymentException {
        NewBot newBot = storageFactory
                .create(userId)
                .getState();

        URI wss = client
                .target(config.wsHost)
                .path("await")
                .queryParam("client", newBot.client)
                .queryParam("access_token", newBot.token)
                .getUri();

        return container.connectToServer(this, wss);
    }

    public String newDevice(UUID userId, String password, String token) throws CryptoException, HttpException {
        Crypto crypto = cryptoFactory.create(userId);
        LoginClient loginClient = new LoginClient(client);

        ArrayList<PreKey> preKeys = crypto.newPreKeys(0, 20);
        PreKey lastKey = crypto.newLastPreKey();

        return loginClient.registerClient(token, password, preKeys, lastKey, "tablet", "permanent", "lithium");
    }

    public String getDeviceId(UUID userId) {
        try {
            return storageFactory.create(userId).getState().client;
        } catch (IOException ex) {
            return null;
        }
    }

    public NewBot updateState(UUID userId, String clientId, String token, @Nullable UUID last) throws IOException {
        State state = storageFactory.create(userId);

        NewBot newBot;
        try {
            newBot = state.getState();
        } catch (IOException ex) {
            newBot = new NewBot();
            newBot.id = userId;
            newBot.client = clientId;
        }

        newBot.token = token;
        if (last != null)
            newBot.locale = last.toString();

        state.saveState(newBot);
        return state.getState();
    }

    public UserApplication addConfig(Configuration config) {
        this.config = config;
        return this;
    }

    public UserApplication addStorageFactory(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
        return this;
    }

    public UserApplication addCryptoFactory(CryptoFactory cryptoFactory) {
        this.cryptoFactory = cryptoFactory;
        return this;
    }

    public UserApplication addClient(Client client) {
        this.client = client;
        return this;
    }

    public UserApplication addHandler(MessageHandlerBase handler) {
        this.handler = handler;
        return this;
    }


}
