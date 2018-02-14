package com.wire.bots.sdk;

import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.storage.Storage;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.ArrayList;
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
                Crypto crypto = cryptoFactory.create(k);
                Storage storage = storageFactory.create(k);
                return new BotClient(crypto, storage);
            } catch (Exception e) {
                Logger.error("GetWireClient. BotId: %s, status: %s", botId, e.getMessage());
                return null;
            }
        });
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
        removeClient(botId);
        if (!purged)
            Logger.error("Failed to purge bot: %s", botId);
    }

    public ArrayList<WireClient> listClients() throws Exception {
        ArrayList<WireClient> ret = new ArrayList<>();
        for (String bot : listAllBots()) {
            WireClient wireClient = getWireClient(bot);
            if (wireClient != null)
                ret.add(wireClient);
        }
        return ret;
    }

    private ArrayList<String> listAllBots() throws Exception {
        ArrayList<String> ret = new ArrayList<>();
        Storage storage = storageFactory.create("");
        for (NewBot newBot : storage.listAllStates()) {
            ret.add(newBot.id);
        }
        return ret;
    }

    public CryptoFactory getCryptoFactory() {
        return cryptoFactory;
    }

    public StorageFactory getStorageFactory() {
        return storageFactory;
    }
}
