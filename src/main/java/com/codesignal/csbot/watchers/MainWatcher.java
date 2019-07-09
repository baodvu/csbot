package com.codesignal.csbot.watchers;

import java.util.Timer;
import java.util.TimerTask;

public class MainWatcher {
    public static void init() {
        final int DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new StockMarketCalendarWatcher().run();
            }
        }, 0L, DAY_IN_MILLISECONDS);
    }
}
