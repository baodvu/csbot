package com.codesignal.csbot.listeners.handlers.special;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.taskService.GetRunRawResultMessage;
import com.codesignal.csbot.utils.LanguageAbbreviation;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.RateLimiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")  // It's been @Beta for 8 years (since 2011).
public class CodeCompileHandler implements SpecialCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(CodeCompileHandler.class);
    private static final long LOADING_EMOTE_ID = 508808716376866826L;

    private static Map<Long, RateLimiter> rateLimiters = new HashMap<>();
    private static Set<String> lruCache = Collections.newSetFromMap(new LinkedHashMap<>(){
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 100;
        }
    });

    private boolean shouldNotProcessMessage(Message message) {
        if (message.getAuthor().isBot()) {
            return true;
        }

        return !message.getContentRaw().matches("(?s)dev```[^\\s]+.*```");
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (shouldNotProcessMessage(event.getMessage())) {
            return;
        }
        lruCache.add(event.getMessageId());
        event.getMessage().addReaction("▶").queue();
    }

    public void onMessageUpdate(MessageUpdateEvent event) {
        if (shouldNotProcessMessage(event.getMessage())) {
            return;
        }
        lruCache.add(event.getMessageId());
        event.getMessage().addReaction("▶").queue();
        event.getMessage().getChannel().retrieveMessageById(event.getMessageIdLong()).queue(
                (updatedMessage) -> {
                    if (updatedMessage.getReactions().stream().anyMatch(messageReaction ->
                            messageReaction.getReactionEmote().isEmoji()
                                    && messageReaction.getReactionEmote().getEmoji().equals("▶")
                                    && messageReaction.getCount() > 1
                    )) {
                        processRequestMessage(event.getMessage(), event.getAuthor().getName());
                    }
                }
        );
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
                    "Slow down. There's a limit of 5 seconds between requests.").queue();
            return;
        }
        Pattern pattern = Pattern.compile("dev```([^\\s]+)\n(.*)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(message.getContentRaw());

        if (matcher.find()) {
            String lang = LanguageAbbreviation.toStandardized(matcher.group(1));
            if (null == lang) {
                message.getChannel().sendMessage(String.format(
                        "Can't find any language that matches `%s`.", matcher.group(1))).queue();
                return;
            }
            String code = matcher.group(2);

            Emote loadingEmote = message.getJDA().getEmoteById(LOADING_EMOTE_ID);
            if (loadingEmote != null) {
                message.addReaction(loadingEmote).queue();
            }
            CodesignalClient csClient = CodesignalClientSingleton.getInstance();
            csClient.send(new GetRunRawResultMessage(code, lang), (result) -> {
                if (result.getError() != null) {
                    message.getChannel().sendMessage(
                            "Reimu can't handle your request at the moment :<").queue();
                    throw new RuntimeException("Failed to get raw run result: " + result.getError().toString());
                }
                int runTime = result.getResult().get("runTime").asInt();
                EmbedBuilder eb = new EmbedBuilder();
                if (result.getResult().get("verdict").asText().equals("OK")) {
                    eb.setColor(new Color(0x9CF434));
                    eb.setTitle("Output");
                    eb.setDescription(String.format("```\n%s```",
                            StringUtils.abbreviate(result.getResult().get("output").asText(), 2040)));
                } else {
                    eb.setColor(new Color(0xF45C37));
                    eb.setTitle(result.getResult().get("verdict").asText());
                    if (!result.getResult().get("compilationLog").asText().isEmpty()) {
                        eb.setDescription(String.format("```\n%s```",
                                StringUtils.abbreviate(result.getResult().get("compilationLog").asText(), 2040)));
                    }
                }
                eb.setFooter(String.format("Requested by %s | Runtime: %d ms", requester, runTime));
                message.getChannel().sendMessage(eb.build()).queue();
                message.getChannel().retrieveMessageById(message.getIdLong()).queue(
                        (updatedMessage) ->
                                updatedMessage.getReactions().forEach(messageReaction -> {
                                    if (messageReaction.getReactionEmote().isEmote()
                                            && messageReaction.getReactionEmote().getEmote().getIdLong() ==
                                            LOADING_EMOTE_ID) {
                                        messageReaction.removeReaction().queue();
                                    }
                                })
                );
            });
        }
    }
}
