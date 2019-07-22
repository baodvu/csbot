package com.codesignal.csbot.watchers;

import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainWatcher {
    private static final Logger logger = LoggerFactory.getLogger(MainWatcher.class);

    public static void init(JDA discordClient) {
        UTCWatcher utcWatcher = new UTCWatcher();
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
                () -> utcWatcher.run(discordClient),
                60 - ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).getSecond(),
                TimeUnit.MINUTES.toSeconds(1),
                TimeUnit.SECONDS);

        OfficialChallengeWatcher challengeWatcher = new OfficialChallengeWatcher();
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
                () -> challengeWatcher.run(discordClient),
                0,
                5,
                TimeUnit.SECONDS);
    }
}
