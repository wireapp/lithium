package com.wire.bots.sdk.tools;

public class AuthValidator {
    private final String auth;

    public AuthValidator(String auth) {
        this.auth = auth;
    }

    public boolean validate(String authToken) {
        return Util.compareTokens(auth, authToken);
    }
}
