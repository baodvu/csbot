package com.codesignal.csbot.models;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;
import java.time.Instant;


@Table(value = "notification")
public class Notification implements Serializable {
    @PrimaryKey(value = "entity_id")
    private final String entityId;

    @Column(value = "created_at")
    private final Instant createdAt;

    public Notification(String entityId, Instant createdAt) {
        this.entityId = entityId;
        this.createdAt = createdAt;
    }

    public String getEntityId() {
        return entityId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
