package com.wire.bots.sdk.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.xenon.tools.Logger;
import com.wire.xenon.tools.Util;
import com.wire.xenon.user.model.Event;

import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class EventDecoder implements Decoder.BinaryStream<Event> {
    private final static ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public Event decode(InputStream is) {
        try {
            String str = new String(Util.toByteArray(is), StandardCharsets.UTF_8);
            if (str.equalsIgnoreCase("pong")) {
                Logger.debug("MessageDecoder: %s", str);
            } else {
                return mapper.readValue(str, Event.class);
            }
        } catch (IOException e) {
            Logger.error("MessageDecoder: %s", e);
        }
        return null;
    }
}
