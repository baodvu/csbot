package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Set;

public class PingCommandHandler implements CommandHandler {
    private static Set<String> names = Set.of("ping");

    public Set<String> getNames() { return names; }
    public String getShortDescription() { return "A simple handler example"; }
    public String getUsage() { return "Usage: ping"; }

    public void onMessageReceived(MessageReceivedEvent event) {
        event.getTextChannel().sendMessage("pong").queue();
    }
}
