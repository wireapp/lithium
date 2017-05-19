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
import com.wire.bots.sdk.server.model.InboundMessage;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Path("/bots/{bot}/messages")
public class MessageResource extends MessageResourceBase {
    public MessageResource(MessageHandlerBase handler, Configuration conf, ClientRepo repo) {
        super(handler, conf, repo);
    }

    @POST
    public Response newMessage(@HeaderParam("Authorization") String auth,
                               @PathParam("bot") String bot,
                               InboundMessage inbound) throws Exception {

        if (!Util.compareTokens(conf.getAuth(), auth)) {
            Logger.warning(String.format("Invalid auth. Got: '%s'",
                    auth
            ));
            return Response.
                    ok("Invalid Authorization: " + auth).
                    status(403).
                    build();
        }

        WireClient client = repo.getWireClient(bot);
        if (client == null) {
            return Response.
                    ok().
                    status(410).
                    build();
        }

        handleMessage(inbound, client);

        return Response.
                ok().
                status(200).
                build();
    }
}