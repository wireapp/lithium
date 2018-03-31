package com.wire.bots.sdk.user;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.storage.Storage;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;

public class UserClientRepo extends ClientRepo {
    public UserClientRepo(CryptoFactory cryptoFactory, StorageFactory storageFactory) {
        super(cryptoFactory, storageFactory);
    }

    public WireClient getWireClient(String botId, String conv) throws CryptoException, IOException {
        synchronized (clients) {
            String key = String.format("%s-%s", botId, conv);
            WireClient wireClient = clients.get(key);
            if (wireClient == null || wireClient.isClosed()) {
                try {
                    Crypto crypto = cryptoFactory.create(botId);
                    Storage storage = storageFactory.create(botId);

                    wireClient = new UserClient(crypto, storage, conv);
                    clients.put(key, wireClient);
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.error("GetWireClient. BotId: %s, conv: %s, status: %s",
                            botId,
                            conv,
                            e.getMessage());
                }
            }
            return wireClient;
        }
    }
}
