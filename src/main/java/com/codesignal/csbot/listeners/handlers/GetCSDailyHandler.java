package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.GetUserFeedMessage;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;



public class GetCSDailyHandler extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(GetCSDailyHandler.class);
    private static List<String> names = List.of("csdaily", "cs-daily", "csd");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Retrieve CodeSignal daily challenge information"; }

    public GetCSDailyHandler() {
        this.buildArgParser();
    }

    @SuppressWarnings("unchecked")
    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        this.parseArgs(event);
        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        Message message = new GetUserFeedMessage();
        csClient.send(message, (ResultMessage resultMessage) -> {
            LinkedHashMap<Object, Object> result = (LinkedHashMap<Object, Object>) resultMessage.getResult();
            ArrayList<LinkedHashMap<Object, Object>> feed =
                    (ArrayList<LinkedHashMap<Object, Object>>) result.get("feed");
            LinkedHashMap<Object, Object> challenge = (LinkedHashMap<Object, Object>) feed.get(0).get("challenge");
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Latest official challenge", null);
            eb.addField("Name", String.format("%s", challenge.get("name")), true);
            eb.addField("Task ID", String.format("%s", challenge.get("taskId")), true);
            eb.addField("Challenge ID", String.format("%s", challenge.get("_id")), true);
            eb.addField("Author ID", String.format("%s", challenge.get("authorId")), true);
            eb.addField("Status", String.format("%s", challenge.get("status")), true);
            eb.addField("Visibility", String.format("%s", challenge.get("visibility")), true);
            eb.addField("Type", String.format("%s", challenge.get("type")), true);
            eb.addField("General Type", String.format("%s", challenge.get("generalType")), true);
            eb.addField("Reward", String.format("%s", challenge.get("reward")), true);
            eb.addField("First Solution", String.format("%s", challenge.get("firstSolution")), true);
            eb.addField("Solution Count", String.format("%s", challenge.get("solutionCount")), true);
            eb.setColor(new Color(0xF4EB41));
            event.getChannel().sendMessage(eb.build()).queue();
        });
    }
}
