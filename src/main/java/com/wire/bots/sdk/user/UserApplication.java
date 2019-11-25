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
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.model.Access;
import com.wire.bots.sdk.user.model.Message;
import io.dropwizard.setup.Environment;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

import javax.websocket.*;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ClientEndpoint(decoders = MessageDecoder.class)
public class UserApplication {
    private final ScheduledExecutorService renewal;

    private StorageFactory storageFactory;
    private CryptoFactory cryptoFactory;
    private Client client;
    private MessageHandlerBase handler;
    private Configuration config;
    private UserMessageResource userMessageResource;
    private ClientManager container;
    private String clientId;
    private UUID userId;

    public UserApplication(Environment env) {
        renewal = env.lifecycle().scheduledExecutorService("access renewal").build();
    }

    public void run() throws Exception {
        String email = config.userMode.email;
        String password = config.userMode.password;

        LoginClient loginClient = new LoginClient(client);
        Access access = loginClient.login(email, password);

        userId = access.getUserId();

        Logger.info("Logged in as: %s userId: %s", email, userId);

        clientId = getDeviceId(userId);
        if (clientId == null) {
            clientId = newDevice(userId, password, access.getToken());
            Logger.info("Created new device. clientId: %s", clientId);
        }

        saveDevice(userId, clientId, access.getToken());

        renewal.scheduleAtFixedRate(() -> {
            try {
                Access newAccess = loginClient.renewAccessToken(access.getCookie());
                saveDevice(userId, clientId, newAccess.getToken());
                Logger.debug("Updated access token");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, access.expire, access.expire, TimeUnit.SECONDS);

        userMessageResource = new UserMessageResource(handler)
                .addUserId(userId)
                .addClient(client)
                .addCryptoFactory(cryptoFactory)
                .addStorageFactory(storageFactory);

        container = ClientManager.createClient();
        container.getProperties().put(ClientProperties.RECONNECT_HANDLER, new SocketReconnectHandler(5));
        container.setDefaultMaxSessionIdleTimeout(-1);

        connectSocket();
    }

    @OnMessage
    public void onMessage(Message message) {
        for (Payload payload : message.payload) {
            try {
                switch (payload.type) {
                    case "team.member-join":
                    case "user.update":
                        userMessageResource.onUpdate(message.id, payload);
                        break;
                    case "user.connection":
                        userMessageResource.onNewMessage(
                                message.id,
                                /* payload.connection.from, */ //todo check this!!
                                payload.connection.convId,
                                payload);
                        break;
                    case "conversation.otr-message-add":
                    case "conversation.member-join":
                    case "conversation.create":
                        userMessageResource.onNewMessage(
                                message.id,
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
    public void onOpen(Session session, EndpointConfig config) {
        Logger.info("Session opened: %s", session.getId());
    }

    @OnClose
    public void onClose(Session closed, CloseReason reason) throws IOException, DeploymentException {
        Logger.warning("Session closed: %s, %s", closed.getId(), reason);
        connectSocket();
    }

    private void connectSocket() throws IOException, DeploymentException {
        NewBot newBot = storageFactory.create(userId).getState();

        URI wss = client
                .target(config.wsHost)
                .path("await")
                .path(newBot.token)
                .path(clientId)
                .getUri();

        Logger.info("Connecting websocket: %s", config.wsHost);

        container.connectToServer(this, wss);
    }

    public String newDevice(UUID userId, String password, String token) throws CryptoException, HttpException {
        Crypto crypto = cryptoFactory.create(userId);
        LoginClient loginClient = new LoginClient(client);

        ArrayList<PreKey> preKeys = crypto.newPreKeys(0, 20);
        PreKey lastKey = crypto.newLastPreKey();

        return loginClient.registerClient(token, password, preKeys, lastKey, "tablet", "permanent", "wbots");
    }

    public String getDeviceId(UUID userId) {
        try {
            return storageFactory.create(userId).getState().client;
        } catch (IOException ex) {
            return null;
        }
    }

    public void saveDevice(UUID userId, String clientId, String token) throws IOException {
        NewBot state = new NewBot();
        state.id = userId;
        state.client = clientId;
        state.token = token;
        storageFactory.create(userId).saveState(state);
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
