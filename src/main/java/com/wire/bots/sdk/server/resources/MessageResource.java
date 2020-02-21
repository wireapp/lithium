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

import com.codahale.metrics.annotation.Metered;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.exceptions.MissingStateException;
import com.wire.bots.sdk.server.model.ErrorMessage;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.tools.Logger;
import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

@Api
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/bots/{bot}/messages")
public class MessageResource extends MessageResourceBase {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageResource(MessageHandlerBase handler, ClientRepo repo) {
        super(handler, repo);
    }

    @POST
    @ApiOperation(value = "New OTR Message")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Invalid Authorization", response = ErrorMessage.class),
            @ApiResponse(code = 503, message = "Missing bot's state object", response = ErrorMessage.class),
            @ApiResponse(code = 200, message = "Alles gute")})
    @Authorization("Bearer")
    @Metered
    public Response newMessage(@ApiParam("Service token as Bearer") @NotNull @HeaderParam("Authorization") String auth,
                               @ApiParam("UUID Bot instance id") @PathParam("bot") UUID botId,
                               @ApiParam("UUID Unique message id") @QueryParam("id") UUID messageID,
                               @ApiParam @Valid @NotNull Payload payload) throws IOException {

        if (Logger.getLevel() == Level.FINE) {
            String strPayload = objectMapper.writeValueAsString(payload);
            Logger.debug("MessageResource: bot: %s, id: %s, %s", botId, messageID, strPayload);
        }

        if (messageID == null) {
            messageID = UUID.randomUUID(); //todo fix this once Wire BE adds messageId into payload
        }

        try (WireClient client = getWireClient(botId, payload)) {
            handleMessage(messageID, payload, client);
        } catch (CryptoException e) {
            Logger.error("newMessage: %s %s", botId, e);
            respondWithError(botId, payload);
            return Response.
                    status(503).
                    entity(new ErrorMessage(e.getMessage())).
                    build();
        } catch (MissingStateException e) {
            Logger.error("newMessage: %s %s", botId, e);
            return Response.
                    status(410).
                    entity(new ErrorMessage(e.getMessage())).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("newMessage: %s %s", botId, e);
            return Response.
                    status(400).
                    entity(new ErrorMessage(e.getMessage())).
                    build();
        }

        return Response.
                ok().
                status(200).
                build();
    }

    private void respondWithError(UUID botId, Payload payload) {
        try (WireClient client = getWireClient(botId, payload)) {
            client.sendReaction(UUID.randomUUID(), "");
        } catch (Exception e1) {
            Logger.error("respondWithError: bot: %s %s", botId, e1);
        }
    }
}
