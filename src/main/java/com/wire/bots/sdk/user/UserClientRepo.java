package com.wire.bots.sdk.user;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.UUID;

public class UserClientRepo extends ClientRepo {
    public UserClientRepo(Client httpClient, CryptoFactory cf, StorageFactory sf) {
        super(httpClient, cf, sf);
    }

    public WireClient getWireClient(UUID userId, UUID conv) throws CryptoException, IOException {
        Crypto crypto = cf.create(userId.toString());
        NewBot state = sf.create(userId.toString()).getState();
        API api = new API(httpClient, conv, state.token);
        return new UserClient(state, conv, crypto, api);
    }

    /*
    We dont want to purge the state when running in UserMode
     */
    @Override
    public void purgeBot(String botId) {

    }
}
