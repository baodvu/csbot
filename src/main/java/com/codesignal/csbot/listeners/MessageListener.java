package com.codesignal.csbot.listeners;

import com.codesignal.csbot.models.DiscordMessage;
import com.codesignal.csbot.models.DiscordMessageVersioned;
import com.codesignal.csbot.storage.Storage;
import com.datastax.driver.core.utils.UUIDs;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MessageListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
    private Storage storage;

    public MessageListener() {
        this.storage = new Storage();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        if (event.isFromType(ChannelType.PRIVATE)) {
            logger.info("[PM] {}: {}",
                    event.getAuthor().getName(),
                    event.getMessage().getContentDisplay());
        } else {
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

        if (event.getMessage().getContentRaw().startsWith("git rekt")) {
            logger.info("Looking for edited messages in the last hour");
            TextChannel channel = event.getTextChannel();
            channel.sendMessage("Here are the edited messages within the last hour:").queue();

            int message_count = 0;
            int character_count = 0;

            List<String> buffer = new ArrayList<>();

            for (DiscordMessageVersioned message : storage.getEditedMessagesFromLastHour(event.getChannel().getIdLong())) {
                User author = event.getJDA().getUserById(message.getAuthorId());
                long unix_timestamp = UUIDs.unixTimestamp(message.getCreatedAt());

                String s = String.format(
                        "`%s` `[%d/%s]`: %s",
                        author != null ? author.getName() : "",
                        message.getMessageId(),
                        new Date(unix_timestamp).toString(),
                        message.getContent()
                );

                if (character_count + s.length() > 2000) {
                    logger.info(String.join("\n", buffer));
                    logger.info("{}", character_count + s.length() + 2 * buffer.size());
                    channel.sendMessage(String.join("\n", buffer)).queue();
                    buffer.clear();
                    character_count = 0;
                }

                buffer.add(s);
                character_count += s.length() + 2;
                message_count++;

                if (message_count >= 200) {
                    channel.sendMessage(String.join("\n", buffer)).queue();
                    buffer.clear();
                    channel.sendMessage("Too many messages bro").queue();
                    break;
                }
            }

            if (!buffer.isEmpty()) {
                channel.sendMessage(String.join("\n", buffer)).queue();
                buffer.clear();
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
