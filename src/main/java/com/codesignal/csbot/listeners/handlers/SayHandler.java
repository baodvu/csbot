package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.List;

public class SayHandler extends AbstractCommandHandler{
    private static List<String> names = List.of("say");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Tell the bot to say something."; }

    public SayHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("-c", "--channel-id")
                .type(Long.class)
                .help("Specify language to filter on")
                .required(true);
        parser.addArgument("text")
                .help("What you want the bot to say");
    }

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            // Only admins can do dis.
            return;
        }
        Namespace ns = this.parseArgs(event);

        long channelId = ns.getLong("channel_id");
        String text = ns.getString("text");

        TextChannel channel = event.getGuild().getTextChannelById(channelId);
        if (channel == null) return;

        channel.sendMessage(text).queue();
    }
}
