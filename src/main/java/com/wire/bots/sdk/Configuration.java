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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Application configuration class. Extend this class to add your custom configuration
 */
public class Configuration extends io.dropwizard.Configuration {
    @JsonProperty("database")
    @NotNull
    public Database database;

    @Valid
    private _JerseyClientConfiguration jerseyClient = new _JerseyClientConfiguration();

    @JsonProperty
    @Deprecated
    public DB db;

    @JsonProperty("swagger")
    public SwaggerBundleConfiguration swagger = new _SwaggerBundleConfiguration();

    @JsonProperty
    @Valid
    public UserMode userMode;

    @JsonProperty
    public String apiHost = "https://prod-nginz-https.wire.com";

    @JsonProperty
    public String wsHost = "wss://prod-nginz-ssl.wire.com/await";

    @JsonProperty("jerseyClient")
    public JerseyClientConfiguration getJerseyClient() {
        return jerseyClient;
    }

    @JsonProperty("jerseyClient")
    public void setJerseyClient(_JerseyClientConfiguration jerseyClient) {
        this.jerseyClient = jerseyClient;
    }

    @Deprecated
    public static class DB {
        public String host;
        public Integer port;
        public String user;
        public String password;
        public Integer timeout = 5000;
        public String url;
        public String driver;
    }

    public static class UserMode {
        @NotNull
        @NotEmpty
        public String email;
        @NotNull
        @NotEmpty
        public String password;
        @JsonProperty
        public boolean sync = true;
    }

    public static class Database extends DataSourceFactory {
        @JsonProperty
        public boolean baseline;
    }

    public static class _JerseyClientConfiguration extends JerseyClientConfiguration {
        public _JerseyClientConfiguration() {
            setChunkedEncodingEnabled(false);
            setGzipEnabled(false);
            setGzipEnabledForRequests(false);
        }
    }

    private static class _SwaggerBundleConfiguration extends SwaggerBundleConfiguration {
        _SwaggerBundleConfiguration() {
            setResourcePackage("com.wire.bots.sdk.server.resources");
        }
    }

    @JsonIgnore
    public boolean isUserMode() {
        return userMode != null && userMode.email != null && userMode.password != null;
    }
}
