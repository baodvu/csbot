package com.codesignal.csbot.storage;

import com.codesignal.csbot.models.DiscordMessage;
import com.codesignal.csbot.models.DiscordMessageVersioned;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Storage {
    private static final Logger logger = LoggerFactory.getLogger(Storage.class);
    private CassandraOperations cassandraOps;

    public Storage() {
        String CONTACT_POINTS = System.getenv("CASSANDRA_CONTACT_POINTS");
        Cluster cluster = Cluster.builder().addContactPoints(CONTACT_POINTS).build();
        Session session = cluster.connect("codesignal");

        cassandraOps = new CassandraTemplate(session);
    }

    public void saveMessage(DiscordMessage message) {
        cassandraOps.insert(message);
    }

    public void saveVersionedMessage(DiscordMessageVersioned message) {
        cassandraOps.insert(message);
    }

    public DiscordMessage getMessage(long channel_id, long message_id) {
        Select s = QueryBuilder.select().from("discord_message");
        s.where(QueryBuilder.eq("channel_id", channel_id))
                .and(QueryBuilder.eq("message_id", message_id));

        return cassandraOps.selectOne(s, DiscordMessage.class);
    }

    public List<DiscordMessageVersioned> getEditedMessagesFromLastHour(long channel_id) {
        UUID last_hour = UUIDs.startOf(new Date(System.currentTimeMillis() - 3600 * 1000).getTime());

        Select s = QueryBuilder.select().from("discord_message_versioned");
        s.where(QueryBuilder.eq("channel_id", channel_id))
                .and(QueryBuilder.gte("created_at", last_hour));

        return cassandraOps.select(s, DiscordMessageVersioned.class);
    }
}
