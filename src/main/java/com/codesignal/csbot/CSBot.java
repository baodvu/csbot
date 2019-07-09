package com.codesignal.csbot;

import com.codesignal.csbot.listeners.MessageListener;
import com.codesignal.csbot.watchers.MainWatcher;
import com.codesignal.csbot.wss.CSWebSocket;
import io.sentry.Sentry;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CSBot {
    private static final Logger logger = LoggerFactory.getLogger(CSBot.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting up Codesignal Bot");

        // Exception logging
        Sentry.init();

        // Watchers to pull latest changes to a website / page
        MainWatcher.init();

        String DISCORD_BOT_TOKEN = System.getenv("DISCORD_TOKEN");

        new JDABuilder(AccountType.BOT)
                .setToken(DISCORD_BOT_TOKEN)
                .setActivity(Activity.playing("codesignal"))
                .addEventListeners(new MessageListener())
                .build();

        new CSWebSocket().build();
    }
}
