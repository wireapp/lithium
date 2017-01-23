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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

public class OtrMessage {
    @JsonProperty
    private final String sender; //clientId of the sender

    @JsonProperty
    private final HashMap<String, HashMap<String, byte[]>> recipients = new HashMap<>(); //<UserId, <ClientId, Cipher>>

    @JsonIgnore
    private byte[] content;    // GenericMessage proto

    public OtrMessage(String sender) {
        this.sender = sender;
    }

    public OtrMessage(String sender, byte[] content) {
        this.sender = sender;
        this.content = content;
    }

    HashMap<String, byte[]> getClients(String userId) {
        HashMap<String, byte[]> clients = recipients.get(userId);
        if (clients == null) {
            clients = new HashMap<>();
            recipients.put(userId, clients);
        }
        return clients;
    }

    public void add(String userId, String clientId, byte[] cipher) {
        HashMap<String, byte[]> clients = getClients(userId);
        clients.put(clientId, cipher);
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

    public int size() {
        int count = 0;
        for (HashMap<String, byte[]> devs : recipients.values()) {
            count += devs.size();
        }
        return count;
    }

    public String getSender() {
        return sender;
    }

    public HashMap<String, HashMap<String, byte[]>> getRecipients() {
        return recipients;
    }
}
