package com.wire.bots.sdk;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.state.State;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.UUID;

public class ClientRepo {
    protected final Client httpClient;
    protected final CryptoFactory cf;
    protected final StorageFactory sf;

    public ClientRepo(Client httpClient, CryptoFactory cf, StorageFactory sf) {
        this.httpClient = httpClient;
        this.cf = cf;
        this.sf = sf;
    }

    public WireClient getClient(UUID botId) throws IOException, CryptoException {
        NewBot state = sf.create(botId).getState();
        Crypto crypto = cf.create(botId);
        API api = new API(httpClient, state.token);
        return new BotClient(state, crypto, api);
    }

    public void purgeBot(UUID botId) throws IOException {
        State state = sf.create(botId);
        if (state == null)
            return;

        boolean purged = state.removeState();
        if (!purged)
            throw new IOException("Failed to purge Bot: " + botId);
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public CryptoFactory getCf() {
        return cf;
    }

    public StorageFactory getSf() {
        return sf;
    }
}
