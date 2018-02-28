package com.wire.bots.sdk.models.otr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

//<UserId, [ClientId]>
public class Missing extends ConcurrentHashMap<String, Collection<String>> {
    public Collection<String> toClients(String userId) {
        return get(userId);
    }

    public Collection<String> toUserIds() {
        return keySet();
    }

    public void add(String userId, String clientId) {
        Collection<String> clients = computeIfAbsent(userId, k -> new ArrayList<>());
        clients.add(clientId);
    }

    public void add(String userId, Collection<String> clients) {
        Collection<String> old = computeIfAbsent(userId, k -> new ArrayList<>());
        old.addAll(clients);
    }
}
