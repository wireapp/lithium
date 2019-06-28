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

package com.wire.bots.sdk.models;

import java.util.UUID;

/**
 */
abstract class MessageBase {
    protected final UUID userId;
    protected final String clientId;
    protected final UUID conversationId;
    protected final UUID messageId;
    protected String time;

    MessageBase(UUID msgId, UUID convId, String clientId, UUID userId) {
        this.messageId = msgId;
        this.conversationId = convId;
        this.clientId = clientId;
        this.userId = userId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getClientId() {
        return clientId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
