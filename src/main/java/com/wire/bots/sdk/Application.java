package com.wire.bots.sdk;

import io.dropwizard.setup.Environment;

public class Application extends Server<Configuration> {
    public static void main(String[] args) throws Exception {
        new Application().run(args);
    }

    @Override
    protected MessageHandlerBase createHandler(Configuration configuration, Environment env) {
        return new MessageHandlerBase() {
        };
    }
}
