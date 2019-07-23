package com.codesignal.csbot.watchers;

import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainWatcher {
    private static final Logger logger = LoggerFactory.getLogger(MainWatcher.class);

    public static void init(JDA discordClient) {
//        UTCWatcher utcWatcher = new UTCWatcher();
//        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
//                () -> utcWatcher.run(discordClient),
//                60 - ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).getSecond(),
//                TimeUnit.MINUTES.toSeconds(1),
//                TimeUnit.SECONDS);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

        ChallengeWatcher officialChallengeWatcher = new ChallengeWatcher("all", new Color(0xfebe1e));
        service.scheduleAtFixedRate(
                () -> officialChallengeWatcher.run(discordClient),
                0,
                5,
                TimeUnit.SECONDS);

        ChallengeWatcher communityChallengeWatcher = new ChallengeWatcher("all", new Color(0xabfe1e));
        service.scheduleAtFixedRate(
                () -> communityChallengeWatcher.run(discordClient),
                0,
                5,
                TimeUnit.SECONDS);
    }
}
