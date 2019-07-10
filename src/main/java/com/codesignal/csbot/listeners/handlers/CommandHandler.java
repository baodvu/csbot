package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Set;

public interface CommandHandler {
    Set<String> getNames();

    void onMessageReceived(MessageReceivedEvent event);
}
