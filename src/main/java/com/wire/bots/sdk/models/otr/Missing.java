package com.wire.bots.sdk.models.otr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

//<UserId, [ClientId]>
public class Missing extends HashMap<String, Collection<String>> {
    public Collection<String> toClients(String userId) {
        return get(userId);
    }

    public Collection<String> toUserIds() {
        return keySet();
    }

    public void add(String userId, String clientId) {
        Collection<String> clients = toClients(userId);
        if (clients == null) {
            clients = new ArrayList<>();
            put(userId, clients);
        }
        clients.add(clientId);
    }
}
