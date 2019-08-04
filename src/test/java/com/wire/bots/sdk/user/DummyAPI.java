package com.wire.bots.sdk.user;

import com.wire.bots.sdk.models.otr.*;
import org.glassfish.jersey.client.JerseyClientBuilder;

import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

class DummyAPI extends API {
    private final Devices devices = new Devices();
    private final HashMap<String, PreKey> lastPreKeys = new HashMap<>(); // <userId-clientId, PreKey>
    private OtrMessage msg;

    DummyAPI() {
        super(JerseyClientBuilder.createClient(), null, null);
    }

    @Override
    public Devices sendMessage(OtrMessage msg, Object... ignoreMissing) {
        this.msg = msg;
        Devices missing = new Devices();

        for (UUID userId : devices.missing.toUserIds()) {
            for (String client : devices.missing.toClients(userId)) {
                if (msg.get(userId, client) == null)
                    missing.missing.add(userId, client);
            }
        }
        return missing;
    }

    @Override
    public PreKeys getPreKeys(Missing missing) {
        PreKeys ret = new PreKeys();
        for (UUID userId : missing.toUserIds()) {
            HashMap<String, PreKey> devs = new HashMap<>();
            for (String client : missing.toClients(userId)) {
                String key = key(userId, client);
                PreKey preKey = lastPreKeys.get(key);
                devs.put(client, preKey);
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

    void addDevice(UUID userId, String client, com.wire.bots.cryptobox.PreKey lastKey) {
        devices.missing.add(userId, client);
        addLastKey(userId, client, lastKey);
    }

    private void addLastKey(UUID userId, String clientId, com.wire.bots.cryptobox.PreKey lastKey) {
        String key = key(userId, clientId);
        PreKey preKey = convert(lastKey);
        lastPreKeys.put(key, preKey);
    }

    private String key(UUID userId, String clientId) {
        return String.format("%s-%s", userId, clientId);
    }

    OtrMessage getMsg() {
        return msg;
    }
}
