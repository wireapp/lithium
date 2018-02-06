package com.wire.bots.sdk;

import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.storage.Storage;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRepo {
    protected final CryptoFactory cryptoFactory;
    protected final StorageFactory storageFactory;
    protected final ConcurrentHashMap<String, WireClient> clients = new ConcurrentHashMap<>();

    public ClientRepo(CryptoFactory cryptoFactory, StorageFactory storageFactory) {
        this.cryptoFactory = cryptoFactory;
        this.storageFactory = storageFactory;
    }

    public WireClient getWireClient(String botId) {
        synchronized (clients) {
            WireClient wireClient = clients.get(botId);
            if (wireClient == null || wireClient.isClosed()) {
                try {
                    Crypto crypto = cryptoFactory.create(botId);
                    Storage storage = storageFactory.create(botId);
                    wireClient = new BotClient(crypto, storage);
                    clients.put(botId, wireClient);
                } catch (Exception e) {
                    Logger.error("GetWireClient. BotId: %s, status: %s", botId, e.getMessage());
                }
            }
            return wireClient;
        }
    }

    public void removeClient(String botId) {
        synchronized (clients) {
            WireClient remove = clients.remove(botId);
            if (remove != null) {
                try {
                    remove.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void purgeBot(String botId) throws Exception {
        boolean purged = storageFactory.create(botId).removeState();
        if (!purged)
            Logger.error("Failed to purge bot: %s", botId);
    }
}
