package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Set;

public interface CommandHandler {
    /**
     * Name and aliases of the command.
     */
    Set<String> getNames();

    /**
     * A short description (less than 100 characters) of what the command does.
     */
    String getShortDescription();

    /**
     * A short description of how the command can be used.
     *
     * Example:
     *
     * Usage: undelete <options>
     *     -n +NUM to output the last NUM lines
     */
    String getUsage();

    /**
     * This handler is called when a new message is received.
     *
     * @param event Discord message event, which you can use to read the message content and reply
     */
    void onMessageReceived(MessageReceivedEvent event);
}
