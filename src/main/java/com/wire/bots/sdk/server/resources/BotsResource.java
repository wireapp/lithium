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

import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.OtrManager;
import com.wire.bots.sdk.Util;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.NewBotResponseModel;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Base64;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/bots")
public class BotsResource {
    private MessageHandlerBase handler;
    private Configuration conf;

    public BotsResource(MessageHandlerBase handler, Configuration conf) {
        this.handler = handler;
        this.conf = conf;
    }

    @POST
    public Response newBot(@HeaderParam("Authorization") String auth, NewBot newBot) throws Exception {
        //ObjectMapper mapper = new ObjectMapper();
        //Logger.info(mapper.writeValueAsString(newBot));

        if (!conf.getAuth().equalsIgnoreCase(auth))
            return Response.
                    ok().
                    status(403).
                    build();

        if (!handler.onNewBot(newBot))
            return Response.status(409).build();

        String path = String.format("%s/%s", conf.getCryptoDir(), newBot.id);

        File dir = new File(path);
        dir.mkdirs();

        Util.writeLine(newBot.client, new File(path + "/client.id"));
        Util.writeLine(newBot.token, new File(path + "/token.id"));

        try (OtrManager otrManager = new OtrManager(path)) {
            NewBotResponseModel ret = new NewBotResponseModel();
            ret.name = handler.getName();
            ret.accentId = handler.getAccentColour();
            String profilePreview = handler.getSmallProfilePicture();
            if (profilePreview != null) {
                NewBotResponseModel.Asset asset = new NewBotResponseModel.Asset();
                asset.key = profilePreview;
                asset.type = "image";
                asset.size = "preview";
                ret.assets.add(asset);
            }

            String profileBig = handler.getBigProfilePicture();
            if (profileBig != null) {
                NewBotResponseModel.Asset asset = new NewBotResponseModel.Asset();
                asset.key = profileBig;
                asset.type = "image";
                asset.size = "complete";
                ret.assets.add(asset);
            }

            com.wire.cryptobox.PreKey pk = otrManager.newLastPreKey();
            ret.lastPreKey = new PreKey();
            ret.lastPreKey.id = pk.id;
            ret.lastPreKey.key = Base64.getEncoder().encodeToString(pk.data);
            for (int i = 0; i < newBot.conversation.members.size(); i++) {
                com.wire.cryptobox.PreKey[] preKeys = otrManager.newPreKeys(i * 8, 8);
                for (com.wire.cryptobox.PreKey k : preKeys) {
                    PreKey prekey = new PreKey();
                    prekey.id = k.id;
                    prekey.key = Base64.getEncoder().encodeToString(k.data);
                    ret.preKeys.add(prekey);
                }
            }

            Response build = Response.
                    ok(ret).
                    status(201).
                    build();

            //Logger.info(mapper.writeValueAsString(ret));

            return build;
        }
    }
}