package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.MethodMessage;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.List;

public class SendMethodMessageHandler extends AbstractCommandHandler{
    private static List<String> names = List.of("cssendraw");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Execute a raw WSS method request"; }

    public SendMethodMessageHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("-m", "--method")
                .type(String.class)
                .help("Meteor method");
        parser.addArgument("params").nargs("*")
                .help("Parameters");
    }

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        if (!event.getAuthor().getName().equals("ephemeraldream")
                && (event.getMember() == null
                || !event.getMember().hasPermission(Permission.ADMINISTRATOR))) {
            return;
        }
        Namespace ns = this.parseArgs(event);

        String method = ns.getString("method");
        String params = String.join(" ", ns.getList("params"));

        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        ResultMessage resultMessage;
        try {
            resultMessage = csClient.send(
                    new MethodMessage(
                            method,
                            new ObjectMapper().readValue(params, new TypeReference<List<JsonNode>>() {})
                    )
            ).get();
        } catch (Exception e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        if (resultMessage.getResult() != null) {
            eb.addField("Response",
                    StringUtils.abbreviate(String.format("%s", resultMessage.getResult()), 1024),
                    false);
        }
        if (resultMessage.getError() != null) {
            eb.addField("Error",
                    StringUtils.abbreviate(String.format("%s", resultMessage.getError()), 1024),
                    false);
        }
        eb.setColor(new Color(0xF45964));
        event.getChannel().sendMessage(eb.build()).queue();
    }
}
