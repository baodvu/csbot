package com.codesignal.csbot.listeners;

import com.codesignal.csbot.listeners.handlers.CommandHandler;
import com.codesignal.csbot.listeners.handlers.GetCSDailyHandler;
import com.codesignal.csbot.listeners.handlers.PingCommandHandler;
import com.codesignal.csbot.listeners.handlers.UndeleteCommandHandler;
import com.codesignal.csbot.models.DiscordMessage;
import com.codesignal.csbot.models.DiscordMessageVersioned;
import com.codesignal.csbot.storage.Storage;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class MessageListener extends ListenerAdapter {
    private static final String COMMAND_PREFIX = ".";
    private static final List<CommandHandler> COMMAND_HANDLERS = List.of(
            new GetCSDailyHandler(),
            new PingCommandHandler(),
            new UndeleteCommandHandler()
    );
    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
    private Storage storage = new Storage();
    private SpellChecker spellChecker;
    private Map<String, CommandHandler> commandHandlerMap = new HashMap<>();

    public MessageListener() {
        buildLuceneDictionary();
    }

    private void buildLuceneDictionary() {
        StringBuilder sb = new StringBuilder();
        for (CommandHandler handler: COMMAND_HANDLERS) {
            for (String name: handler.getNames()) {
                sb.append(name);
                sb.append("\n");
                commandHandlerMap.put(name, handler);
            }
        }
        StringReader reader = new StringReader(sb.toString());

        try {
            PlainTextDictionary words = new PlainTextDictionary(reader);

            // use in-memory lucene spell checker to make the suggestions
            RAMDirectory dir = new RAMDirectory();
            spellChecker = new SpellChecker(dir);
            spellChecker.indexDictionary(words, 10, 10);
        } catch (IOException e) {
            logger.error("This can't be happening.");
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            // Ignores messages coming from bots.
            return;
        }

        if (event.isFromType(ChannelType.PRIVATE)) {
            // Ignores private messages.
            logger.info("[PM] {}: {}",
                    event.getAuthor().getName(),
                    event.getMessage().getContentDisplay());
        } else {
            // Saves message into the database.
            String content = event.getMessage().getContentDisplay();

            if (!event.getMessage().getAttachments().isEmpty()) {
                content += "\n" + event.getMessage().getAttachments().stream().map(
                        att -> "Attachment: " + att.getUrl()
                ).collect(Collectors.joining("\n"));
            }

            logger.info("[{}][{}] {}: {}",
                    event.getGuild().getName(),
                    event.getTextChannel().getName(),
                    event.getMember() != null ? event.getMember().getEffectiveName() : "",
                    content);

            storage.saveMessage(
                    new DiscordMessage(
                            event.getChannel().getIdLong(),
                            event.getMessageIdLong(),
                            com.datastax.driver.core.utils.UUIDs.timeBased(),
                            event.getAuthor().getIdLong(),
                            content
                    )
            );
        }

        // If this is a command for the bot, indicated by COMMAND_PREFIX, then pass it over to the
        // appropriate handler, using lucene approximate matching if needed.
        String message = event.getMessage().getContentRaw();
        if (message.startsWith(COMMAND_PREFIX)) {
            StringTokenizer tokens = new StringTokenizer(message.substring(COMMAND_PREFIX.length()));

            if (tokens.countTokens() > 0) {
                String command = tokens.nextToken();
                try {
                    if (!commandHandlerMap.containsKey(command)) {
                        String[] similarCommands = spellChecker.suggestSimilar(command, 1);
                        if (similarCommands.length > 0) {
                            command = similarCommands[0];
                            event.getTextChannel().sendMessage(
                                    String.format("*Did you mean to say `.%s`?*", command)
                            ).queue();
                        } else {
                            command = null;
                        }
                    }
                    if (command != null) {
                        commandHandlerMap.get(command).onMessageReceived(event);
                    }
                } catch (IOException exception) {
                    logger.error("IOException occurred in lucene spellchecker");
                }
            }
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        DiscordMessage message = storage.getMessage(
                event.getChannel().getIdLong(),
                event.getMessageIdLong()
        );

        // If message is of a bot, then it will be null since we don't store bot messages.
        if (message == null) return;

        storage.saveVersionedMessage(
                new DiscordMessageVersioned(
                        message.getChannelId(),
                        message.getMessageId(),
                        message.getCreatedAt(),
                        message.getAuthorId(),
                        message.getContent()
                )
        );

        String content = event.getMessage().getContentDisplay();

        if (!event.getMessage().getAttachments().isEmpty()) {
            content += "\n" + event.getMessage().getAttachments().stream().map(
                    att -> "Attachment: " + att.getUrl()
            ).collect(Collectors.joining("\n"));
        }

        logger.info("[update] message_id: {} | message: {} | channel: {}",
                event.getMessageId(),
                event.getMessage(),
                event.getChannel());

        storage.saveVersionedMessage(
                new DiscordMessageVersioned(
                        event.getChannel().getIdLong(),
                        event.getMessageIdLong(),
                        com.datastax.driver.core.utils.UUIDs.timeBased(),
                        event.getAuthor().getIdLong(),
                        content
                )
        );
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        logger.info("[delete] message_id: {} | channel: {}",
                event.getMessageId(),
                event.getChannel());

        DiscordMessage message = storage.getMessage(
                event.getChannel().getIdLong(),
                event.getMessageIdLong()
        );

        // If message is of a bot, then it will be null since we don't store bot messages.
        if (message == null) return;

        storage.saveVersionedMessage(
                new DiscordMessageVersioned(
                        message.getChannelId(),
                        message.getMessageId(),
                        message.getCreatedAt(),
                        message.getAuthorId(),
                        message.getContent()
                )
        );

        storage.saveVersionedMessage(
                new DiscordMessageVersioned(
                        event.getChannel().getIdLong(),
                        event.getMessageIdLong(),
                        com.datastax.driver.core.utils.UUIDs.timeBased(),
                        message.getAuthorId(),
                        "[deleted]"
                )
        );
    }
}
