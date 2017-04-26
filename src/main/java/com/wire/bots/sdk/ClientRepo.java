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

    public WireClient getWireClient(String botId) {
        synchronized (clients) {
            WireClient wireClient = clients.get(botId);
            if (wireClient == null || wireClient.isClosed()) {
                File clientFile = new File(String.format("%s/%s/client.id", conf.getCryptoDir(), botId));
                File tokenFile = new File(String.format("%s/%s/token.id", conf.getCryptoDir(), botId));
                File convFile = new File(String.format("%s/%s/conversation.id", conf.getCryptoDir(), botId));

                if (!clientFile.exists() || !tokenFile.exists())
                    return null;

                try {
                    String clientId = Util.readLine(clientFile);
                    String token = Util.readLine(tokenFile);
                    String conv = convFile.exists() ? Util.readLine(convFile) : null;

                    wireClient = factory.createClient(botId, conv, clientId, token);
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

    @Deprecated
    public WireClient getWireClient(String botId, String ignored) throws CryptoException, IOException {
        return getWireClient(botId);
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

    public void purgeBot(String botId) {
        File clientFile = new File(String.format("%s/%s/client.id", conf.getCryptoDir(), botId));
        File tokenFile = new File(String.format("%s/%s/token.id", conf.getCryptoDir(), botId));
        File convFile = new File(String.format("%s/%s/conversation.id", conf.getCryptoDir(), botId));

        clientFile.delete();
        tokenFile.delete();
        convFile.delete();
    }
}
