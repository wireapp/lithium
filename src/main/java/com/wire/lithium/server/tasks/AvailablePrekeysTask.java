package com.wire.lithium.server.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wire.lithium.ClientRepo;
import com.wire.xenon.WireClient;
import com.wire.xenon.tools.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AvailablePrekeysTask extends TaskBase {
    private final ClientRepo repo;

    public AvailablePrekeysTask(ClientRepo repo) {
        super("prekeys");
        this.repo = repo;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        UUID botId = UUID.fromString(extractString(parameters, "bot"));

        try {
            WireClient client = repo.getClient(botId);
            ArrayList<Integer> availablePrekeys = client.getAvailablePrekeys();
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            output.println(mapper.writeValueAsString(availablePrekeys));
        } catch (Exception e) {
            Logger.exception("Exception during AvailablePrekeysTask", e);
            output.println(e.getMessage());
        }
    }
}
