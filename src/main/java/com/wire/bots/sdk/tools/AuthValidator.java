package com.wire.bots.sdk.tools;

import com.wire.xenon.tools.Util;

public class AuthValidator {
    private final String auth;

    public AuthValidator(String auth) {
        this.auth = auth;
    }

    public boolean validate(String auth) {
        return Util.compareAuthorizations(this.auth, auth);
    }

    public String getAuth() {
        return auth;
    }
}
