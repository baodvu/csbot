package com.codesignal.csbot.models;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;


@Table(value = "series_tracker")
public class SeriesTracker {
    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private final long userId;

    @PrimaryKeyColumn(name = "query", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private final String query;

    @Column(value = "episode")
    private final String episode;

    @Column(value = "source")
    private final String source;

    @Column(value = "min_age")
    private final long minAge;

    @Column(value = "max_age")
    private final long maxAge;

    @Column(value = "min_size")
    private final long minSize;

    @Column(value = "max_size")
    private final long maxSize;

    @Column(value = "min_seeds")
    private final long minSeeds;

    @Column(value = "max_seeds")
    private final long maxSeeds;

    @Column(value = "min_peers")
    private final long minPeers;

    @Column(value = "max_peers")
    private final long maxPeers;

    @Column(value = "created_at")
    private final long createdAt;

    public SeriesTracker(
            long userId, String query, String episode, String source, long minAge, long maxAge, long minSize,
            long maxSize, long minSeeds, long maxSeeds, long minPeers, long maxPeers, long createdAt) {
        this.userId = userId;
        this.query = query;
        this.episode = episode;
        this.source = source;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.minSeeds = minSeeds;
        this.maxSeeds = maxSeeds;
        this.minPeers = minPeers;
        this.maxPeers = maxPeers;
        this.createdAt = createdAt;
    }

    public long getUserId() {
        return userId;
    }

    public String getQuery() {
        return query;
    }

    public String getEpisode() {
        return episode;
    }

    public String getSource() {
        return source;
    }

    public long getMinAge() {
        return minAge;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public long getMinSize() {
        return minSize;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public long getMinSeeds() {
        return minSeeds;
    }

    public long getMaxSeeds() {
        return maxSeeds;
    }

    public long getMinPeers() {
        return minPeers;
    }

    public long getMaxPeers() {
        return maxPeers;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
