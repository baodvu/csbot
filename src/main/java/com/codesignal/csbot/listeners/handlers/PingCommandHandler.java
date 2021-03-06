package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.listeners.BotCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import java.util.List;

public class PingCommandHandler extends AbstractCommandHandler {
    private static final List<String> names = List.of("?ing");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "A simple handler example"; }

    public PingCommandHandler() {
        this.buildArgParser();
    }

    public void onMessageReceived(MessageReceivedEvent event, BotCommand botCommand) throws ArgumentParserException {
        this.parseArgs(event);

        if (botCommand.getRawCommandName().endsWith("ing")) {
            event.getChannel().sendMessage(
                    botCommand.getRawCommandName().replace("ing", "ong")).queue();
        }
    }
}
