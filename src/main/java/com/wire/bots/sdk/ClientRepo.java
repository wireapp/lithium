package com.wire.bots.sdk;

import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.factories.WireClientFactory;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRepo {
    protected final WireClientFactory wireClientFactory;
    protected final StorageFactory storageFactory;
    protected final ConcurrentHashMap<String, WireClient> clients = new ConcurrentHashMap<>();

    public ClientRepo(WireClientFactory factory, StorageFactory storageFactory) {
        this.wireClientFactory = factory;
        this.storageFactory = storageFactory;
    }

    public WireClient getWireClient(String botId) {
        synchronized (clients) {
            WireClient wireClient = clients.get(botId);
            if (wireClient == null || wireClient.isClosed()) {
                try {
                    wireClient = wireClientFactory.create(botId);
                    WireClient old = clients.put(botId, wireClient);
                    if (old != null)
                        old.close();
                } catch (Exception e) {
                    Logger.error("GetWireClient. BotId: %s, status: %s", botId, e.getLocalizedMessage());
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
