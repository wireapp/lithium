package com.wire.bots.sdk.server.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMultimap;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.UUID;

public class AvailablePrekeysTask extends TaskBase {
    private final ClientRepo repo;

    public AvailablePrekeysTask(ClientRepo repo) {
        super("prekeys");
        this.repo = repo;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) {
        UUID botId = UUID.fromString(extractString(parameters, "bot"));

        try {
            WireClient client = repo.getWireClient(botId);
            ArrayList<Integer> availablePrekeys = client.getAvailablePrekeys();
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            output.println(mapper.writeValueAsString(availablePrekeys));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            output.println(e.getMessage());
        }
    }
}
