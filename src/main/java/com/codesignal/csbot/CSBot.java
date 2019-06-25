package com.codesignal.csbot;

import com.codesignal.csbot.listeners.MessageListener;
import io.sentry.Sentry;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

public class CSBot {
    private static final Logger logger = LoggerFactory.getLogger(CSBot.class);

    public static void main(String[] args) throws LoginException {
        logger.info("Starting up Codesignal Bot");

        // Exception logging
        Sentry.init();

        String DISCORD_BOT_TOKEN = System.getenv("DISCORD_TOKEN");

        new JDABuilder(AccountType.BOT)
                .setToken(DISCORD_BOT_TOKEN)
                .setActivity(Activity.playing("codesignal"))
                .addEventListeners(new MessageListener())
                .build();
    }
}
