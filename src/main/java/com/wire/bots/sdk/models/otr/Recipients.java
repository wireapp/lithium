package com.wire.bots.sdk.models.otr;

import java.util.HashMap;
import java.util.Set;

//<UserId, ClientCipher> //Base64 encoded cipher
public class Recipients extends HashMap<String, ClientCipher> {

    public String get(String userId, String clientId) {
        HashMap<String, String> clients = toClients(userId);
        return clients.get(clientId);
    }

    public void add(String userId, String clientId, String cipher) {
        ClientCipher clients = toClients(userId);
        clients.put(clientId, cipher);
    }

    //<UserId, <ClientId, Cipher>>
    public void add(String userId, ClientCipher clients) {
        Set<String> clientIds = clients.keySet();
        for (String clientId : clientIds) {
            String bytes = clients.get(clientId);
            add(userId, clientId, bytes);
        }
    }

    public void add(Recipients recipients) {
        Set<String> userIds = recipients.keySet();
        for (String userId : userIds) {
            ClientCipher hashMap = recipients.get(userId);
            add(userId, hashMap);
        }
    }

    private ClientCipher toClients(String userId) {
        return computeIfAbsent(userId, k -> new ClientCipher());
    }

}
