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

package com.wire.bots.sdk.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Payload {
    @JsonProperty
    @NotNull
    public String type;
    @JsonProperty("conversation")
    public UUID convId;
    @JsonProperty
    @NotNull
    public String from;
    @JsonProperty
    @NotNull
    public String time;
    @JsonProperty
    @NotNull
    public Data data;
    @JsonProperty
    public UUID team;

    // legacy
    @JsonProperty
    public Connection connection;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty
        @NotNull
        public String sender;
        @JsonProperty
        @NotNull
        public String recipient;
        @JsonProperty
        public String text;
        @JsonProperty("user_ids")
        public ArrayList<String> userIds;
        @JsonProperty
        public String name;

        //legacy
        @JsonProperty
        public String id;
        @JsonProperty
        public String key;
        @JsonProperty
        public UUID user;
    }

    // legacy
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Connection {
        @JsonProperty
        public String status;

        @JsonProperty
        public String to;
    }
}
