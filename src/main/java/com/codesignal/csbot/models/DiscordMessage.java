package com.codesignal.csbot.models;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;
import java.util.UUID;


@Table(value = "discord_message")
public class DiscordMessage implements Serializable {
    @PrimaryKeyColumn(name = "channel_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private final long channelId;

    @PrimaryKeyColumn(name = "message_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private final long messageId;

    @Column(value = "created_at")
    private final UUID createdAt;

    @Column(value = "author_id")
    private final long authorId;

    private final String content;

    public DiscordMessage(long channelId, long messageId, UUID createdAt, long authorId, String content) {
        this.channelId = channelId;
        this.messageId = messageId;
        this.createdAt = createdAt;
        this.authorId = authorId;
        this.content = content;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public UUID getCreatedAt() {
        return createdAt;
    }
}
