package com.wire.bots.sdk.user;

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.factories.WireClientFactory;
import com.wire.bots.sdk.tools.Logger;
import com.wire.cryptobox.CryptoException;

import java.io.IOException;

public class UserClientRepo extends ClientRepo {
    public UserClientRepo(WireClientFactory factory, StorageFactory storageFactory) {
        super(factory, storageFactory);
    }

    public WireClient getWireClient(String botId, String conv) throws CryptoException, IOException {
        synchronized (clients) {
            String key = String.format("%s-%s", botId, conv);
            WireClient wireClient = clients.get(key);
            if (wireClient == null || wireClient.isClosed()) {
                try {
                    wireClient = wireClientFactory.create(botId);

                    // hack
                    UserClient userClient = (UserClient) wireClient;
                    userClient.setConversationId(conv);
                    // hack

                    WireClient old = clients.put(key, wireClient);
                    if (old != null)
                        old.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.error("GetWireClient. BotId: %s, conv: %s, status: %s",
                            botId,
                            conv,
                            e.getLocalizedMessage());
                }
            }
            return wireClient;
        }
    }
}
