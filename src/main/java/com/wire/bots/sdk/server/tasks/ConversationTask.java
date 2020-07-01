package com.wire.bots.sdk.server.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.tools.Logger;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConversationTask extends TaskBase {
    private final ClientRepo repo;

    public ConversationTask(ClientRepo repo) {
        super("conversation");
        this.repo = repo;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
        UUID botId = UUID.fromString(extractString(parameters, "bot"));

        try {
            WireClient client = repo.getClient(botId);
            Conversation conversation = client.getConversation();
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            output.println(mapper.writeValueAsString(conversation));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            output.println(e.getMessage());
        }
    }
}
