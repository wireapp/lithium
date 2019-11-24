package com.wire.bots.sdk.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.user.model.Message;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.io.IOException;

public class MessageDecoder implements Decoder.Text<Message> {
    private final static ObjectMapper mapper = new ObjectMapper();

    @Override
    public Message decode(String s) throws DecodeException {
        try {
            return mapper.readValue(s, Message.class);
        } catch (IOException e) {
            throw new DecodeException(s, "MessageDecoder", e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return s.startsWith("{") && s.endsWith("}");
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
