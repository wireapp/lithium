package com.wire.bots.sdk.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationList {
    @JsonProperty("has_more")
    @NotNull
    public Boolean hasMore;

    @JsonProperty
    @NotNull
    public List<Event> notifications = new ArrayList<>();
}
