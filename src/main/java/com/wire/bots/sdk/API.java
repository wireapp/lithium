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

package com.wire.bots.sdk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.xenon.Const;
import com.wire.xenon.WireAPI;
import com.wire.xenon.assets.IAsset;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.NewBotResponseModel;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.exceptions.HttpException;
import com.wire.xenon.models.AssetKey;
import com.wire.xenon.models.otr.*;
import com.wire.xenon.tools.Logger;
import com.wire.xenon.tools.Util;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class API implements WireAPI {

    private final WebTarget messages;
    private final WebTarget assets;
    private final WebTarget client;
    private final WebTarget prekeys;
    private final WebTarget users;
    private final WebTarget conversation;
    private final WebTarget bot;

    private final Client httpClient;
    private final String token;

    public API(Client httpClient, String token) {
        this.httpClient = httpClient;
        this.token = token;

        bot = httpClient
                .target(host())
                .path("bot");
        messages = bot
                .path("messages");
        assets = bot
                .path("assets");
        users = bot
                .path("users");
        conversation = bot
                .path("conversation");
        client = bot
                .path("client")
                .path("prekeys");
        prekeys = users
                .path("prekeys");

        if (Logger.getLevel() == Level.FINE) {
            Feature feature = new LoggingFeature(Logger.getLOGGER(), Level.FINE, null, null);
            assets.register(feature);
            users.register(feature);
        }
    }

    public Response options() {
        return bot
                .request()
                .options();
    }

    private String host() {
        String host = System.getProperty(Const.WIRE_BOTS_SDK_API, System.getenv("WIRE_API_HOST"));
        return host != null ? host : "https://prod-nginz-https.wire.com";
    }

    /**
     * This method sends the OtrMessage to BE. Message must contain cipher for all participants and all their clients.
     *
     * @param msg           OtrMessage object containing ciphers for all clients
     * @param ignoreMissing If TRUE ignore missing clients and deliver the message to available clients
     * @return List of missing devices in case of fail or an empty list.
     * @throws HttpException Http Exception is thrown when status >= 400
     */
    @Override
    public Devices sendMessage(OtrMessage msg, Object... ignoreMissing) throws HttpException {
        Response response = messages
                .queryParam("ignore_missing", ignoreMissing)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .post(Entity.entity(msg, MediaType.APPLICATION_JSON));

        int statusCode = response.getStatus();
        if (statusCode == 412) {
            // This message was not sent due to missing clients. Parse those missing clients so the caller can add them
            return response.readEntity(Devices.class);
        }

        if (statusCode >= 400) {
            throw new HttpException(response.readEntity(String.class), statusCode);
        }

        return response.readEntity(Devices.class);
    }

    @Override
    public Devices sendPartialMessage(OtrMessage msg, UUID userId) throws HttpException {
        Response response = messages
                .queryParam("report_missing", userId)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .post(Entity.entity(msg, MediaType.APPLICATION_JSON));

        int statusCode = response.getStatus();
        if (statusCode == 412) {
            // This message was not sent due to missing clients. Parse those missing clients so the caller can add them
            return response.readEntity(Devices.class);
        }

        if (statusCode >= 400) {
            throw new HttpException(response.readEntity(String.class), statusCode);
        }

        return response.readEntity(Devices.class);
    }

    @Override
    public Collection<User> getUsers(Collection<UUID> ids) {
        return users
                .queryParam("ids", ids.toArray())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .get(new GenericType<ArrayList<User>>() {
                });
    }

    @Override
    public User getSelf() {
        return bot
                .path("self")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .get(User.class);
    }

    @Override
    public Conversation getConversation() {
        return conversation
                .request()
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .accept(MediaType.APPLICATION_JSON)
                .get(Conversation.class);
    }

    @Override
    public PreKeys getPreKeys(Missing missing) {
        return prekeys
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(missing, MediaType.APPLICATION_JSON), PreKeys.class);
    }

    @Override
    public ArrayList<Integer> getAvailablePrekeys(@Nullable String clientId) {
        return client
                .request()
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .accept(MediaType.APPLICATION_JSON)
                .get(new GenericType<ArrayList<Integer>>() {
                });
    }

    @Override
    public void uploadPreKeys(ArrayList<PreKey> preKeys) throws IOException {
        NewBotResponseModel model = new NewBotResponseModel();
        model.preKeys = preKeys;

        Response res = client
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(model, MediaType.APPLICATION_JSON));

        int statusCode = res.getStatus();
        if (statusCode >= 400) {
            throw new IOException(res.readEntity(String.class));
        }
    }

    @Override
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

        Response response = assets
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .post(Entity.entity(os.toByteArray(), "multipart/mixed; boundary=frontier"));

        if (response.getStatus() >= 400) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        return response.readEntity(AssetKey.class);
    }

    private MultiPart getMultiPart(IAsset asset) throws NoSuchAlgorithmException {
        MetaData metaData = new MetaData();
        metaData.retention = asset.getRetention();
        metaData.scope = asset.isPublic();

        BodyPart bodyPart1 = new BodyPart(metaData, MediaType.APPLICATION_JSON_TYPE);
        BodyPart bodyPart2 = new BodyPart().entity(asset.getEncryptedData());

        MultivaluedMap<String, String> headers = bodyPart2.getHeaders();
        headers.add("Content-Type", asset.getMimeType());
        headers.add("Content-MD5", Util.calcMd5(asset.getEncryptedData()));

        return new MultiPart()
                .bodyPart(bodyPart1)
                .bodyPart(bodyPart2);
    }

    @Override
    public byte[] downloadAsset(String assetKey, String assetToken) throws HttpException {
        Invocation.Builder req = assets
                .path(assetKey)
                .request()
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .header(HttpHeaders.AUTHORIZATION, bearer());

        if (assetToken != null)
            req.header("Asset-Token", assetToken);

        Response response = req.get();

        if (response.getStatus() >= 400) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        response.close();

        response = httpClient
                .target(location)
                .request()
                .get();

        if (response.getStatus() >= 400) {
            throw new HttpException(response.readEntity(String.class), response.getStatus());
        }

        return response.readEntity(byte[].class);
    }

    @Override
    public boolean deleteConversation(UUID teamId) {
        return false;
    }

    @Override
    public User addService(UUID serviceId, UUID providerId) throws HttpException {
        return null;
    }

    @Override
    public User addParticipants(UUID... userIds) throws HttpException {
        return null;
    }

    @Override
    public Conversation createConversation(String name, UUID teamId, List<UUID> users) throws HttpException {
        return null;
    }

    @Override
    public Conversation createOne2One(UUID teamId, UUID userId) throws HttpException {
        return null;
    }

    @Override
    public void leaveConversation(UUID user) throws HttpException {

    }

    @Override
    public User getUser(UUID userId) throws HttpException {
        return null;
    }

    @Override
    public UUID getUserId(String handle) throws HttpException {
        return null;
    }

    @Override
    public boolean hasDevice(UUID userId, String clientId) {
        return false;
    }

    @Override
    public UUID getTeam() throws HttpException {
        return null;
    }

    @Override
    public Collection<UUID> getTeamMembers(UUID teamId) {
        return null;
    }

    @Override
    public void acceptConnection(UUID user) throws Exception {

    }

    private String bearer() {
        return String.format("Bearer %s", token);
    }

    public static class MetaData {
        @JsonProperty("public")
        public boolean scope;
        @JsonProperty
        public String retention;
    }
}
