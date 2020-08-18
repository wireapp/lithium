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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.Backend;
import com.wire.bots.sdk.assets.IAsset;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.otr.*;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.Service;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.model.Connection;
import org.glassfish.jersey.logging.LoggingFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class API extends LoginClient implements Backend {
    private final WebTarget conversationsPath;
    private final WebTarget usersPath;
    private final WebTarget assetsPath;
    private final WebTarget teamsPath;
    private final WebTarget connectionsPath;
    private final WebTarget selfPath;

    private final String token;
    private final String convId;

    public API(Client client, UUID convId, String token) {
        super(client);
        this.convId = convId != null ? convId.toString() : null;
        this.token = token;

        String host = host();
        WebTarget target = client
                .target(host);

        conversationsPath = target.path("conversations");
        usersPath = target.path("users");
        assetsPath = target.path("assets/v3");
        teamsPath = target.path("teams");
        connectionsPath = target.path("connections");
        selfPath = target.path("self");

        if (Logger.getLevel() == Level.FINE) {
            Feature feature = new LoggingFeature(Logger.getLOGGER(), Level.FINE, null, null);
            assetsPath.register(feature);
            usersPath.register(feature);
        }
    }

    @Override
    public Devices sendMessage(OtrMessage msg, Object... ignoreMissing) throws HttpException {
        Response response = conversationsPath.
                path(convId).
                path("otr/messages").
                queryParam("ignore_missing", ignoreMissing).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                post(Entity.entity(msg, MediaType.APPLICATION_JSON));

        int statusCode = response.getStatus();
        if (statusCode == 412) {
            return response.readEntity(Devices.class);
        }

        if (statusCode >= 400)
            throw new HttpException(response.getStatusInfo().getReasonPhrase(), response.getStatus());

        response.close();
        return new Devices();
    }

    @Override
    public Devices sendPartialMessage(OtrMessage msg, UUID userId) throws HttpException {
        Response response = conversationsPath.
                path(convId).
                path("otr/messages").
                queryParam("report_missing", userId).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                post(Entity.entity(msg, MediaType.APPLICATION_JSON));

        int statusCode = response.getStatus();
        if (statusCode == 412) {
            return response.readEntity(Devices.class);
        }

        if (statusCode >= 400)
            throw new HttpException(response.getStatusInfo().getReasonPhrase(), response.getStatus());

        response.close();
        return new Devices();
    }

    @Override
    public PreKeys getPreKeys(Missing missing) {
        if (missing.isEmpty())
            return new PreKeys();

        return usersPath.path("prekeys").
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                accept(MediaType.APPLICATION_JSON).
                post(Entity.entity(missing, MediaType.APPLICATION_JSON), PreKeys.class);
    }

    public byte[] downloadAsset(String assetKey, String assetToken) throws HttpException {
        Invocation.Builder req = assetsPath
                .path(assetKey)
                .queryParam("access_token", token)
                .request();

        if (assetToken != null)
            req.header("Asset-Token", assetToken);

        Response response = req.get();

        if (response.getStatus() >= 400) {
            String log = String.format("%s. AssetId: %s", response.readEntity(String.class), assetKey);
            throw new HttpException(log, response.getStatus());
        }

        return response.readEntity(byte[].class);
    }

    void acceptConnection(UUID user) throws HttpException {
        Connection connection = new Connection();
        connection.setStatus("accepted");

        Response response = connectionsPath.
                path(user.toString()).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                put(Entity.entity(connection, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 400) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }
        response.close();
    }

    public AssetKey uploadAsset(IAsset asset) throws Exception {
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
        os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        os.write(asset.getEncryptedData());
        os.write("\r\n--frontier--\r\n".getBytes(StandardCharsets.UTF_8));

        Response response = assetsPath
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .post(Entity.entity(os.toByteArray(), "multipart/mixed; boundary=frontier"));

        String entity = response.readEntity(String.class);

        if (response.getStatus() >= 300) {
            throw new HttpException(entity, response.getStatus());
        }

        Logger.debug("uploadAsset: res: %s", entity);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(entity, AssetKey.class);
    }

    Conversation getConversation() throws IOException {
        Response response = conversationsPath.
                path(convId).
                request().
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                get();

        if (response.getStatus() >= 300) {
            Logger.warning(response.readEntity(String.class));
            throw new IOException(response.getStatusInfo().getReasonPhrase());
        }

        _Conv conv = response.readEntity(_Conv.class);

        Conversation ret = new Conversation();
        ret.name = conv.name;
        ret.id = conv.id;
        ret.members = conv.members.others;
        return ret;
    }

    public boolean deleteConversation(UUID teamId) throws HttpException {
        Response response = teamsPath.
                path(teamId.toString()).
                path("conversations").
                path(convId).
                request().
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                delete();

        if (response.getStatus() >= 400) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        return response.getStatus() == 200;
    }

    public User addService(UUID serviceId, UUID providerId) throws HttpException {
        _Service service = new _Service();
        service.service = serviceId;
        service.provider = providerId;

        Response response = conversationsPath.
                path(convId).
                path("bots").
                request().
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                post(Entity.entity(service, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 400) {
            String msg = response.readEntity(String.class);
            throw new HttpException(msg, response.getStatus());
        }

        User user = response.readEntity(User.class);
        user.service = new Service();
        user.service.id = serviceId;
        user.service.providerId = providerId;
        return user;
    }

    public User addParticipants(UUID... userIds) throws HttpException {
        _NewConv newConv = new _NewConv();
        newConv.users = Arrays.asList(userIds);

        Response response = conversationsPath.
                path(convId).
                path("members").
                request().
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                post(Entity.entity(newConv, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 400) {
            String msg = response.readEntity(String.class);
            throw new HttpException(msg, response.getStatus());
        }

        return response.readEntity(User.class);
    }

    public Conversation createConversation(String name, UUID teamId, List<UUID> users) throws HttpException {
        _NewConv newConv = new _NewConv();
        newConv.name = name;
        newConv.users = users;
        if (teamId != null) {
            newConv.team = new _TeamInfo();
            newConv.team.teamId = teamId;
        }

        Response response = conversationsPath.
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                post(Entity.entity(newConv, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 400) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        _Conv conv = response.readEntity(_Conv.class);

        Conversation ret = new Conversation();
        ret.name = conv.name;
        ret.id = conv.id;
        ret.members = conv.members.others;
        return ret;
    }

    public Conversation createOne2One(UUID teamId, UUID userId) throws HttpException {

        _NewConv newConv = new _NewConv();
        newConv.users = Collections.singletonList(userId);

        if (teamId != null) {
            newConv.team = new _TeamInfo();
            newConv.team.teamId = teamId;
        }

        Response response = conversationsPath
                .path("one2one")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .post(Entity.entity(newConv, MediaType.APPLICATION_JSON));

        if (response.getStatus() >= 400) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        _Conv conv = response.readEntity(_Conv.class);

        Conversation ret = new Conversation();
        ret.name = conv.name;
        ret.id = conv.id;
        ret.members = conv.members.others;
        return ret;
    }

    public void leaveConversation(UUID user) throws HttpException {
        Response response = conversationsPath
                .path(convId)
                .path("members")
                .path(user.toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .delete();

        if (response.getStatus() >= 400) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }
    }

    void uploadPreKeys(ArrayList<PreKey> preKeys) {
        usersPath.path("prekeys").
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                accept(MediaType.APPLICATION_JSON).
                post(Entity.entity(preKeys, MediaType.APPLICATION_JSON));
    }

    ArrayList<Integer> getAvailablePrekeys(String clientId) {
        return clientsPath.
                path(clientId).
                path("prekeys").
                request().
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                accept(MediaType.APPLICATION_JSON).
                get(new GenericType<ArrayList<Integer>>() {
                });
    }

    public Collection<User> getUsers(Collection<UUID> ids) throws HttpException {
        Response response = usersPath.
                queryParam("ids", ids.toArray()).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                get();

        if (response.getStatus() != 200) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        return response.readEntity(new GenericType<Collection<User>>() {
        });
    }

    public User getUser(UUID userId) throws HttpException {
        Response response = usersPath.
                path(userId.toString()).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                get();

        if (response.getStatus() != 200) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        return response.readEntity(User.class);
    }

    public UUID getUserId(String handle) throws HttpException {
        Response response = usersPath.
                path("handles").
                path(handle).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                get();

        if (response.getStatus() != 200) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        final _TeamMember teamMember = response.readEntity(_TeamMember.class);
        return teamMember.user;
    }

    public boolean hasDevice(UUID userId, String clientId) {
        Response response = usersPath.
                path(userId.toString()).
                path("clients").
                path(clientId).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                get();

        response.close();
        return response.getStatus() == 200;
    }

    public User getSelf() throws HttpException {
        Response response = selfPath.
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                get();

        if (response.getStatus() != 200) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        return response.readEntity(User.class);
    }

    public UUID getTeam() throws HttpException {
        Response response = teamsPath
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (response.getStatus() != 200) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        _Teams teams = response.readEntity(_Teams.class);
        if (teams.teams.isEmpty())
            return null;

        return teams.teams.get(0).id;
    }

    public Collection<UUID> getTeamMembers(UUID teamId) {
        _Team team = teamsPath
                .path(teamId.toString())
                .path("members")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .accept(MediaType.APPLICATION_JSON)
                .get(_Team.class);

        return team.members.stream().map(x -> x.user).collect(Collectors.toCollection(ArrayList::new));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Conv {
        @JsonProperty
        public UUID id;

        @JsonProperty
        public String name;

        @JsonProperty
        public _Members members;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _Members {
        @JsonProperty
        public List<Member> others;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _Service {
        public UUID service;
        public UUID provider;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _Team {
        @JsonProperty
        public UUID id;
        @JsonProperty
        public String name;
        @JsonProperty
        public List<_TeamMember> members;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _TeamMember {
        @JsonProperty
        public UUID user;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _Teams {
        @JsonProperty
        public ArrayList<_Team> teams;
    }

    static class _NewConv {
        @JsonProperty
        public String name;

        @JsonProperty
        public _TeamInfo team;

        @JsonProperty
        public List<UUID> users;

        @JsonProperty
        public _Service service;
    }

    static class _TeamInfo {
        @JsonProperty("teamid")
        public UUID teamId;

        @JsonProperty
        public boolean managed;
    }

    static class _Device {
        @JsonProperty("id")
        public String clientId;

        @JsonProperty("class")
        public String type;
    }
}
