//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.sdk.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wire.bots.sdk.exceptions.AuthException;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.model.Access;
import com.wire.bots.sdk.user.model.NewClient;
import org.glassfish.jersey.logging.LoggingFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class LoginClient {
    private static final String LABEL = "wbots";
    private static final String COOKIE_NAME = "zuid";
    final WebTarget clientsPath;
    private final WebTarget loginPath;
    private final WebTarget accessPath;
    private final WebTarget cookiesPath;

    public LoginClient(Client client) {
        String host = host();
        loginPath = client
                .target(host)
                .path("login");
        clientsPath = client
                .target(host)
                .path("clients");
        accessPath = client
                .target(host)
                .path("access");

        cookiesPath = client
                .target(host)
                .path("cookies");

        Feature feature = new LoggingFeature(Logger.getLOGGER(), Level.FINE, null, null);
        accessPath.register(feature);
    }

    public static String host() {
        return Util.getHost();
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }

    public Access login(String email, String password) throws HttpException {
        return login(email, password, false);
    }

    public Access login(String email, String password, boolean persisted) throws HttpException {
        _Login login = new _Login();
        login.email = email;
        login.password = password;
        login.label = LABEL;

        Response response = loginPath.
                queryParam("persist", persisted).
                request(MediaType.APPLICATION_JSON).
                post(Entity.entity(login, MediaType.APPLICATION_JSON));

        int status = response.getStatus();

        if (status == 401) {   //todo nginx returns text/html for 401. Cannot deserialize as json
            response.readEntity(String.class);
            throw new AuthException(status);
        }

        if (status == 403) {
            String entity = response.readEntity(String.class);
            throw new AuthException(entity, status);
        }

        if (status >= 400) {
            String entity = response.readEntity(String.class);
            throw new HttpException(entity, status);
        }

        Access access = response.readEntity(Access.class);

        NewCookie zuid = response.getCookies().get(COOKIE_NAME);
        if (zuid != null) {
            access.setCookie(zuid);
        }
        return access;
    }

    @Deprecated
    public String registerClient(String token, String password, ArrayList<PreKey> preKeys, PreKey lastKey) throws HttpException {
        String deviceClass = "tablet";
        String type = "permanent";
        return registerClient(token, password, preKeys, lastKey, deviceClass, type, LABEL);
    }

    /**
     * @param token
     * @param password Wire password
     * @param preKeys
     * @param lastKey
     * @param clazz    "tablet" | "phone" | "desktop"
     * @param type     "permanent" | "temporary"
     * @param label    can be anything
     * @return Client id
     * @throws HttpException
     */
    public String registerClient(String token, String password, ArrayList<PreKey> preKeys, PreKey lastKey,
                                 String clazz, String type, String label) throws HttpException {
        NewClient newClient = new NewClient();
        newClient.password = password;
        newClient.lastPreKey = lastKey;
        newClient.preKeys = preKeys;
        newClient.sigkeys.enckey = Base64.getEncoder().encodeToString(new byte[32]);
        newClient.sigkeys.mackey = Base64.getEncoder().encodeToString(new byte[32]);
        newClient.clazz = clazz;
        newClient.label = label;
        newClient.type = type;

        Response response = clientsPath
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .post(Entity.entity(newClient, MediaType.APPLICATION_JSON));

        int status = response.getStatus();

        if (status == 401) {   //todo nginx returns text/html for 401. Cannot deserialize as json
            response.readEntity(String.class);
            throw new AuthException(status);
        }

        if (status >= 400)
            throw response.readEntity(HttpException.class);

        return response.readEntity(_Client.class).id;
    }

    public Access renewAccessToken(Cookie cookie) throws HttpException {
        Invocation.Builder builder = accessPath
                .request(MediaType.APPLICATION_JSON)
                .cookie(cookie);

        Response response = builder.
                post(Entity.entity(null, MediaType.APPLICATION_JSON));

        int status = response.getStatus();

        if (status == 401) {   //todo nginx returns text/html for 401. Cannot deserialize as json
            response.readEntity(String.class);
            throw new AuthException(status);
        }

        if (status == 403) {
            throw response.readEntity(AuthException.class);
        }

        if (status >= 400) {
            throw response.readEntity(HttpException.class);
        }

        Access access = response.readEntity(Access.class);

        NewCookie zuid = response.getCookies().get(COOKIE_NAME);
        if (zuid != null) {
            access.setCookie(zuid);
        }
        return access;
    }

    public void logout(String token, Cookie cookie) throws HttpException {
        Response response = accessPath
                .path("logout")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .cookie(cookie)
                .post(Entity.entity(null, MediaType.APPLICATION_JSON));

        int status = response.getStatus();
        if (status == 401) {   //todo nginx returns text/html for 401. Cannot deserialize as json
            response.readEntity(String.class);
            throw new AuthException(status);
        }

        if (status == 403) {
            throw response.readEntity(AuthException.class);
        }

        if (status >= 400) {
            throw response.readEntity(HttpException.class);
        }
    }

    public void removeCookies(String token, String password) throws HttpException {
        _RemoveCookies removeCookies = new _RemoveCookies();
        removeCookies.password = password;
        removeCookies.labels = Collections.singletonList(LABEL);

        Response response = cookiesPath.
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                post(Entity.entity(removeCookies, MediaType.APPLICATION_JSON));

        int status = response.getStatus();

        if (status == 401) {   //todo nginx returns text/html for 401. Cannot deserialize as json
            response.readEntity(String.class);
            throw new AuthException(status);
        }

        if (status >= 400)
            throw response.readEntity(HttpException.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _Login {
        public String email;
        public String password;
        public String label;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _Client {
        public String id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _RemoveCookies {
        public String password;
        public List<String> labels;
    }
}
