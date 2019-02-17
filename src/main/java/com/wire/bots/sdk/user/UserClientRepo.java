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
    public UserClientRepo(Client httpClient, CryptoFactory cryptoFactory, StorageFactory storageFactory) {
        super(httpClient, cryptoFactory, storageFactory);
    }

    public WireClient getWireClient(UUID botId, UUID conv) throws CryptoException, IOException {
        Crypto crypto = cryptoFactory.create(botId.toString());
        State storage = storageFactory.create(botId.toString());
        return new UserClient(httpClient, crypto, storage, conv);
    }

    @Override
    public void removeClient(String botId) {

    }

    /*
    We dont want to purge the state when running in UserMode
     */
    @Override
    public void purgeBot(String botId) {

    }
}
