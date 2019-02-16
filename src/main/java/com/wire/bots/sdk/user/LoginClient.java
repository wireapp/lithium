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
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.model.NewClient;
import com.wire.bots.sdk.user.model.User;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Base64;

public class LoginClient {
    final WebTarget clientsPath;
    private final WebTarget loginPath;

    public LoginClient(Client client) {
        String host = host();
        loginPath = client
                .target(host)
                .path("login");
        clientsPath = client
                .target(host)
                .path("clients");
    }

    public static String host() {
        return Util.getHost();
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }

    public User login(String email, String password) throws HttpException {
        return login(email, password, false);
    }

    public User login(String email, String password, boolean persisted) throws HttpException {
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
        return ret;
    }

    public String registerClient(PreKey key, String token, String password) throws HttpException {
        NewClient newClient = new NewClient();
        newClient.lastPreKey = key;
        newClient.sigkeys.enckey = Base64.getEncoder().encodeToString(new byte[32]);
        newClient.sigkeys.mackey = Base64.getEncoder().encodeToString(new byte[32]);

        newClient.password = password;
        newClient.deviceType = "tablet";
        newClient.label = "wbotz";
        newClient.type = "permanent";

        Response response = clientsPath
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .post(Entity.entity(newClient, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 400)
            throw new HttpException(response.readEntity(String.class), response.getStatus());

        return response.readEntity(_Client.class).id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Client {
        public String id;
    }
}
