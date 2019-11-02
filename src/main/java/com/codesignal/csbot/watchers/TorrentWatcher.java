package com.codesignal.csbot.watchers;

import com.codesignal.csbot.models.SeriesTracker;
import com.codesignal.csbot.storage.TorrentStorage;
import com.codesignal.csbot.utils.Deluge;
import com.codesignal.csbot.utils.Torrent;
import com.codesignal.csbot.utils.TorrentSearch;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class TorrentWatcher {
    private final TorrentStorage storage;

    public TorrentWatcher() {
        storage = new TorrentStorage();
    }

    void run(JDA discordClient) {
        TextChannel channel = discordClient.getTextChannelById(603056339710640155L);
        if (channel == null) throw new RuntimeException("Channel reimu-beta does not exist");

        List<SeriesTracker> trackers = storage.getAllTrackers();
        for (SeriesTracker tracker: trackers) {
            try {
                List<Torrent> torrents = TorrentSearch.getTorrentsWithFilters(
                        (tracker.getQuery() + " " + tracker.getEpisode()).strip(),
                        tracker.getSource(),
                        tracker.getMinAge(),
                        tracker.getMaxAge(),
                        tracker.getMinSize(),
                        tracker.getMaxSize(),
                        tracker.getMinSeeds(),
                        tracker.getMaxSeeds(),
                        tracker.getMinPeers(),
                        tracker.getMaxPeers()
                );
                if (!torrents.isEmpty()) {
                    Torrent torrent = torrents.get(0);
                    Deluge delugeClient = new Deluge();
                    String delugeResponse = delugeClient.addMagnet(torrent.getMagnetLink());
                    channel.sendMessage("Sending to Deluge: `" + torrent.getName()
                            + "`\n" + "Response: `" + delugeResponse + "`").queue();
                    if (delugeResponse.contains("\"error\": null")) {
                        storage.removeTracker(tracker);
                    }
                }
            } catch (Exception exception) {
                channel.sendMessage(
                    "Error when checking torrents: " + exception.getMessage()).queue();
            }
        }
    }
}
