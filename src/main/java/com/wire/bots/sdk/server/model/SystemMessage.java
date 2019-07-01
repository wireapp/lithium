package com.wire.bots.sdk.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemMessage {
    public UUID id;
    public String type;
    public String time;
    public UUID from;
    public Conversation conversation;
    public List<UUID> users;
}
