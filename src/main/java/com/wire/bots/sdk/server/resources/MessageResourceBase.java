package com.wire.bots.sdk.server.resources;

import com.google.protobuf.InvalidProtocolBufferException;
import com.waz.model.Messages;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.server.GenericMessageProcessor;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.tools.AuthValidator;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

public abstract class MessageResourceBase {
    private final AuthValidator validator;
    private final ClientRepo repo;
    private final MessageHandlerBase handler;

    public MessageResourceBase(MessageHandlerBase handler, AuthValidator validator, ClientRepo repo) {
        this.handler = handler;
        this.validator = validator;
        this.repo = repo;
    }

    protected void handleMessage(Payload payload, WireClient client) throws Exception {
        Payload.Data data = payload.data;
        String botId = client.getId();

        switch (payload.type) {
            case "conversation.otr-message-add": {
                String from = payload.from.toString();

                Logger.debug("conversation.otr-message-add: bot: %s from: %s:%s", botId, from, data.sender);

                GenericMessageProcessor processor = new GenericMessageProcessor(client, handler);

                Messages.GenericMessage message = decrypt(client, payload);

                handler.onEvent(client, from, message);

                boolean process = processor.process(from,
                        data.sender,
                        payload.convId,
                        payload.time,
                        message);

                if (process)
                    processor.cleanUp(message.getMessageId());
            }
            break;
            case "conversation.member-join": {
                Logger.debug("conversation.member-join: bot: %s", botId);

                // Check if this bot got added to the conversation
                ArrayList<String> participants = data.userIds;
                if (participants.remove(botId)) {
                    handler.onNewConversation(client);
                }

                // Check if we still have some prekeys available. Upload new prekeys if needed
                handler.validatePreKeys(client, participants.size());

                handler.onMemberJoin(client, participants);
            }
            break;
            case "conversation.member-leave": {
                Logger.debug("conversation.member-leave: bot: %s", botId);

                // Check if this bot got removed from the conversation
                ArrayList<String> participants = data.userIds;
                if (participants.remove(botId)) {
                    handler.onBotRemoved(botId);
                    repo.purgeBot(botId);
                }

                if (!participants.isEmpty()) {
                    handler.onMemberLeave(client, participants);
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

    protected WireClient getWireClient(String botId, Payload payload) throws IOException, CryptoException {
        return repo.getClient(botId);
    }

    protected boolean isValid(String auth) {
        return validator.validate(auth);
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

    private Messages.GenericMessage decrypt(WireClient client, Payload payload)
            throws CryptoException, InvalidProtocolBufferException {
        String from = payload.from.toString();
        String sender = payload.data.sender;
        String cipher = payload.data.text;

        String encoded = client.decrypt(from, sender, cipher);
        byte[] decoded = Base64.getDecoder().decode(encoded);
        return Messages.GenericMessage.parseFrom(decoded);
    }
}
