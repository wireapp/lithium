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
import com.wire.bots.sdk.user.model.User;
import org.glassfish.jersey.logging.LoggingFeature;

import javax.naming.AuthenticationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Level;

public class LoginClient {
    final WebTarget clientsPath;
    private final WebTarget loginPath;
    private final WebTarget accessPath;

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

        Feature feature = new LoggingFeature(Logger.getLOGGER(), Level.FINE, null, null);
        accessPath.register(feature);
    }

    public static String host() {
        return Util.getHost();
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }

    public User login(String email, String password) throws HttpException, AuthenticationException {
        return login(email, password, false);
    }

    public User login(String email, String password, boolean persisted) throws HttpException, AuthenticationException {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);

        Response response = loginPath.
                queryParam("persist", persisted).
                request(MediaType.APPLICATION_JSON).
                post(Entity.entity(user, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 400)
            throw new HttpException(response.readEntity(String.class), response.getStatus());

        User ret = response.readEntity(User.class);
        String cookie = response.getStringHeaders().getFirst(HttpHeaders.SET_COOKIE);
        ret.setCookie(cookie);
        ret.setUserId(User.extractUserId(ret.getToken()));
        return ret;
    }

    @Deprecated
    public String registerClient(String token, String password, ArrayList<PreKey> preKeys, PreKey lastKey) throws HttpException {
        String deviceClass = "tablet";
        String type = "permanent";
        String label = "wbotz";
        return registerClient(token, password, preKeys, lastKey, deviceClass, type, label);
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

        if (response.getStatus() >= 400)
            throw new HttpException(response.readEntity(String.class), response.getStatus());

        return response.readEntity(_Client.class).id;
    }

    public Access renewAccessToken(Cookie cookie) throws HttpException {
        Invocation.Builder builder = accessPath
                .request(MediaType.APPLICATION_JSON)
                .cookie(cookie);

        Response response = builder.
                post(Entity.entity(null, MediaType.APPLICATION_JSON));

        int status = response.getStatus();
        if (status == 403) {
            String entity = response.readEntity(String.class);
            throw new AuthException(entity, status);
        }

        if (status >= 400) {
            String entity = response.readEntity(String.class);
            throw new HttpException(entity, status);
        }

        Access access = response.readEntity(Access.class);

        NewCookie zuid = response.getCookies().get("zuid");
        if (zuid != null) {
            access.cookie = zuid.getValue();
        }
        return access;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Client {
        public String id;
    }
}
