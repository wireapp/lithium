package com.wire.bots.sdk.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.core.Cookie;
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

    @JsonIgnore
    private Cookie cookie;

    public Cookie getCookie() {
        return cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public int getExpire() {
        return expire;
    }

    public String getType() {
        return type;
    }
}
