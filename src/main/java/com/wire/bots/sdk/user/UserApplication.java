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
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.model.Access;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;


public class UserApplication {
    private StorageFactory storageFactory;
    private CryptoFactory cryptoFactory;
    private Client client;
    private MessageHandlerBase handler;
    private Configuration config;

    public void run() throws Exception {
        String email = config.userMode.email;
        String password = config.userMode.password;

        LoginClient loginClient = new LoginClient(client);
        Access access = loginClient.login(email, password);

        UUID userId = access.getUserId();

        Logger.info("Logged in as: %s userId: %s", email, userId);

        UserMessageResource userMessageResource = new UserMessageResource(handler)
                .addUserId(userId)
                .addClient(client)
                .addCryptoFactory(cryptoFactory)
                .addStorageFactory(storageFactory);

        String clientId = getDeviceId(userId);
        if (clientId == null) {
            clientId = newDevice(userId, password, access.getToken());
            saveDevice(userId, clientId);
            Logger.info("Created new device. clientId: %s", clientId);
        }

        ClientManager container = ClientManager.createClient();
        container.getProperties().put(ClientProperties.RECONNECT_HANDLER, new SocketReconnectHandler(5));

        URI wss = client
                .target(config.wsHost)
                .path("await")
                .path(access.getToken())
                .path(clientId)
                .getUri();

        Logger.info("Connecting websocket: %s", wss);

        container.connectToServer(new ClientEndpoint(userMessageResource), wss);
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

    public void saveDevice(UUID userId, String clientId) throws IOException {
        NewBot state = new NewBot();
        state.client = clientId;
        state.id = userId;
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
