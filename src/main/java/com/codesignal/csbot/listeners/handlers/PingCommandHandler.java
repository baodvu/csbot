package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import java.util.List;

public class PingCommandHandler extends AbstractCommandHandler {
    private static List<String> names = List.of("ping");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "A simple handler example"; }

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        this.parseArgs(event);
        event.getTextChannel().sendMessage("pong").queue();
    }
}
