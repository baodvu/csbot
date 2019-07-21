package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import java.util.List;


public interface CommandHandler {
    /**
     * Name and aliases of the command.
     * Note that the first string will be the name, and the rest is the aliases.
     * The name should be self-descriptive, such that it can be used in a command list.
     */
    List<String> getNames();

    /**
     * A short description (less than 100 characters) of what the command does.
     */
    String getShortDescription();

    /**
     * This handler is called when a new message is received.
     *
     * @param event Discord message event, which you can use to read the message content and reply
     */
    void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException;
}
