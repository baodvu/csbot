package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.GetUserFeedMessage;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;



public class GetCSDailyHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(GetCSDailyHandler.class);
    private static Set<String> names = Set.of("csdaily", "csd");

    public Set<String> getNames() { return names; }
    public String getShortDescription() { return "Retrieve CodeSignal daily challenge information"; }
    public String getUsage() { return "Usage: csdaily"; }

    @SuppressWarnings("unchecked")
    public void onMessageReceived(MessageReceivedEvent event) {
        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        Message message = new GetUserFeedMessage();
        csClient.send(message, (ResultMessage resultMessage) -> {
            LinkedHashMap<Object, Object> result = (LinkedHashMap<Object, Object>) resultMessage.getResult();
            ArrayList<Object> feed = (ArrayList<Object>) result.get("feed");
            LinkedHashMap<Object, Object> challenge = (LinkedHashMap<Object, Object>) feed.get(0);
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Latest official challenge", null);
            eb.addField("Task ID", (String) challenge.get("_id"), true);
            eb.addField("Challenge ID", (String) challenge.get("challengeId"), true);
            eb.setColor(new Color(0xF4EB41));
            event.getTextChannel().sendMessage(eb.build()).queue();
        });
    }
}
