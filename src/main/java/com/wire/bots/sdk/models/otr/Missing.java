package com.wire.bots.sdk.models.otr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//<UserId, [ClientId]>
public class Missing extends ConcurrentHashMap<UUID, Collection<String>> {
    public Collection<String> toClients(UUID userId) {
        return get(userId);
    }

    public Collection<UUID> toUserIds() {
        return keySet();
    }

    public void add(UUID userId, String clientId) {
        Collection<String> clients = computeIfAbsent(userId, k -> new ArrayList<>());
        clients.add(clientId);
    }

    public void add(UUID userId, Collection<String> clients) {
        Collection<String> old = computeIfAbsent(userId, k -> new ArrayList<>());
        old.addAll(clients);
    }
}
