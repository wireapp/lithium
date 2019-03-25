package com.wire.bots.sdk.server.resources;

import com.google.protobuf.InvalidProtocolBufferException;
import com.waz.model.Messages;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.GenericMessageProcessor;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.tools.Logger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

public abstract class MessageResourceBase {

    final ClientRepo repo;
    private final MessageHandlerBase handler;

    public MessageResourceBase(MessageHandlerBase handler, ClientRepo repo) {
        this.handler = handler;
        this.repo = repo;
    }

    protected void handleMessage(Payload payload, WireClient client) throws Exception {
        Payload.Data data = payload.data;
        String botId = client.getId();

        switch (payload.type) {
            case "conversation.otr-message-add": {
                Logger.debug("conversation.otr-message-add: bot: %s from: %s:%s", botId, payload.from, data.sender);

                GenericMessageProcessor processor = new GenericMessageProcessor(client, handler);

                Messages.GenericMessage message = decrypt(client, payload);

                handler.onEvent(client, payload.from.toString(), message);

                boolean process = processor.process(payload.from.toString(), data.sender, message);
                if (process)
                    processor.cleanUp(message.getMessageId());
            }
            break;
            case "conversation.member-join": {
                Logger.debug("conversation.member-join: bot: %s", botId);

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

                // Send dummy message just initialize the session for the new member
                client.sendReaction(UUID.randomUUID().toString(), "");   //todo hack
            }
            break;
            case "conversation.member-leave": {
                Logger.debug("conversation.member-leave: bot: %s", botId);

                // Check if this bot got removed from the conversation
                if (data.userIds.remove(botId)) {
                    handler.onBotRemoved(botId);
                    repo.purgeBot(botId);
                }

                if (!data.userIds.isEmpty()) {
                    handler.onMemberLeave(client, data.userIds);
                }
            }
            break;
            case "conversation.delete": {
                Logger.debug("conversation.delete: bot: %s", botId);

                // Cleanup
                handler.onBotRemoved(botId);
                repo.purgeBot(botId);
            }
            break;
            case "conversation.create": {
                Logger.debug("conversation.create: bot: %s", botId);

                client.sendReaction(UUID.randomUUID().toString(), ""); //todo hack
                handler.onNewConversation(client);
            }
            break;
            case "conversation.rename": {
                Logger.debug("conversation.rename: bot: %s", botId);
                handler.onConversationRename(client);
            }
            break;
            // UserMode code starts here
            case "user.connection": {
                Payload.Connection connection = payload.connection;
                Logger.debug("user.connection: bot: %s, from: %s to: %s status: %s",
                        botId,
                        connection.from,
                        connection.to,
                        connection.status);

                boolean accepted = handler.onConnectRequest(client, connection.from, connection.to, connection.status);
                if (accepted) {
                    // Send dummy message just initialize the session for the new member
                    client.sendReaction(UUID.randomUUID().toString(), ""); //todo hack
                    handler.onNewConversation(client);
                }
            }
            break;
            // UserMode code ends here
            default:
                Logger.debug("Unknown event: %s", payload.type);
                break;
        }
    }

    protected void handleUpdate(Payload payload) {
        switch (payload.type) {
            case "team.member-join": {
                Logger.debug("%s: team: %s, user: %s", payload.type, payload.team, payload.data.user);
                handler.onNewTeamMember(payload.team, payload.data.user);
            }
            break;
            case "user.update": {
                Logger.debug("%s: id: %s", payload.type, payload.user.id);
                handler.onUserUpdate(payload.user.id);
            }
            break;
            default:
                Logger.debug("Unknown event: %s", payload.type);
                break;
        }
    }

    private Messages.GenericMessage decrypt(WireClient client, Payload inbound)
            throws CryptoException, InvalidProtocolBufferException {
        String userId = inbound.from.toString();
        String clientId = inbound.data.sender;
        String cypher = inbound.data.text;

        String encoded = client.decrypt(userId, clientId, cypher);
        byte[] decoded = Base64.getDecoder().decode(encoded);
        return Messages.GenericMessage.parseFrom(decoded);
    }
}
