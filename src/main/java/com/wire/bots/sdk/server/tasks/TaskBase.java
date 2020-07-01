package com.wire.bots.sdk.server.tasks;

import io.dropwizard.servlets.tasks.Task;

import java.util.List;
import java.util.Map;

public abstract class TaskBase extends Task {

    public TaskBase(String name) {
        super(name);
    }

    protected static int extract(Map<String, List<String>> parameters, String name) {
        return extract(parameters, name, 0);
    }

    protected static int extract(Map<String, List<String>> parameters, String name, int def) {
        int val = def;
        final List<String> usr = parameters.get(name);
        if (!usr.isEmpty()) {
            String id = usr.get(0);
            val = Integer.parseInt(id);
        }

        return val;
    }

    protected static String extractString(Map<String, List<String>> parameters, String name, String def) {
        final List<String> usr = parameters.get(name);
        if (!usr.isEmpty()) {
            return usr.get(0);
        }

        return def;
    }

    protected static String extractString(Map<String, List<String>> parameters, String name) {
        return extractString(parameters, name, "");
    }
}
