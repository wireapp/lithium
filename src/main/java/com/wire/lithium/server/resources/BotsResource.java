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

package com.wire.lithium.server.resources;

import com.codahale.metrics.annotation.Metered;
import com.wire.lithium.models.NewBotResponseModel;
import com.wire.lithium.server.monitoring.MDCUtils;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.backend.models.ErrorMessage;
import com.wire.xenon.backend.models.NewBot;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;
import com.wire.xenon.factories.StorageFactory;
import com.wire.xenon.tools.Logger;
import io.swagger.annotations.*;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Api
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/bots")
public class BotsResource {
    protected final MessageHandlerBase handler;

    protected final StorageFactory storageF;
    protected final CryptoFactory cryptoF;

    public BotsResource(MessageHandlerBase handler, StorageFactory storageF, CryptoFactory cryptoF) {
        this.handler = handler;
        this.storageF = storageF;
        this.cryptoF = cryptoF;
    }

    @POST
    @ApiOperation(value = "New Bot instance", response = NewBotResponseModel.class, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized", response = ErrorMessage.class),
            @ApiResponse(code = 409, message = "Bot not accepted (whitelist?)", response = ErrorMessage.class),
            @ApiResponse(code = 201, message = "Alles gute")})
    @Authorization("Bearer")
    @Metered
    public Response newBot(@Context ContainerRequestContext context,
                           @ApiParam @Valid @NotNull NewBot newBot) {

        NewBotResponseModel ret = new NewBotResponseModel();

        try {
            UUID botId = newBot.id;
            // put information to every log for more information
            MDCUtils.put("botId", botId);
            MDCUtils.put("conversationId", newBot.conversation.id);
            MDCUtils.put("userId", newBot.origin.id);

            String token = (String) context.getProperty("wire-auth");
            if (!onNewBot(newBot, token)) {
                return Response
                        .status(409)
                        .entity(new ErrorMessage("User not whitelisted or service does not accept new instances atm"))
                        .build();
            }

            boolean saveState = storageF.create(botId).saveState(newBot);
            if (!saveState) {
                Logger.warning("Failed to save the state. Bot: %s", botId);
            }

            ret.name = handler.getName(newBot);
            ret.accentId = handler.getAccentColour();
            String profilePreview = handler.getSmallProfilePicture();
            if (profilePreview != null) {
                ret.addAsset(profilePreview, "preview");
            }

            String profileBig = handler.getBigProfilePicture();
            if (profileBig != null) {
                ret.addAsset(profileBig, "complete");
            }

            try (Crypto crypto = cryptoF.create(botId)) {
                ret.lastPreKey = crypto.newLastPreKey();
                ret.preKeys = crypto.newPreKeys(0, 50);
            }

        } catch (Exception e) {
            Logger.exception(e, "newBot: %s", e.getMessage());
            return Response.
                    status(500).
                    entity(new ErrorMessage(e.getMessage())).
                    build();
        }

        return Response.
                ok(ret).
                status(201).
                build();
    }

    protected boolean onNewBot(NewBot newBot, String auth) {
        return handler.onNewBot(newBot, auth);
    }
}
