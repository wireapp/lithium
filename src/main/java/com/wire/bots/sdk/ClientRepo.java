package com.wire.bots.sdk;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.state.State;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRepo {
    protected final Client httpClient;
    protected final CryptoFactory cryptoFactory;
    protected final StorageFactory storageFactory;
    protected final ConcurrentHashMap<String, WireClient> clients = new ConcurrentHashMap<>();

    public ClientRepo(Client httpClient, CryptoFactory cryptoFactory, StorageFactory storageFactory) {
        this.httpClient = httpClient;
        this.cryptoFactory = cryptoFactory;
        this.storageFactory = storageFactory;
    }

    @Deprecated
    public WireClient getWireClient(String botId) {
        return clients.computeIfAbsent(botId, k -> {
            try {
                State storage = storageFactory.create(botId);
                Crypto crypto = cryptoFactory.create(botId);
                return new BotClient(httpClient, crypto, storage);
            } catch (Exception e) {
                return null;
            }
        });
    }

    public WireClient getClient(String botId) throws IOException, CryptoException {
        State storage = storageFactory.create(botId);
        Crypto crypto = cryptoFactory.create(botId);
        return new BotClient(httpClient, crypto, storage);
    }

    public void removeClient(String botId) throws IOException {
        WireClient remove = clients.remove(botId);
        if (remove != null) {
            remove.close();
        }
    }

    public void purgeBot(String botId) throws IOException {
        boolean purged = storageFactory.create(botId).removeState();
        removeClient(botId);
        if (!purged)
            throw new IOException("Failed to purge Bot: " + botId);
    }
}
