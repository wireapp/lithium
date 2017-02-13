package com.wire.bots.sdk;

import com.wire.cryptobox.CryptoException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ClientRepo {
    private final WireClientFactory factory;
    private final Configuration conf;
    private final HashMap<String, WireClient> clients = new HashMap<>();

    public ClientRepo(WireClientFactory factory, Configuration conf) {
        this.factory = factory;
        this.conf = conf;
    }

    public WireClient getWireClient(String botId) throws CryptoException, IOException {
        return getWireClient(botId, null);
    }

    public WireClient getWireClient(String botId, String convId) throws CryptoException, IOException {
        synchronized (clients) {
            WireClient wireClient = clients.get(botId);
            if (wireClient == null || wireClient.isClosed()) {
                String path = String.format("%s/%s", conf.getCryptoDir(), botId);
                String clientId = Util.readLine(new File(path + "/client.id"));
                String token = Util.readLine(new File(path + "/token.id"));

                wireClient = factory.createClient(botId, convId, clientId, token);
                WireClient old = clients.put(botId, wireClient);
                if (old != null)
                    old.close();
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
}
