package com.wire.bots.sdk.server.tasks;

import com.google.common.collect.ImmutableMultimap;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task class that will post text into ALL conversation that this Service has!!!
 * Usage:
 * curl -X POST http://localhost:8051/tasks/massive_broadcast --data "text=Hello"
 * You need to exec this task against your `admin port`
 * For more on dropwizard tasks check out: http://www.dropwizard.io/1.0.5/docs/manual/core.html#tasks
 */
public class BroadcastAllTask extends TaskBase {
    protected final Configuration conf;
    protected final ClientRepo repo;
    AtomicInteger succeeded = new AtomicInteger(0);
    AtomicInteger failed = new AtomicInteger(0);

    public BroadcastAllTask(Configuration conf, ClientRepo repo) {
        super("massive_broadcast");
        this.conf = conf;
        this.repo = repo;
    }

    @Override
    public void execute(final ImmutableMultimap<String, String> parameters, final PrintWriter output) throws Exception {
        int threads = extract(parameters, "th", 20);
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(threads);

        File dir = new File(conf.cryptoDir);
        if (!dir.exists()) {
            output.println("Dir: " + dir.getAbsolutePath() + " does not exist");
            return;
        }

        final String text = extractString(parameters, "text");
        if (text.isEmpty()) {
            output.println("Are you missing `text` param?");
            return;
        }

        File[] botDirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().split("-").length == 5 && file.isDirectory();
            }
        });

        Date start = new Date();
        for (File botDir : botDirs) {
            final String botId = botDir.getName();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    send(output, text, botId);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.HOURS);

        Date end = new Date();
        long l = end.getTime() - start.getTime();
        output.printf("Processed: %,d/%,d convs in %,dsec\n",
                succeeded.get(),
                failed.get(),
                TimeUnit.MILLISECONDS.toSeconds(l));

        succeeded.set(0);
        failed.set(0);
    }

    private void send(PrintWriter output, String text, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            if (client != null) {
                client.sendText(text);

                int curr = succeeded.incrementAndGet();
                if (curr % 100 == 0) {
                    output.println(curr);
                    output.flush();
                }
            }
        } catch (Exception e) {
            String msg = String.format("Bot: %s. Error: %s", botId, e.getMessage());
            Logger.error(msg);

            output.println("Failed for botId: " + botId);
            output.flush();
            failed.incrementAndGet();
        }
    }
}
