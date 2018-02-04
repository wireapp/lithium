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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.bots.sdk.models.otr.PreKey;

import java.util.ArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewBotResponseModel {
    @JsonProperty
    public String name;

    @JsonProperty("accent_id")
    public Integer accentId;

    @JsonProperty("last_prekey")
    public PreKey lastPreKey;

    @JsonProperty("prekeys")
    public ArrayList<PreKey> preKeys;

    @JsonProperty("assets")
    public ArrayList<Asset> assets;

    public void addAsset(String key, String size) {
        if (assets == null)
            assets = new ArrayList<>();

        Asset asset = new Asset();
        asset.key = key;
        asset.type = "image";
        asset.size = size;
        assets.add(asset);
    }

    public static class Asset {
        @JsonProperty("type")
        public String type;

        @JsonProperty("key")
        public String key;

        @JsonProperty("size")
        public String size;
    }
}
