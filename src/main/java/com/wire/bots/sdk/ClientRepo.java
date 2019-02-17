package com.wire.bots.sdk;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.state.State;

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
            State storage = sf.create(botId);
            Crypto crypto = cf.create(botId);
            return new BotClient(httpClient, crypto, storage);
        } catch (Exception e) {
            return null;
        }
    }

    public WireClient getClient(String botId) throws IOException, CryptoException {
        State storage = sf.create(botId);
        Crypto crypto = cf.create(botId);
        return new BotClient(httpClient, crypto, storage);
    }

    @Deprecated
    public void removeClient(String botId) throws IOException {

    }

    public void purgeBot(String botId) throws IOException {
        boolean purged = sf.create(botId).removeState();
        if (!purged)
            throw new IOException("Failed to purge Bot: " + botId);
    }
}
