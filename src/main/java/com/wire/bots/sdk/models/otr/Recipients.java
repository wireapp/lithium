package com.wire.bots.sdk.models.otr;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

//<UserId, ClientCipher> //Base64 encoded cipher
public class Recipients extends HashMap<UUID, ClientCipher> {

    public String get(UUID userId, String clientId) {
        HashMap<String, String> clients = toClients(userId);
        return clients.get(clientId);
    }

    public void add(UUID userId, String clientId, String cipher) {
        ClientCipher clients = toClients(userId);
        clients.put(clientId, cipher);
    }

    //<UserId, <ClientId, Cipher>>
    public void add(UUID userId, ClientCipher clients) {
        Set<String> clientIds = clients.keySet();
        for (String clientId : clientIds) {
            String bytes = clients.get(clientId);
            add(userId, clientId, bytes);
        }
    }

    public void add(Recipients recipients) {
        Set<UUID> userIds = recipients.keySet();
        for (UUID userId : userIds) {
            ClientCipher hashMap = recipients.get(userId);
            add(userId, hashMap);
        }
    }

    private ClientCipher toClients(UUID userId) {
        return computeIfAbsent(userId, k -> new ClientCipher());
    }

}
