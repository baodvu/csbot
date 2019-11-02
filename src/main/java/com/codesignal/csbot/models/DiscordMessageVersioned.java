package com.codesignal.csbot.models;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;
import java.util.UUID;


@Table(value = "discord_message_versioned")
public class DiscordMessageVersioned implements Serializable {

    @PrimaryKeyColumn(name = "created_at", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private final UUID createdAt;

    @PrimaryKeyColumn(name = "message_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private final long messageId;

    @Column(value = "author_id")
    private final long authorId;

    private final String content;

    public DiscordMessageVersioned(long channelId, long messageId, UUID createdAt, long authorId, String content) {
        this.createdAt = createdAt;
        this.messageId = messageId;
        this.authorId = authorId;
        this.content = content;
    }

    public long getMessageId() {
        return messageId;
    }

    public UUID getCreatedAt() {
        return createdAt;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }
}
