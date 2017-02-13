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

package com.wire.bots.sdk.server.tasks;

import com.google.common.collect.ImmutableMultimap;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;

import java.io.PrintWriter;

/**
 * Task class that will send text for one particular botId (one conversation)
 * Usage:
 * curl -X POST http://localhost:8051/tasks/broadcast --data  "bot=$BOTID&text=Hello"
 * You need to exec this task against your `admin port`
 * For more on dropwizard tasks check out: http://www.dropwizard.io/1.0.5/docs/manual/core.html#tasks
 */
public class BroadcastTask extends TaskBase {
    protected final Configuration conf;
    protected final ClientRepo repo;

    public BroadcastTask(Configuration conf, ClientRepo repo) {
        super("broadcast");
        this.conf = conf;
        this.repo = repo;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        String botId = extractString(parameters, "bot");
        if (botId.isEmpty()) {
            output.println("Missing bot parameter");
            return;
        }

        String text = extractString(parameters, "text");

        if (text.isEmpty()) {
            output.println("Are you missing `text` param?");
            return;
        }

        try {
            WireClient client = repo.getWireClient(botId);
            client.sendText(text);
            output.printf("Bot %s sent some text\n", client.getId());
        } catch (Exception e) {
            Logger.error(e.getMessage());
            output.println(e.getMessage());
        }
    }
}
