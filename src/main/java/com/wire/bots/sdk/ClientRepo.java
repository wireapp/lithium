package com.wire.bots.sdk;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;

import javax.ws.rs.client.Client;
import java.io.IOException;

public class ClientRepo {
    protected final Client httpClient;
    protected final CryptoFactory cf;
    protected final StorageFactory sf;

    public ClientRepo(Client httpClient, CryptoFactory cf, StorageFactory sf) {
        this.httpClient = httpClient;
        this.cf = cf;
        this.sf = sf;
    }

    @Deprecated
    public WireClient getWireClient(String botId) {
        try {
            NewBot state = sf.create(botId).getState();
            Crypto crypto = cf.create(botId);
            API api = new API(httpClient, state.token);
            return new BotClient(state, crypto, api);
        } catch (Exception e) {
            return null;
        }
    }

    public WireClient getClient(String botId) throws IOException, CryptoException {
        NewBot state = sf.create(botId).getState();
        Crypto crypto = cf.create(botId);
        API api = new API(httpClient, state.token);
        return new BotClient(state, crypto, api);
    }

    @Deprecated
    public void removeClient(String botId) {

    }

    public void purgeBot(String botId) throws IOException {
        boolean purged = sf.create(botId).removeState();
        if (!purged)
            throw new IOException("Failed to purge Bot: " + botId);
    }
}
