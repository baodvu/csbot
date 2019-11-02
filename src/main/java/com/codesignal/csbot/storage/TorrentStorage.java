package com.codesignal.csbot.storage;

import com.codesignal.csbot.models.SeriesTracker;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.util.List;


public class TorrentStorage {
    private final CassandraOperations cassandraOps;

    public TorrentStorage() {
        String CONTACT_POINTS = System.getenv("CASSANDRA_CONTACT_POINTS");

        Cluster cluster = Cluster.builder().addContactPoints(CONTACT_POINTS).build();
        Session session = cluster.connect("torrent");

        cassandraOps = new CassandraTemplate(session);
    }

    public void saveTracker(SeriesTracker tracker) {
        cassandraOps.insert(tracker);
    }

    public List<SeriesTracker> getAllTrackers() {
        return cassandraOps.select(QueryBuilder.select().from("series_tracker"), SeriesTracker.class);
    }

    public void removeTracker(SeriesTracker tracker) {
        cassandraOps.delete(tracker);
    }
}
