package com.wire.bots.sdk.server.tasks;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;

public abstract class TaskBase extends Task {

    public TaskBase(String name) {
        super(name);
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
