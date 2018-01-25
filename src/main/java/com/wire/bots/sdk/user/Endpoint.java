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
import com.wire.bots.sdk.Util;
import com.wire.bots.sdk.server.model.InboundMessage;
import com.wire.bots.sdk.server.resources.MessageResource;
import com.wire.bots.sdk.user.model.Message;
import com.wire.bots.sdk.user.model.User;
import com.wire.cryptobox.CryptoException;
import com.wire.cryptobox.PreKey;
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
    private static final String WSS = "wss://%s-nginz-ssl.%s/await?access_token=%s&client=%s";
    private static final String PROD = "prod";
    private static final String WIRE_COM = "wire.com";
    private static final String ZINFRA_IO = "zinfra.io";
    private static final String CLIENT_ID = "client.id";
    private static final String TOKEN_ID = "token.id";
    private static final String ENV = "env";
    private MessageResource messageResource;
    private final Configuration config;
    private ClientManager client = null;
    private String token;
    private String cookie;
    private String clientId;
    private String botId;

    public Endpoint(Configuration config) throws CryptoException {
        this.config = config;
    }

    public Session connectWebSocket(MessageResource messageResource) throws Exception {
        if (client == null) {
            client = ClientManager.createClient();
        }
        this.messageResource = messageResource;

        return client.connectToServer(this, getPath());
    }

    /**
     * Signin as a regular Wire user
     *
     * @param email    Email address
     * @param password Plain text password
     * @param persisted True if you want to renew token every 15 mins
     * @throws Exception
     */
    public String signIn(String email, String password, boolean persisted) throws Exception {
        LoginClient wireClient = new LoginClient();
        User login = wireClient.login(email, password);
        token = login.getToken();
        cookie = login.getCookie();
        botId = login.extractUserId();

        String dataDir = String.format("%s/%s", config.getCryptoDir(), botId);
        clientId = initDevice(dataDir, password, token);

        if (persisted)
            initRenewal(15);

        return botId;
    }

    @OnMessage
    public void onMessage(InputStream rawInput) throws Exception {
        //String s = IOUtils.toString(rawInput);
        ObjectMapper mapper = new ObjectMapper();
        Message message = mapper.readValue(rawInput, Message.class);

        for (InboundMessage payload : message.payload) {
            //Logger.info(payload.type);
            try {
                messageResource.onNewMessage(botId, payload.conversation, payload);
            } catch (Exception e) {
                Logger.warning(e.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) throws Exception {
        Logger.info(String.format("Session closed: %s, %s", session.getId(), reason));
        //   client.connectToServer(this, getPath());
    }

    private URI getPath() throws URISyntaxException {
        String env = System.getProperty(ENV, PROD);
        String domain = env.equals(PROD) ? WIRE_COM : ZINFRA_IO;

        String url = String.format(WSS,
                env,
                domain,
                token,
                clientId);
        return new URI(url);
    }

    private static String initDevice(String dataDir, String password, String token)
            throws CryptoException, IOException {
        File base = new File(dataDir);
        if (base.mkdirs())
            Logger.info("Created: " + dataDir);

        File clientIdFile = new File(String.format("%s/%s", base.getAbsolutePath(), CLIENT_ID));

        File tokenFile = new File(String.format("%s/%s", base.getAbsolutePath(), TOKEN_ID));
        Util.writeLine(token, tokenFile);

        if (clientIdFile.exists()) {
            String clientId = Util.readLine(clientIdFile);
            Logger.info("init Device: ClientID: " + clientId);
            return clientId;
        }

        // register new device
        try (OtrManager otrManager = new OtrManager(base.getAbsolutePath())) {
            PreKey key = otrManager.newLastPreKey();
            LoginClient login = new LoginClient();
            String clientId = login.registerClient(key, token, password);
            Util.writeLine(clientId, clientIdFile);
            Logger.info("init Device: New ClientID: " + clientId);
            return clientId;
        }
    }

    private void initRenewal(int periodMinutes) {
        Timer timer = new Timer("RenewToken");
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    token = API.renewAccessToken(cookie, token);
                    File tokenFile = new File(String.format("%s/%s/%s", config.getCryptoDir(), botId, TOKEN_ID));
                    Util.writeLine(token, tokenFile);
                } catch (Exception e) {
                    Logger.warning("Failed periodic access_token renewal: " + e.getMessage());
                    e.printStackTrace();

                }
            }
        }, TimeUnit.MINUTES.toMillis(periodMinutes), TimeUnit.MINUTES.toMillis(periodMinutes));
    }

    public String getToken() {
        return token;
    }

    public String getClientId() {
        return clientId;
    }

    public String getBotId() {
        return botId;
    }

}
