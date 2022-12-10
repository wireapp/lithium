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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.lithium.ClientRepo;
import com.wire.lithium.server.monitoring.MDCUtils;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.MessageResourceBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.assets.Reaction;
import com.wire.xenon.backend.models.ErrorMessage;
import com.wire.xenon.backend.models.Payload;
import com.wire.xenon.exceptions.MissingStateException;
import com.wire.xenon.tools.Logger;
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
    private final ClientRepo repo;

    public MessageResource(MessageHandlerBase handler, ClientRepo repo) {
        super(handler);
        this.repo = repo;
    }

    @POST
    @ApiOperation(value = "New OTR Message")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Invalid Authorization", response = ErrorMessage.class),
            @ApiResponse(code = 503, message = "Missing bot's state object", response = ErrorMessage.class),
            @ApiResponse(code = 200, message = "Alles gute")})
    @Authorization("Bearer")
    @Metered
    public Response newMessage(@ApiParam("UUID Bot instance id") @PathParam("bot") UUID botId,
                               @ApiParam("UUID Unique event id") @QueryParam("id") UUID eventId,
                               @ApiParam @Valid @NotNull Payload payload) throws IOException {

        if (eventId == null) {
            eventId = UUID.randomUUID(); //todo fix this once Wire BE adds eventId into payload
        }

        if (Logger.getLevel() == Level.FINE) {
            Logger.debug("eventId: %s, botId: %s, %s",
                    eventId,
                    botId,
                    objectMapper.writeValueAsString(payload));
        }

        // put tracing information to logs
        MDCUtils.put("botId", botId);
        MDCUtils.put("eventId", eventId);
        MDCUtils.put("conversationId", payload.conversation.id);

        try (WireClient client = getWireClient(botId, payload)) {
            handleMessage(eventId, payload, client);
        } catch (CryptoException e) {
            Logger.exception("newMessage: %s %s", e, botId, e.getMessage());
            respondWithError(botId, payload);
            return Response.
                    status(503).
                    entity(new ErrorMessage(e.getMessage())).
                    build();
        } catch (MissingStateException e) {
            Logger.exception("newMessage: %s %s", e, botId, e.getMessage());
            return Response.
                    status(410).
                    entity(new ErrorMessage(e.getMessage())).
                    build();
        } catch (Exception e) {
            Logger.exception("newMessage: %s %s", e, botId, e.getMessage());
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
            client.send(new Reaction(UUID.randomUUID(), ""));
        } catch (Exception e) {
            Logger.exception("respondWithError: bot: %s %s", e, botId, e.getMessage());
        }
    }

    protected WireClient getWireClient(UUID botId, Payload payload) throws IOException, CryptoException {
        return repo.getClient(botId);
    }
}
