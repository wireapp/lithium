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

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.server.model.InboundMessage;
import com.wire.bots.sdk.tools.AuthValidator;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Path("/bots/{bot}/messages")
public class MessageResource extends MessageResourceBase {
    private final AuthValidator validator;

    public MessageResource(MessageHandlerBase handler, AuthValidator validator, ClientRepo repo) {
        super(handler, repo);
        this.validator = validator;
    }

    @POST
    public Response newMessage(@HeaderParam("Authorization") String auth,
                               @PathParam("bot") String botId,
                               InboundMessage inbound) {

        if (!validator.validate(auth)) {
            Logger.warning(String.format("%s, Invalid auth. Got: '%s'",
                    botId,
                    auth
            ));
            return Response.
                    ok("Invalid Authorization: " + auth).
                    status(403).
                    build();
        }

        WireClient client = repo.getWireClient(botId);
        if (client == null) {
            return Response.
                    ok().
                    status(503).
                    build();
        }

        try {
            handleMessage(inbound, client);
        } catch (Exception e) {
            Logger.error("MessageResource::newMessage: bot: %s %s", botId, e);
            return Response.
                    status(400).
                    entity(e).
                    build();
        }

        return Response.
                ok().
                status(200).
                build();
    }
}