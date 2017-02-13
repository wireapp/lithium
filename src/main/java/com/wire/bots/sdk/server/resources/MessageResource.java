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

package com.wire.bots.sdk.server.resources;

import com.waz.model.Messages;
import com.wire.bots.sdk.*;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.GenericMessageProcessor;
import com.wire.bots.sdk.server.model.InboundMessage;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/bots/{bot}/messages")
public class MessageResource {
    private final MessageHandlerBase handler;
    private final Configuration conf;
    private final ClientRepo repo;

    public MessageResource(MessageHandlerBase handler, Configuration conf, ClientRepo repo) {
        this.handler = handler;
        this.conf = conf;
        this.repo = repo;
    }

    @POST
    public Response newMessage(@HeaderParam("Authorization") String auth,
                               @PathParam("bot") String botId,
                               InboundMessage inbound) throws Exception {

        if (!conf.getAuth().equals(auth))
            return Response.
                    ok().
                    status(403).
                    build();

        WireClient client = repo.getWireClient(botId, inbound.conversation);

        InboundMessage.Data data = inbound.data;
        switch (inbound.type) {
            case "conversation.otr-message-add": {
                GenericMessageProcessor processor = new GenericMessageProcessor(client, handler);

                byte[] bytes = client.decrypt(inbound.from, data.sender, data.text);
                Messages.GenericMessage genericMessage = Messages.GenericMessage.parseFrom(bytes);

                sendDeliveryReceipt(client, genericMessage.getMessageId());

                handler.onEvent(client, inbound.from, genericMessage);

                processor.process(inbound.from, genericMessage);
            }
            break;
            case "conversation.member-join": {
                Logger.info("conversation.member-join: " + String.join(",", data.userIds));

                // Check if this bot got added to the conversation
                if (data.userIds.remove(botId)) {
                    handler.onNewConversation(client);
                }

                int minAvailable = 8 * data.userIds.size();
                if (minAvailable > 0) {
                    ArrayList<Integer> availablePrekeys = client.getAvailablePrekeys();
                    availablePrekeys.remove(new Integer(65535));  //remove last prekey
                    if (availablePrekeys.size() < minAvailable) {
                        Integer lastKeyOffset = Collections.max(availablePrekeys);
                        ArrayList<PreKey> keys = client.newPreKeys(lastKeyOffset + 1, minAvailable);
                        client.uploadPreKeys(keys);
                        Logger.info("Uploaded " + keys.size() + " prekeys");
                    }
                    handler.onMemberJoin(client, data.userIds);
                }
            }
            break;
            case "conversation.member-leave": {
                Logger.info("conversation.member-leave: " + String.join(",", data.userIds));

                // Check if this bot got removed from the conversation
                if (data.userIds.remove(botId)) {
                    repo.removeClient(botId);
                    handler.onBotRemoved(botId);
                }

                if (!data.userIds.isEmpty()) {
                    handler.onMemberLeave(client, data.userIds);
                }
            }
            break;
            // Legacy code starts here
            case "conversation.otr-asset-add": {
                GenericMessageProcessor processor = new GenericMessageProcessor(client, handler);

                byte[] bytes = client.decrypt(inbound.from, data.sender, data.key);
                Messages.GenericMessage genericMessage = Messages.GenericMessage.parseFrom(bytes);

                handler.onEvent(client, inbound.from, genericMessage);

                processor.process(inbound.from, inbound.data.id, genericMessage);
            }
            break;
            case "user.connection": {
                if (inbound.connection.status.equals("pending")) {
                    client.acceptConnection(inbound.connection.to);
                }
            }
            break;
            case "conversation.create": {
                handler.onNewConversation(client);
            }
            break;
            // Legacy code ends here
        }

        return Response.
                ok().
                status(200).
                build();
    }

    private void sendDeliveryReceipt(WireClient client, String messageId) {
        try {
            client.sendDelivery(messageId);
        } catch (Exception e) {
            Logger.warning("sendDeliveryReceipt: " + e.getMessage());
        }
    }
}