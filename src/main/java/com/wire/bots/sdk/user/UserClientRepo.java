package com.wire.bots.sdk.user;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.state.State;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.UUID;

public class UserClientRepo extends ClientRepo {
    final private Object lock = new Object();
    private Crypto crypto;
    private State state;

    public UserClientRepo(Client httpClient, CryptoFactory cf, StorageFactory sf) {
        super(httpClient, cf, sf);
    }

    public WireClient getWireClient(UUID botId, UUID conv) throws CryptoException, IOException {
        synchronized (lock) {
            if (crypto == null || crypto.isClosed()) {
                crypto = cf.create(botId.toString());
            }
            if (state == null) {
                state = sf.create(botId.toString());
            }
        }
        return new UserClient(httpClient, crypto, state, conv);
    }

    /*
    We dont want to purge the state when running in UserMode
     */
    @Override
    public void purgeBot(String botId) {

    }
}
