package com.wire.bots.sdk.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Access {
    @JsonProperty("user")
    public UUID userId;

    @JsonProperty("access_token")
    public String token;

    @JsonProperty("expires_in")
    public int expire;

    @JsonProperty("token_type")
    public String type;

    public String cookie;
    public String clientId;
}
