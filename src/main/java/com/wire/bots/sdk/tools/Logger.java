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

package com.wire.bots.sdk.tools;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

public class Logger {
    private final static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
    private static AtomicInteger errorCount = new AtomicInteger();
    private static AtomicInteger warningCount = new AtomicInteger();

    static {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(Level.SEVERE);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(Level.SEVERE);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");

        //java.util.logging.Logger.setLevel(Level.INFO);
        ConsoleHandler stderr = new ConsoleHandler();
        stderr.setFormatter(new BotFormatter());
        LOGGER.addHandler(stderr);

        try {
            FileHandler fileHandler = new FileHandler("current.log", true);

            fileHandler.setFormatter(new BotFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.setUseParentHandlers(false);
    }

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

    public static void error(String msg) {
        errorCount.incrementAndGet();
        LOGGER.severe(msg);
    }

    public static void error(String format, Object... args) {
        errorCount.incrementAndGet();
        LOGGER.severe(String.format(format, args));
    }

    public static void warning(String msg) {
        warningCount.incrementAndGet();
        LOGGER.warning(msg);
    }

    public static void warning(String format, Object... args) {
        warningCount.incrementAndGet();
        LOGGER.warning(String.format(format, args));
    }

    public static int getErrorCount() {
        return errorCount.get();
    }

    public static int getWarningCount() {
        return warningCount.get();
    }

    static class BotFormatter extends Formatter {
        private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(df.format(new Date(record.getMillis()))).append(" - ");
            // builder.append("[").append(record.getSourceClassName()).append(".");
            // builder.append(record.getSourceMethodName()).append("] - ");
            builder.append("[").append(record.getLevel()).append("] - ");
            builder.append(formatMessage(record));
            builder.append("\n");
            return builder.toString();
        }

        @Override
        public String getHead(Handler h) {
            return super.getHead(h);
        }

        @Override
        public String getTail(Handler h) {
            return super.getTail(h);
        }
    }
}
