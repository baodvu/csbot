package com.codesignal.csbot.listeners.handlers;


import com.codesignal.csbot.listeners.BotCommand;
import com.codesignal.csbot.utils.Deluge;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.List;


public class MagnetHandler extends AbstractCommandHandler{
    private static final List<String> names = List.of("magnet");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Download a magnet link"; }

    public MagnetHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("link").required(true)
                .help("Magnet link that starts with `magnet:`");
    }

    public void onMessageReceived(MessageReceivedEvent event, BotCommand botCommand) throws ArgumentParserException {
        if (event.getMember() == null ||
                event.getMember().getRoles().stream().noneMatch(role -> role.getName().equals("ayaya"))) {
            // Only admins can do dis.
            return;
        }
        Namespace ns = this.parseArgs(event);
        MessageChannel channel = event.getChannel();

        String link = ns.getString("link");
        try {
            Deluge delugeClient = new Deluge();
            String delugeResponse = delugeClient.addMagnet(link);
            channel.sendMessage("Sending to Deluge: `" + link
                    + "`\n" + "Response: `" + delugeResponse + "`").queue();
        } catch (Exception e) {
            channel.sendMessage("Error: " + e.getMessage()).queue();
        }
    }
}

