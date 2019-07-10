package com.codesignal.csbot.watchers;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class MainWatcher {
    private static final Logger logger = LoggerFactory.getLogger(MainWatcher.class);

    public static void init() {
        final int DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    new StockMarketCalendarWatcher().run();
                } catch (UnirestException exp) {
                    logger.error("Failed to get data from investing.com");
                }
            }
        }, 0L, DAY_IN_MILLISECONDS);
    }
}
