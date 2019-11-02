package com.codesignal.csbot.utils;


import org.apache.commons.lang3.StringUtils;

public class Torrent {
    private final String name;
    private final String magnetLink;
    private final String source;
    private final Long createdAt;
    private final Long sizeMBs;
    private final Long seeds;
    private final Long peers;

    public Torrent(String name, String magnetLink, String source, Long createdAt, Long sizeMBs, Long seeds, Long peers) {
        this.name = name;
        this.magnetLink = magnetLink;
        this.source = source;
        this.createdAt = createdAt;
        this.sizeMBs = sizeMBs;
        this.seeds = seeds;
        this.peers = peers;
    }

    public String getName() {
        return name;
    }

    public String getMagnetLink() {
        return magnetLink;
    }

    public String getSource() {
        return source;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getSizeMBs() {
        return sizeMBs;
    }

    public Long getSeeds() {
        return seeds;
    }

    public Long getPeers() {
        return peers;
    }

    @Override
    public String toString() {
        return name +
                " magnetLink='" + StringUtils.abbreviate(magnetLink, 50) + '\'' +
                " source='" + source + '\'' +
                " seeds=" + seeds +
                " peers=" + peers;
    }
}
