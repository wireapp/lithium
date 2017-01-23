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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Diagnosis {
    @JsonProperty
    public String expected_base_url;
    @JsonProperty
    public List<Service> services;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Service {
        @JsonProperty
        public String id;
        @JsonProperty
        public String name;
        @JsonProperty
        public String description;
        @JsonProperty
        public String base_url;
        @JsonProperty
        public List<String> auth_tokens;
        @JsonProperty
        public List<PublicKey> public_keys;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PublicKey {
            @JsonProperty
            public int size;
            @JsonProperty
            public String pem;
            @JsonProperty
            public String type;
        }
    }
}
