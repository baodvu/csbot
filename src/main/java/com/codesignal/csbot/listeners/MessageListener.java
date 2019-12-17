package com.codesignal.csbot.listeners;

import com.codesignal.csbot.listeners.handlers.*;
import com.codesignal.csbot.listeners.handlers.music.PlayCommandHandler;
import com.codesignal.csbot.listeners.handlers.special.CodeCompileHandler;
import com.codesignal.csbot.models.DiscordMessage;
import com.codesignal.csbot.models.DiscordMessageVersioned;
import com.codesignal.csbot.storage.Storage;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class MessageListener extends ListenerAdapter {
    private static final String COMMAND_PREFIX = System.getenv("COMMAND_PREFIX");
    private static final List<CommandHandler> COMMAND_HANDLERS = List.of(
            new AddSeriesTracker(),
            new GetCSDailyHandler(),
            new GetHiddenTestsHandler(),
            new GetLanguageVersionHandler(),
            new GetTopSubHandler(),
            new MagnetHandler(),
            new PingCommandHandler(),
            new PlayCommandHandler(),
            new SayHandler(),
            new SendMethodMessageHandler(),
            new UndeleteCommandHandler(),
            new WolframAlphaHandler(),
            new YoutubeDownloadHandler()
    );
    private static final CodeCompileHandler codeCompileHandler = new CodeCompileHandler();
    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
    private final Storage storage = new Storage();
    private SpellChecker spellChecker;
    private final Map<String, CommandHandler> commandHandlerMap = new HashMap<>();
    private final long REIMU_DM_CHANNEL = 637927984807804928L;
    private final boolean isProd;

    public MessageListener() {
        isProd = "prod".equals(System.getenv("ENV"));
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

        // Saves message into the database.
        String content = event.getMessage().getContentDisplay();

        if (!event.getMessage().getAttachments().isEmpty()) {
            content += "\n" + event.getMessage().getAttachments().stream().map(
                    att -> "Attachment: " + att.getUrl()
            ).collect(Collectors.joining("\n"));
        }

        String logMessage = String.format("[%s] %s: %s",
                event.getChannel().getName(),
                event.getAuthor().getName(),
                content);
        if (isProd && event.getChannelType().equals(ChannelType.PRIVATE)) {
            TextChannel logChannel = event.getJDA().getTextChannelById(REIMU_DM_CHANNEL);
            if (logChannel != null) logChannel.sendMessage(logMessage).queue();
        }
        logger.info(logMessage);

        storage.saveMessage(
                new DiscordMessage(
                        event.getChannel().getIdLong(),
                        event.getMessageIdLong(),
                        com.datastax.driver.core.utils.UUIDs.timeBased(),
                        event.getAuthor().getIdLong(),
                        content
                )
        );

        // If this is a command for the bot, indicated by COMMAND_PREFIX, then pass it over to the
        // appropriate handler, using lucene approximate matching if needed.
        String message = event.getMessage().getContentRaw();
        boolean shouldHandle = event.getAuthor().getIdLong() == 165451189041758209L || event.getChannelType().isGuild();
        if (shouldHandle && message.startsWith(COMMAND_PREFIX)) {
            final String coreMessage = message.substring(COMMAND_PREFIX.length());
            StringTokenizer tokens = new StringTokenizer(coreMessage);

            if (tokens.countTokens() > 0) {
                String command = tokens.nextToken();
                String oldCommand = command;
                String params = coreMessage.substring(command.length()).strip();
                try {
                    if (!commandHandlerMap.containsKey(command)) {
                        String[] similarCommands = spellChecker.suggestSimilar(command, 1);
                        if (similarCommands.length > 0) {
                            command = similarCommands[0];
                            if (!command.contains("?")) {
                                event.getChannel().sendMessage(
                                        String.format("*Assuming `%s%s` is the British spelling of `%s%s`*",
                                                COMMAND_PREFIX, oldCommand, COMMAND_PREFIX, command)
                                ).queue();
                            }
                        } else {
                            command = null;
                        }
                    }
                    if (command != null) {
                        try {
                            commandHandlerMap.get(command).onMessageReceived(
                                    event, new BotCommand()
                                            .withRawCommandName(oldCommand)
                                            .withCommandName(command)
                                            .withCommandParams(params));
                            return;
                        } catch (ArgumentParserException exp) {
                            // Since we already printed out the error to discord, do nothing here.
                        }
                    }
                } catch (IOException exception) {
                    logger.error("IOException occurred in lucene spellchecker");
                }
            }
        }

        codeCompileHandler.onMessageReceived(event);
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

        String logMessage = String.format("[update][%s] %s: %s",
                event.getChannel().getName(),
                event.getAuthor().getName(),
                event.getMessage());
        if (isProd && event.getChannelType().equals(ChannelType.PRIVATE)) {
            TextChannel logChannel = event.getJDA().getTextChannelById(REIMU_DM_CHANNEL);
            if (logChannel != null) logChannel.sendMessage(logMessage).queue();
        }
        logger.info(logMessage);

        storage.saveVersionedMessage(
                new DiscordMessageVersioned(
                        event.getChannel().getIdLong(),
                        event.getMessageIdLong(),
                        com.datastax.driver.core.utils.UUIDs.timeBased(),
                        event.getAuthor().getIdLong(),
                        content
                )
        );

        codeCompileHandler.onMessageUpdate(event);
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
                        "[DELETED]"
                )
        );
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        codeCompileHandler.onMessageReactionAdd(event);
    }
}
