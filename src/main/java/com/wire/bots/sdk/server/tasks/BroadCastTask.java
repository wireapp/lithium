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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.OtrManager;
import com.wire.bots.sdk.Util;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.WireClientFactory;
import io.dropwizard.servlets.tasks.Task;

import java.io.File;
import java.io.PrintWriter;

public class BroadCastTask extends Task {
    protected final Configuration conf;
    protected final WireClientFactory factory;
    protected PrintWriter output;

    public BroadCastTask(Configuration conf, WireClientFactory factory) {
        super("broadcast");
        this.conf = conf;
        this.factory = factory;
    }

    protected void send(WireClient client, ImmutableMultimap<String, String> parameters) {
        try {
            String text = extractString(parameters, "text");
            if (!text.isEmpty()) {
                client.sendText(text);

                output.printf("Bot %s sent some text\n", client.getId());
            } else {
                output.println("Are you missing `text` param?");
            }
        } catch (Exception e) {
            output.print(e.getMessage());
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter printWriter) throws Exception {
        this.output = new PrintWriter(printWriter, true);
        String bot = extractString(parameters, "bot");
        if (bot.isEmpty()) {
            output.println("Missing bot parameter");
            return;
        }

        String path = String.format("%s/%s", conf.getCryptoDir(), bot);

        try (OtrManager otrManager = new OtrManager(path)) {

            String clientId = Util.readLine(new File(path + "/client.id"));
            String token = Util.readLine(new File(path + "/token.id"));

            WireClient client = factory.createClient(otrManager, bot, bot, clientId, token);

            send(client, parameters);
        } catch (Exception e) {
            Logger.error(e.getMessage());
            output.println(e.getMessage());
        }
    }

    protected static int extract(ImmutableMultimap<String, String> parameters, String name) {
        return extract(parameters, name, 0);
    }

    protected static int extract(ImmutableMultimap<String, String> parameters, String name, int def) {
        int val = def;
        ImmutableCollection<String> usr = parameters.get(name);
        if (!usr.isEmpty()) {
            String id = usr.asList().get(0);
            val = Integer.parseInt(id);
        }

        return val;
    }

    protected static String extractString(ImmutableMultimap<String, String> parameters, String name, String def) {
        ImmutableCollection<String> usr = parameters.get(name);
        if (!usr.isEmpty()) {
            return usr.asList().get(0);
        }

        return def;
    }

    protected static String extractString(ImmutableMultimap<String, String> parameters, String name) {
        return extractString(parameters, name, "");
    }
}
