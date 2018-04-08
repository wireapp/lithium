package com.wire.bots.sdk;

import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.state.State;
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
        return clients.computeIfAbsent(botId, k -> {
            try {
                State storage = storageFactory.create(botId);
                Crypto crypto = cryptoFactory.create(botId);
                return new BotClient(crypto, storage);
            } catch (Exception e) {
                Logger.error("GetWireClient. BotId: %s %s", botId, e);
                return null;
            }
        });
    }

    public void removeClient(String botId) {
        WireClient remove = clients.remove(botId);
        if (remove != null) {
            try {
                remove.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void purgeBot(String botId) throws Exception {
        boolean purged = storageFactory.create(botId).removeState();
        removeClient(botId);
        if (!purged)
            Logger.error("Failed to purge bot: %s", botId);
    }
}
