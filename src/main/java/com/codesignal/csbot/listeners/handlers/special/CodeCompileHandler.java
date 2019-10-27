package com.codesignal.csbot.listeners.handlers.special;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.taskService.GetRunRawResultMessage;
import com.codesignal.csbot.utils.LanguageAbbreviation;
import com.google.common.util.concurrent.RateLimiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")  // It's been @Beta for 8 years (since 2011).
public class CodeCompileHandler implements SpecialCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(CodeCompileHandler.class);

    private static Map<Long, RateLimiter> rateLimiters = new HashMap<>();
    private static Set<String> lruCache = Collections.newSetFromMap(new LinkedHashMap<>(){
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 100;
        }
    });

    private boolean shouldProcessMessage(Message message) {
        if (message.getAuthor().isBot()) {
            return false;
        }

        return message.getContentRaw().matches("(?s)```[^\\s]+.*```");
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (!shouldProcessMessage(event.getMessage())) {
            return;
        }
        lruCache.add(event.getMessageId());
        event.getMessage().addReaction("▶").queue();
    }

    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!lruCache.contains(event.getMessageId())) {
            return;
        }
        processRequestMessage(event.getMessage(), event.getAuthor().getName());
    }

    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot() || !lruCache.contains(event.getMessageId())) {
            return;
        }
        event.getChannel().retrieveMessageById(event.getMessageId()).queue(
                (message) -> processRequestMessage(message, event.getUser().getName())
        );
    }

    private void processRequestMessage(Message message, final String requester) {
        long authorId = message.getAuthor().getIdLong();
        if (!rateLimiters.containsKey(authorId)) {
            rateLimiters.put(authorId, RateLimiter.create(0.2));
        }
        if (!rateLimiters.get(authorId).tryAcquire(1)) {
            message.getChannel().sendMessage(
                    "Slow down buddy. Please wait 5 seconds between requests.").queue();
            return;
        }
        Pattern pattern = Pattern.compile("```([^\\s]+)\n(.*)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(message.getContentRaw());

        if (matcher.find()) {
            String lang = LanguageAbbreviation.toStandardized(matcher.group(1));
            if (null == lang) {
                message.getChannel().sendMessage(String.format(
                        "Can't find any language that matches \"%s\".", lang));
                return;
            }
            String code = matcher.group(2);

            Emote loadingEmote = message.getJDA().getEmoteById(508808716376866826L);
            if (loadingEmote != null) {
                message.addReaction(loadingEmote).queue();
            }
            CodesignalClient csClient = CodesignalClientSingleton.getInstance();
            csClient.send(new GetRunRawResultMessage(code, lang), (result) -> {
                int runTime = result.getResult().get("runTime").asInt();
                EmbedBuilder eb = new EmbedBuilder();
                if (result.getResult().get("verdict").asText().equals("OK")) {
                    eb.setColor(new Color(0x9CF434));
                    eb.setTitle("Output");
                    eb.setDescription(String.format("```\n%s```", result.getResult().get("output").asText()));
                } else {
                    eb.setColor(new Color(0xF45C37));
                    eb.setTitle(result.getResult().get("verdict").asText());
                    if (!result.getResult().get("compilationLog").asText().isEmpty()) {
                        eb.setDescription("```\n" + result.getResult().get("compilationLog").asText() + "```");
                    }
                }
                eb.setFooter(String.format("Requested by %s | Runtime: %d ms", requester, runTime));
                message.getChannel().sendMessage(eb.build()).queue();
                message.getChannel().retrieveMessageById(message.getIdLong()).queue(
                        (updatedMessage) ->
                                updatedMessage.getReactions().forEach(messageReaction -> {
                                    messageReaction.removeReaction().queue();
                                })
                );
            });
        }
    }
}
