package com.wire.bots.sdk.user;

import com.wire.bots.sdk.models.otr.*;
import org.glassfish.jersey.client.JerseyClientBuilder;

import java.util.Base64;
import java.util.HashMap;

class DummyAPI extends API {
    private final Devices devices = new Devices();
    private final HashMap<String, PreKey> lastPreKeys = new HashMap<>();
    private OtrMessage msg;

    DummyAPI() {
        super(JerseyClientBuilder.createClient(), null, null);
    }

    @Override
    Devices sendMessage(OtrMessage msg, boolean ignoreMissing) {
        this.msg = msg;
        Devices missing = new Devices();

        for (String userId : devices.missing.toUserIds()) {
            for (String client : devices.missing.toClients(userId)) {
                if (msg.get(userId, client) == null)
                    missing.missing.add(userId, client);
            }
        }
        return missing;
    }

    @Override
    PreKeys getPreKeys(Missing missing) {
        PreKeys ret = new PreKeys();
        for (String userId : missing.toUserIds()) {
            HashMap<String, PreKey> devs = new HashMap<>();
            for (String client : missing.toClients(userId)) {
                devs.put(client, lastPreKeys.get(userId));
            }
            ret.put(userId, devs);
        }

        return ret;
    }

    private PreKey convert(com.wire.bots.cryptobox.PreKey lastKey) {
        PreKey preKey = new PreKey();
        preKey.id = lastKey.id;
        preKey.key = Base64.getEncoder().encodeToString(lastKey.data);
        return preKey;
    }

    void addDevice(String userId, String client) {
        devices.missing.add(userId, client);
    }

    void addLastKey(String userId, com.wire.bots.cryptobox.PreKey lastKey) {
        lastPreKeys.put(userId, convert(lastKey));
    }

    OtrMessage getMsg() {
        return msg;
    }
}
