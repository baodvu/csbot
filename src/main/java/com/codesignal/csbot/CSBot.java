package com.codesignal.csbot;

import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.listeners.MessageListener;
import com.codesignal.csbot.watchers.MainWatcher;
import io.sentry.Sentry;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class CSBot {
    private static final Logger logger = LoggerFactory.getLogger(CSBot.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting up Codesignal Bot");
        logger.info(String.format("Environment variables: env=%s", System.getenv("ENV")));

        // Exception logging
        Sentry.init();

        // Init discord client
        String DISCORD_BOT_TOKEN = System.getenv("DISCORD_TOKEN");
        JDA discordClient = new JDABuilder(AccountType.BOT)
                .setToken(DISCORD_BOT_TOKEN)
                .setActivity(Activity.playing("codesignal"))
                .addEventListeners(new MessageListener())
                .build().awaitReady();

        // Watchers to pull latest changes to a website / page
        MainWatcher.init(discordClient);

        CodesignalClientSingleton.getInstance();
    }
}
