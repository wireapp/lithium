//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.sdk.models.otr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Devices {
    @JsonProperty
    public final HashMap<String, ArrayList<String>> missing = new HashMap<>();  //<UserId, [ClientId]>

    @JsonProperty
    public final HashMap<String, ArrayList<String>> redundant = new HashMap<>();  //<UserId, [ClientId]>

    @JsonProperty
    public final HashMap<String, ArrayList<String>> deleted = new HashMap<>();  //<UserId, [ClientId]>

    public Collection<String> getClients(String userId) {
        return missing.get(userId);
    }

    public Collection<String> getUserIds() {
        return missing.keySet();
    }

    public boolean hasMissing() {
        return missing.isEmpty();
    }

    public int size() {
        int ret = 0;
        for (Collection<String> cls : missing.values())
            ret += cls.size();
        return ret;
    }

    public void add(String userId, String clientId) {
        ArrayList<String> clients = missing.get(userId);
        if (clients == null) {
            clients = new ArrayList<>();
            missing.put(userId, clients);
        }
        clients.add(clientId);
    }
}
