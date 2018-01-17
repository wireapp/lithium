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

import com.wire.bots.sdk.*;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.NewBotResponseModel;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/bots")
public class BotsResource {
    private final MessageHandlerBase handler;
    private final Configuration conf;
    private final ClientRepo repo;

    public BotsResource(MessageHandlerBase handler, Configuration conf, ClientRepo repo) {
        this.handler = handler;
        this.conf = conf;
        this.repo = repo;
    }

    @POST
    public Response newBot(@HeaderParam("Authorization") String auth, NewBot newBot) throws Exception {
        if (!Util.compareTokens(conf.getAuth(), auth)) {
            Logger.warning(String.format("Invalid auth. Got: '%s' expected: '%s'",
                    auth,
                    conf.getAuth()
            ));
            return Response.
                    ok("Invalid Authorization: " + auth).
                    status(403).
                    build();
        }

        if (!handler.onNewBot(newBot))
            return Response.
                    status(409).
                    build();

        String path = String.format("%s/%s", conf.getCryptoDir(), newBot.id);

        File dir = new File(path);
        if (!dir.mkdirs())
            Logger.warning("Failed to create dir: %s", dir.getAbsolutePath());

        Util.writeLine(newBot.client, new File(path + "/client.id"));
        Util.writeLine(newBot.token, new File(path + "/token.id"));
        Util.writeLine(newBot.conversation.id, new File(path + "/conversation.id"));
        Util.writeLine(newBot.origin.id, new File(path + "/origin.id"));
        Util.writeLine(newBot.locale, new File(path + "/locale"));

        NewBotResponseModel ret = new NewBotResponseModel();
        ret.name = handler.getName();
        ret.accentId = handler.getAccentColour();
        String profilePreview = handler.getSmallProfilePicture();
        if (profilePreview != null) {
            NewBotResponseModel.Asset asset = new NewBotResponseModel.Asset();
            asset.key = profilePreview;
            asset.type = "image";
            asset.size = "preview";

            if (ret.assets == null)
                ret.assets = new ArrayList<>();
            ret.assets.add(asset);
        }

        String profileBig = handler.getBigProfilePicture();
        if (profileBig != null) {
            NewBotResponseModel.Asset asset = new NewBotResponseModel.Asset();
            asset.key = profileBig;
            asset.type = "image";
            asset.size = "complete";
            if (ret.assets == null)
                ret.assets = new ArrayList<>();
            ret.assets.add(asset);
        }

        WireClient client = repo.getWireClient(newBot.id);
        ret.lastPreKey = client.newLastPreKey();
        ret.preKeys = client.newPreKeys(0, newBot.conversation.members.size() * 8);

        return Response.
                ok(ret).
                status(201).
                build();
    }
}