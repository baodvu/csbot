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

import java.util.Date;

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
            logger.info("[{}][{}] {}: {}",
                    event.getGuild().getName(),
                    event.getTextChannel().getName(),
                    event.getMember().getEffectiveName(),
                    event.getMessage().getContentDisplay());
            storage.saveMessage(
                    new DiscordMessage(
                            event.getChannel().getIdLong(),
                            event.getMessageIdLong(),
                            com.datastax.driver.core.utils.UUIDs.timeBased(),
                            event.getAuthor().getIdLong(),
                            event.getMessage().getContentRaw()
                    )
            );
        }

        if (event.getMessage().getContentRaw().startsWith("git rekt")) {
            logger.info("Looking for edited messages in the last hour");
            TextChannel channel = event.getTextChannel();
            channel.sendMessage("Here are the edited messages within the last hour:").queue();

            int count = 0;

            for (DiscordMessageVersioned message : storage.getEditedMessagesFromLastHour(event.getChannel().getIdLong())) {
                User author = event.getJDA().getUserById(message.getAuthorId());
                long unix_timestamp = UUIDs.unixTimestamp(message.getCreatedAt());

                channel.sendMessage(
                        String.format(
                                "`%s` `[%d/%s]`: %s",
                                author.getName(),
                                message.getMessageId(),
                                new Date(unix_timestamp).toString(),
                                message.getContent()
                        )
                ).queue();

                count++;

                if (count >= 20) {
                    channel.sendMessage("Too many messages bro").queue();
                    break;
                }
            }
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        logger.info("[update] message_id: {} | message: {} | channel: {}",
                event.getMessageId(),
                event.getMessage(),
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
                        event.getAuthor().getIdLong(),
                        event.getMessage().getContentRaw()
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
