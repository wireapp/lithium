package com.wire.bots.sdk.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.exceptions.MissingStateException;
import com.wire.bots.sdk.server.model.NewBot;
import org.skife.jdbi.v2.DBI;

import java.io.IOException;
import java.util.UUID;

public class JdbiState implements State {
    private final static ObjectMapper mapper = new ObjectMapper();

    private final UUID botId;
    private final StatesDAO statesDAO;

    public JdbiState(UUID botId, DBI jdbi) {
        this.botId = botId;
        this.statesDAO = jdbi.onDemand(StatesDAO.class);
    }

    @Override
    public boolean saveState(NewBot newBot) throws IOException {
        String str = mapper.writeValueAsString(newBot);
        return 1 == statesDAO.insert(botId, str);
    }

    @Override
    public NewBot getState() throws IOException {
        String str = statesDAO.get(botId);
        if (str == null)
            throw new MissingStateException(botId);
        return mapper.readValue(str, NewBot.class);
    }

    @Override
    public boolean removeState() {
        return 1 == statesDAO.delete(botId);
    }
}
