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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.bots.sdk.assets.IAsset;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.otr.*;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.Service;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.model.Connection;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class API extends LoginClient {

    private final String token;
    private String convId;

    public API(String convId, String token) {
        this.convId = convId;
        this.token = token;
    }

    public API(String token) throws IOException {
        this.token = token;
    }

    static String renewAccessToken(String cookie, String token) throws IOException {
        Response response = client.target(httpUrl).
                path("access").
                request(MediaType.APPLICATION_JSON).
                header("Authorization", "Bearer " + token).
                header("Cookie", cookie).
                post(Entity.entity(new Connection(), MediaType.APPLICATION_JSON));


        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class));
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }

        return response.readEntity(com.wire.bots.sdk.user.model.User.class).getToken();
    }

    Devices sendMessage(OtrMessage msg) throws IOException {
        return sendMessage(msg, false);
    }

    Devices sendMessage(OtrMessage msg, boolean ignoreMissing) throws IOException {
        Response response = client.target(httpUrl).
                path("conversations").
                path(convId).
                path("otr/messages").
                queryParam("ignore_missing", ignoreMissing).
                request(MediaType.APPLICATION_JSON).
                header("Authorization", "Bearer " + token).
                post(Entity.entity(msg, MediaType.APPLICATION_JSON));

        int statusCode = response.getStatus();
        if (statusCode == 412) {
            //Logger.info(response.readEntity(String.class));
            return response.readEntity(Devices.class);
        }

        if (statusCode >= 300)
            throw new IOException("sendMessage: " + response.readEntity(String.class) + ". code: " + statusCode);

        return new Devices();
    }

    PreKeys getPreKeys(Missing missing) throws IOException {
        if (missing.isEmpty())
            return new PreKeys();

        return client.target(httpUrl).
                path("users/prekeys").
                request(MediaType.APPLICATION_JSON).
                header("Authorization", "Bearer " + token).
                accept(MediaType.APPLICATION_JSON).
                post(Entity.entity(missing, MediaType.APPLICATION_JSON), PreKeys.class);
    }

    byte[] downloadAsset(String assetKey, String assetToken) throws IOException {
        Invocation.Builder req = client.target(httpUrl)
                .path("assets/v3")
                .path(assetKey)
                .request()
                .header("Authorization", "Bearer " + token);

        if (assetToken != null)
            req.header("Asset-Token", assetToken);

        Response response = req.get();

        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class) + ". AssetId: " + assetKey);
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }

        return response.readEntity(byte[].class);
    }

    void acceptConnection(String user) throws IOException {
        Connection connection = new Connection();
        connection.setStatus("accepted");

        Response response = client.target(httpUrl).
                path("connections").
                path(user).
                request(MediaType.APPLICATION_JSON).
                header("Authorization", "Bearer " + token).
                put(Entity.entity(connection, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class));
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }
    }

    AssetKey uploadAsset(IAsset asset) throws Exception {
        StringBuilder sb = new StringBuilder();

        // Part 1
        String strMetadata = String.format("{\"public\": %s, \"retention\": \"%s\"}",
                asset.isPublic(),
                asset.getRetention());
        sb.append("--frontier\r\n");
        sb.append("Content-Type: application/json; charset=utf-8\r\n");
        sb.append("Content-Length: ")
                .append(strMetadata.length())
                .append("\r\n\r\n");
        sb.append(strMetadata)
                .append("\r\n");

        // Part 2
        sb.append("--frontier\r\n");
        sb.append("Content-Type: ")
                .append(asset.getMimeType())
                .append("\r\n");
        sb.append("Content-Length: ")
                .append(asset.getEncryptedData().length)
                .append("\r\n");
        sb.append("Content-MD5: ")
                .append(Util.calcMd5(asset.getEncryptedData()))
                .append("\r\n\r\n");

        // Complete
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(sb.toString().getBytes("utf-8"));
        os.write(asset.getEncryptedData());
        os.write("\r\n--frontier--\r\n".getBytes("utf-8"));

        Response response = client.target(httpUrl)
                .path("assets/v3")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer " + token)
                .post(Entity.entity(os.toByteArray(), "multipart/mixed; boundary=frontier"));

        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class));
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }

        return response.readEntity(AssetKey.class);
    }

    Conversation getConversation() throws IOException {
        Response response = client.target(httpUrl).
                path("conversations").
                path(convId).
                request().
                header("Authorization", "Bearer " + token).
                accept(MediaType.APPLICATION_JSON).
                get();

        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class));
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }

        _Cov conv = response.readEntity(_Cov.class);

        Conversation ret = new Conversation();
        ret.name = conv.name;
        ret.id = conv.id;
        ret.members = conv.members.others;
        return ret;
    }

    public Conversation createConversation(String name) throws IOException {
        Response response = client.target(httpUrl).
                path("conversations").
                request().
                header("Authorization", "Bearer " + token).
                accept(MediaType.APPLICATION_JSON).
                post(Entity.entity("{\"users\":[], \"name\":\"" + name + "\"}", MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class));
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }

        _Cov conv = response.readEntity(_Cov.class);

        convId = conv.id;
        Conversation ret = new Conversation();
        ret.name = conv.name;
        ret.id = conv.id;
        ret.members = conv.members.others;
        return ret;
    }

    public void deleteConversation(String teamId) throws IOException {
        Response response = client.target(httpUrl).
                path("teams").
                path(teamId).
                path("conversations").
                path(convId).
                request().
                header("Authorization", "Bearer " + token).
                accept(MediaType.APPLICATION_JSON).
                delete();

        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class));
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }
    }

    public User addService(String serviceId, String providerId) throws IOException {
        _Service service = new _Service();
        service.service = serviceId;
        service.provider = providerId;

        Response response = client.target(httpUrl).
                path("conversations").
                path(convId).
                path("bots").
                request().
                header("Authorization", "Bearer " + token).
                accept(MediaType.APPLICATION_JSON).
                post(Entity.entity(service, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class));
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }

        User user = response.readEntity(User.class);
        user.service = new Service();
        user.service.id = serviceId;
        user.service.provider = providerId;
        return user;
    }

    Collection<com.wire.bots.sdk.server.model.User> getUsers(Collection<String> ids) throws IOException {
        return client.target(httpUrl).
                path("users").
                queryParam("ids", String.join(",", ids)).
                request(MediaType.APPLICATION_JSON).
                header("Authorization", "Bearer " + token).
                get(new GenericType<ArrayList<com.wire.bots.sdk.server.model.User>>() {
                });
    }

    void uploadPreKeys(ArrayList<PreKey> preKeys) {
        client.target(httpUrl).
                path("users/prekeys").
                request(MediaType.APPLICATION_JSON).
                header("Authorization", "Bearer " + token).
                accept(MediaType.APPLICATION_JSON).
                post(Entity.entity(preKeys, MediaType.APPLICATION_JSON));
    }

    ArrayList<Integer> getAvailablePrekeys(String clientId) {
        return client.target(httpUrl).
                path("clients").
                path(clientId).
                path("prekeys").
                request().
                header("Authorization", "Bearer " + token).
                accept(MediaType.APPLICATION_JSON).
                get(new GenericType<ArrayList<Integer>>() {
                });
    }

    public void setConvId(String convId) {
        this.convId = convId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Cov {
        @JsonProperty
        public String id;

        @JsonProperty
        public String name;

        @JsonProperty
        public _Members members;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Members {
        @JsonProperty
        public List<Member> others;
    }

    class _Service {
        public String service;
        public String provider;
    }
}
