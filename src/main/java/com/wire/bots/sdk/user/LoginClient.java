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
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.user.model.User;
import com.wire.cryptobox.PreKey;
import com.wire.bots.sdk.user.model.NewClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Base64;

public class LoginClient {
    protected final static Client client;
    protected final static String httpUrl;

    static {
        String env = System.getProperty("env", "prod");
        String domain = env.equals("prod") ? "wire.com" : "zinfra.io"; //fixme: remove zinfra
        httpUrl = String.format("https://%s-nginz-https.%s", env, domain);

        ClientConfig cfg = new ClientConfig(JacksonJsonProvider.class);
        client = JerseyClientBuilder.createClient(cfg);
    }

    public User login(String email, String password) throws IOException {
        User login = new User();
        login.setEmail(email);
        login.setPassword(password);

        Response response = client.target(httpUrl).
                path("login").
                queryParam("persist", true).
                request(MediaType.APPLICATION_JSON).
                post(Entity.entity(login, MediaType.APPLICATION_JSON));


        if (response.getStatus() >= 300)
            throw new IOException("Login: " + response.readEntity(String.class) + ". code: " + response.getStatus());

        User user = response.readEntity(User.class);
        String cookie = response.getStringHeaders().getFirst("Set-Cookie");
        user.setCookie(cookie);
        return user;
    }

    public String registerClient(PreKey key, String token, String password) throws IOException {
        NewClient newClient = new NewClient();
        newClient.lastPreKey = new com.wire.bots.sdk.models.otr.PreKey();
        newClient.lastPreKey.id = key.id;
        newClient.lastPreKey.key = Base64.getEncoder().encodeToString(key.data);

        newClient.sigkeys.enckey = Base64.getEncoder().encodeToString(new byte[32]);
        newClient.sigkeys.mackey = Base64.getEncoder().encodeToString(new byte[32]);

        newClient.password = password;
        newClient.deviceType = "desktop";
        newClient.label = "wbotz";
        newClient.type = "permanent";

        Response response = client.target(httpUrl).
                path("clients").
                request(MediaType.APPLICATION_JSON).
                header("Authorization", "Bearer " + token).
                post(Entity.entity(newClient, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 300)
            throw new IOException("registerClient: " + response.readEntity(String.class) + ". code: " + response.getStatus());

        return response.readEntity(_Client.class).id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Client {
        public String id;
    }

    public boolean setHandle(String token, String handle) {
        Response response = client.target(httpUrl)
                .path("self/handle")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .put(Entity.entity(String.format("{ \"handle\":\"%s\"}", handle), MediaType.APPLICATION_JSON));

        if (response.getStatus() > 300) {
            String msg = String.format("setHandle: %s, code: %s", response.readEntity(String.class)
                    , response.getStatus());
            Logger.warning(msg);
        }
        return response.getStatus() == 200;
    }
}
