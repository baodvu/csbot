package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.List;

public class SayHandler extends AbstractCommandHandler{
    private static final List<String> names = List.of("say");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Tell the bot to say something."; }

    public SayHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("-c", "--channel-id")
                .type(Long.class)
                .help("Specify the channel to send the message");
        parser.addArgument("text").nargs("*")
                .help("What you want the bot to say");
    }

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            // Only admins can do dis.
            return;
        }
        Namespace ns = this.parseArgs(event);

        String text = String.join(" ", ns.getList("text"));

        TextChannel channel;
        if (ns.getLong("channel_id") != null) {
            long channelId = ns.getLong("channel_id");
            channel = event.getGuild().getTextChannelById(channelId);
        } else {
            channel = event.getTextChannel();
        }

        if (channel == null) return;

        channel.sendMessage(text).queue();
        event.getMessage().delete().queue();
    }
}
