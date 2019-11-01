package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.GetUserFeedMessage;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.listeners.BotCommand;
import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;



public class GetCSDailyHandler extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(GetCSDailyHandler.class);
    private static final List<String> names = List.of("csdaily", "cs-daily", "csd");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Retrieve CodeSignal daily challenge information"; }

    public GetCSDailyHandler() {
        this.buildArgParser();
    }

    public void onMessageReceived(MessageReceivedEvent event, BotCommand botCommand) throws ArgumentParserException {
        this.parseArgs(event);
        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        Message message = new GetUserFeedMessage("all", 0, 1);
        csClient.send(message, (ResultMessage resultMessage) -> {
            JsonNode challenge = resultMessage.getResult().at("/feed/0/challenge");
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Latest official challenge", null);
            eb.addField("Name", String.format("%s", challenge.get("name").textValue()), true);
            eb.addField("Task ID", String.format("%s", challenge.get("taskId").textValue()), true);
            eb.addField("Challenge ID", String.format("%s", challenge.get("_id").textValue()), true);
            eb.addField("Author ID", String.format("%s", challenge.get("authorId").textValue()), true);
            eb.addField("Status", String.format("%s", challenge.get("status").textValue()), true);
            eb.addField("Visibility", String.format("%s", challenge.get("visibility").textValue()), true);
            eb.addField("Type", String.format("%s", challenge.get("type").textValue()), true);
            eb.addField("General Type", String.format("%s", challenge.get("generalType").textValue()), true);
            eb.addField("Reward", String.format("%s", challenge.get("reward").textValue()), true);
            eb.addField("First Solution", String.format("%s", challenge.get("firstSolution").textValue()), true);
            eb.addField("Solution Count", String.format("%s", challenge.get("solutionCount").intValue()), true);
            eb.setColor(new Color(0xF4EB41));
            event.getChannel().sendMessage(eb.build()).queue();
        });
    }
}
