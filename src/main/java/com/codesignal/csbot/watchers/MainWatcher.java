package com.codesignal.csbot.watchers;

import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainWatcher {
    private static final Logger logger = LoggerFactory.getLogger(MainWatcher.class);

    public static void init(JDA discordClient) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                () -> new UTCWatcher().run(discordClient),
                60 - ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).getSecond(),
                TimeUnit.MINUTES.toSeconds(1),
                TimeUnit.SECONDS);
    }
}
