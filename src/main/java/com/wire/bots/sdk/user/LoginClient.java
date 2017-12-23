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
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.user.model.NewClient;
import com.wire.bots.sdk.user.model.User;
import com.wire.cryptobox.PreKey;
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
    final static String httpUrl;

    static {
        String env = System.getProperty("env", "prod");
        String domain = env.equals("prod") ? "wire.com" : "zinfra.io"; //fixme: remove zinfra
        httpUrl = String.format("https://%s-nginz-https.%s", env, domain);

        ClientConfig cfg = new ClientConfig(JacksonJsonProvider.class);
        client = JerseyClientBuilder.createClient(cfg);
    }

    User login(String email, String password) throws IOException {
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

    String registerClient(PreKey key, String token, String password) throws IOException {
        NewClient newClient = new NewClient();
        newClient.lastPreKey = new com.wire.bots.sdk.models.otr.PreKey();
        newClient.lastPreKey.id = key.id;
        newClient.lastPreKey.key = Base64.getEncoder().encodeToString(key.data);

        newClient.sigkeys.enckey = Base64.getEncoder().encodeToString(new byte[32]);
        newClient.sigkeys.mackey = Base64.getEncoder().encodeToString(new byte[32]);

        newClient.password = password;
        newClient.deviceType = "tablet";
        newClient.label = "wbotz";
        newClient.type = "permanent";

        Response response = client.target(httpUrl).
                path("clients").
                request(MediaType.APPLICATION_JSON).
                header("Authorization", "Bearer " + token).
                post(Entity.entity(newClient, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 300)
            throw new IOException(String.format("registerClient: %s. code: %d",
                    response.readEntity(String.class),
                    response.getStatus()));

        return response.readEntity(_Client.class).id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Client {
        public String id;
    }

    public String newConversation(String token, String name) throws IOException {
        Response response = client.target(httpUrl)
                .path("conversations")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .post(Entity.entity(String.format("{ \"name\":\"%s\", \"users\":[] }", name), MediaType.APPLICATION_JSON));

        if (response.getStatus() > 300) {
            String msg = String.format("newConversation: %s, code: %s", response.readEntity(String.class)
                    , response.getStatus());
            Logger.warning(msg);
            throw new IOException(msg);
        }
        return response.readEntity(Member.class).id;   //todo fix me, ffs!
    }

    public boolean addService(String token, String convId, String provider, String service) throws IOException {
        String json = String.format("{ \"provider\":\"%s\", \"service\":\"%s\" }", provider, service);

        Response response = client.target(httpUrl)
                .path("conversations")
                .path(convId)
                .path("bots")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .post(Entity.entity(json, MediaType.APPLICATION_JSON));

        if (response.getStatus() > 300) {
            String msg = String.format("addService: %s, code: %s", response.readEntity(String.class)
                    , response.getStatus());
            Logger.warning(msg);
            throw new IOException(msg);
        }
        return response.getStatus() == 201;
    }
}
