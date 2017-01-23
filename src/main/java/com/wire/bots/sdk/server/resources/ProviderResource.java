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

package com.wire.bots.sdk.server.resources;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.server.model.Diagnosis;
import com.wire.bots.sdk.server.model.Match;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import sun.misc.BASE64Encoder;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/provider")
public class ProviderResource {

    // TODO: Make this value configurable through yaml
    private static final String backend = "https://prod-nginz-https.wire.com";

    private Configuration conf;

    public ProviderResource(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Proxies the backend resource /provider/login to get the login cookie on a cross domain
     */
    @POST
    @Path("/login")
    public Response login(@Context HttpHeaders headers, String payload) throws IOException {
        Logger.getGlobal().info("Payload: " + payload);

        URL url = new URL(backend + "/provider/login");
        // TODO: Prevent possible MITM attack by checking certificates
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", headers.getHeaderString("Content-Type"));
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(payload);
        wr.flush();
        conn.connect();
        String cookies = conn.getHeaderField("Set-Cookie");
        Logger.getGlobal().info("Cookies: " + cookies);
        if (cookies != null) {
            cookies = cookies.replace(" Domain=wire.com; HttpOnly;", "");
            Logger.getGlobal().info("Cookies: " + cookies);
        }

        return Response.
                status(conn.getResponseCode()).
                header("Set-Cookie", cookies).
                entity(conn.getResponseMessage()).
                build();
    }

    /**
     * Proxies the backend resource /provider/services to get all services of a certain provider. It automatically uses the
     * cookie from the login to authenticate against the backend. No credentials are saved.
     */
    @GET
    @Path("/services")
    public Response getServices(@Context HttpHeaders headers) throws IOException {
        URL url = new URL(backend + "/provider/services");
        // TODO: Prevent possible MITM attack by checking certificates
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", headers.getHeaderString("Accept"));
        Logger.getGlobal().info("Request Cookie: " + headers.getHeaderString("Cookie"));
        conn.setRequestProperty("Cookie", headers.getHeaderString("Cookie"));
        conn.connect();

        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String line = null;
        while ((line = br.readLine()) != null) {
            result.append(line + "\n");
        }
        br.close();

        return Response.
                status(conn.getResponseCode()).
                entity(result.toString()).
                build();
    }

    /**
     * Returns a list of matching and non-matching services for this server. It accepts a data structure composed of the
     * expected base url and the response of the /provider/services resource as JSON. This way we do not expose any confidential
     * information to the outside. You can only use it with the response you got securely from the backend.
     */
    @POST
    @Path("/diagnosis")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response diagnosis(Diagnosis diagnosis) throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, UnrecoverableKeyException {
        List<Diagnosis.Service> services = diagnosis.services;
        String expectedBaseUrl = diagnosis.expected_base_url;
        try {
            String expectedPublicKey = getPublicKey();
            String expectedAuthToken = getAuthToken();
            List<Match> matches = new ArrayList<>();

            for (Diagnosis.Service service : services) {
                Match match = new Match();
                match.id = service.id;
                match.name = service.name;
                match.matching_base_url = service.base_url.equals(expectedBaseUrl);

                match.matching_auth_token = false;
                for (String token : service.auth_tokens) {
                    if (token.equals(expectedAuthToken)) {
                        match.matching_auth_token = true;
                        break;
                    }
                }

                match.matching_public_key = false;
                for (Diagnosis.Service.PublicKey key : service.public_keys) {
                    if (normalizePublicKey(key.pem).equals(expectedPublicKey)) {
                        match.matching_public_key = true;
                        break;
                    }
                }

                matches.add(match);
            }

            return Response.ok(matches).build();
        } catch (Exception e) {
            return Response.serverError().entity("Could not load server config: " + e.getMessage()).build();
        }
    }

    /**
     * Returns the public key out of the keystore.jks
     */
    private String getPublicKey() throws Exception {
        String keyStorePath = null;
        String keyStorePassword = null;
        String certAlias = null;
        DefaultServerFactory serverFactory = (DefaultServerFactory) conf.getServerFactory();
        for (ConnectorFactory connector : serverFactory.getApplicationConnectors()) {
            if (connector.getClass().isAssignableFrom(HttpsConnectorFactory.class)) {
                for (Method method : connector.getClass().getMethods()) {
                    if (method.getName().equals("getKeyStorePath")) {
                        keyStorePath = ((HttpsConnectorFactory) connector).getKeyStorePath();
                        keyStorePassword = ((HttpsConnectorFactory) connector).getKeyStorePassword();
                        certAlias = ((HttpsConnectorFactory) connector).getCertAlias();
                        break;
                    }
                }
            }
        }

        if (keyStorePath != null && keyStorePassword != null && certAlias != null) {
            KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            // Get certificate of public key
            Certificate cert = keystore.getCertificate(certAlias);
            // Get public key
            PublicKey publicKey = cert.getPublicKey();
            String b64 = new BASE64Encoder().encode(publicKey.getEncoded());
            String pem = "-----BEGIN PUBLIC KEY-----" + b64 + "-----END PUBLIC KEY-----";
            return normalizePublicKey(pem);
        } else {
            throw new Exception("Could not get public key from keystore");
        }
    }

    private String normalizePublicKey(String key) {
        return key.replace("\n", "");
    }

    /**
     * Returns the auth token from the configuation yaml (Do not expose this to a public resource!)
     */
    private String getAuthToken() {
        return conf.getAuth().replace("Bearer ", "");
    }
}
