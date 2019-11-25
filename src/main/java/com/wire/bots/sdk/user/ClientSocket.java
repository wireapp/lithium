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

package com.wire.bots.sdk.user;

import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.model.Message;

import javax.websocket.*;

/**
 * Web Socket when running the sdk as a regular user and not as a bot Service
 */
@ClientEndpoint(decoders = MessageDecoder.class)
public class ClientSocket {
    private UserMessageResource userMessageResource;

    ClientSocket(UserMessageResource userMessageResource) {
        this.userMessageResource = userMessageResource;
    }

    @OnMessage
    public void onMessage(Message message) {
        for (Payload payload : message.payload) {
            try {
                switch (payload.type) {
                    case "team.member-join":
                    case "user.update":
                        userMessageResource.onUpdate(message.id, payload);
                        break;
                    case "user.connection":
                        userMessageResource.onNewMessage(
                                message.id,
                                /* payload.connection.from, */ //todo check this!!
                                payload.connection.convId,
                                payload);
                        break;
                    case "conversation.otr-message-add":
                    case "conversation.member-join":
                    case "conversation.create":
                        userMessageResource.onNewMessage(
                                message.id,
                                payload.convId,
                                payload);
                        break;
                    default:
                        Logger.info("Unknown type: %s, from: %s", payload.type, payload.from);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.error("Endpoint:onMessage: %s %s", payload.type, e);
            }
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        Logger.info("Session opened: %s", session.getId());
    }

    @OnClose
    public void onClose(Session closed, CloseReason reason) {
        Logger.warning("Session closed: %s, %s", closed.getId(), reason);
    }
}
