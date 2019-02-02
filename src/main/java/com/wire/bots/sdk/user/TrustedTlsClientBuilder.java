package com.wire.bots.sdk.user;

import com.wire.bots.sdk.Server;
import com.wire.bots.sdk.tools.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class TrustedTlsClientBuilder {
    public static Client build() {
        TrustManager[] certs = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
        }};

        ClientConfig cfg = Server.getClientConfig();

        try {
            SSLContext ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, certs, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

            return ClientBuilder
                    .newBuilder()
                    .sslContext(ctx)
                    .hostnameVerifier((hostname, session) -> true)
                    .withConfig(cfg)
                    .build();
        } catch (Exception e) {
            Logger.error(e.toString());
            return JerseyClientBuilder.createClient(cfg);
        }
    }
}
