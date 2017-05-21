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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.OtrManager;
import com.wire.bots.sdk.server.model.InboundMessage;
import com.wire.bots.sdk.server.resources.MessageResource;
import com.wire.bots.sdk.user.model.User;
import com.wire.cryptobox.CryptoException;
import com.wire.cryptobox.PreKey;
import com.wire.bots.sdk.Util;
import com.wire.bots.sdk.user.model.Message;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Web Socket when running the sdk as a regular user and not as a bot Service
 */
@ClientEndpoint
public class Endpoint {
    private final MessageResource messageResource;
    private final Configuration config;
    private Session session;
    private final ClientManager client;
    private String token;
    private String cookie;
    private String clientId;
    private String botId;

    public Endpoint(Configuration config, MessageResource handler) throws CryptoException {
        this.config = config;
        this.messageResource = handler;
        client = ClientManager.createClient();
    }

    public void connectWebSocket() throws Exception {
        session = client.connectToServer(this, getPath());
    }

    /**
     * Signin as a regular Wire user
     *
     * @param email    Email address
     * @param password Plain text password
     * @throws Exception
     */
    public void signIn(String email, String password) throws Exception {
        LoginClient wireClient = new LoginClient();
        User login = wireClient.login(email, password);
        token = login.getToken();
        cookie = login.getCookie();
        botId = login.extractUserId();

        clientId = initDevice(password, token);

        initRenewal();
    }

    @OnMessage
    public void onMessage(InputStream rawInput) throws Exception {
        //String s = IOUtils.toString(rawInput);
        ObjectMapper mapper = new ObjectMapper();
        Message message = mapper.readValue(rawInput, Message.class);

        for (InboundMessage payload : message.payload) {
            //Logger.info(payload.type);
            messageResource.onNewMessage(botId, payload.conversation, payload);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) throws Exception {
        //Logger.info("onClose websocket. Reason: " + reason.getReasonPhrase());
        this.session = client.connectToServer(this, getPath());
    }

    private URI getPath() throws URISyntaxException {
        String env = System.getProperty("env", "prod");
        String domain = env.equals("prod") ? "wire.com" : "zinfra.io"; //fixme: remove zinfra

        String url = String.format("wss://%s-nginz-ssl.%s/await?access_token=%s&client=%s",
                env,
                domain,
                token,
                clientId);
        return new URI(url);
    }

    private String initDevice(String password, String token) throws CryptoException, IOException {
        File base = new File(config.getCryptoDir() + "/" + botId);
        base.mkdirs();

        File clientIdFile = new File(base.getAbsolutePath() + "/client.id");

        File tokenFile = new File(base.getAbsolutePath() + "/token.id");
        Util.writeLine(token, tokenFile);

        if (clientIdFile.exists()) {
            String clientId = Util.readLine(clientIdFile);
            Logger.info("Init Device: ClientID: " + clientId);
            return clientId;
        }

        // register new device
        try (OtrManager otrManager = new OtrManager(base.getAbsolutePath())) {
            PreKey key = otrManager.newLastPreKey();
            LoginClient login = new LoginClient();
            String clientId = login.registerClient(key, this.token, password);
            Util.writeLine(clientId, clientIdFile);
            Logger.info("Init Device: New ClientID: " + clientId);
            return clientId;
        }
    }

    private void initRenewal() {
        Timer timer = new Timer("RenewToken");
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    token = UserJerseyClient.renewAccessToken(cookie, token);
                    File tokenFile = new File(config.getCryptoDir() + "/" + botId + "/token.id");
                    Util.writeLine(token, tokenFile);
                } catch (Exception e) {
                    Logger.warning("Failed periodic access_token renewal: " + e.getMessage());
                    e.printStackTrace();

                }
            }
        }, TimeUnit.MINUTES.toMillis(14), TimeUnit.MINUTES.toMillis(14));
    }
}
