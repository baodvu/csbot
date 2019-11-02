package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.listeners.BotCommand;
import com.codesignal.csbot.models.SeriesTracker;
import com.codesignal.csbot.storage.TorrentStorage;
import com.codesignal.csbot.utils.Torrent;
import com.codesignal.csbot.utils.TorrentSearch;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class AddSeriesTracker extends AbstractCommandHandler{
    private static final List<String> names = List.of("track");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Track a series or something."; }

    private final TorrentStorage storage;

    public AddSeriesTracker() {
        storage = new TorrentStorage();
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("-e", "--episodes")
                .type(String.class)
                .help("Episodes to track (commas and dashes allowed e.g. 1,4-7,12-14)");
        parser.addArgument("-s", "--source")
                .type(String.class)
                .help("Preferred source");
        parser.addArgument("--min_age")
                .type(Long.class)
                .setDefault(0L)
                .help("Minimum age in seconds");
        parser.addArgument("--max_age")
                .type(Long.class)
                .setDefault(Long.MAX_VALUE)
                .help("Maximum age in seconds");
        parser.addArgument("--min_size")
                .type(Long.class)
                .setDefault(0L)
                .help("Minimum size in MBs");
        parser.addArgument("--max_size")
                .type(Long.class)
                .setDefault(3000L)
                .help("Maximum size in MBs");
        parser.addArgument("--min_seeds")
                .type(Long.class)
                .setDefault(1L)
                .help("Minimum seed count");
        parser.addArgument("--max_seeds")
                .type(Long.class)
                .setDefault(Long.MAX_VALUE)
                .help("Maximum seed count");
        parser.addArgument("--min_peers")
                .type(Long.class)
                .setDefault(0L)
                .help("Minimum peer count");
        parser.addArgument("--max_peers")
                .type(Long.class)
                .setDefault(Long.MAX_VALUE)
                .help("Maximum peer count");
        parser.addArgument("terms").nargs("*").required(true)
                .help("Terms to search for");
    }

    public void onMessageReceived(MessageReceivedEvent event, BotCommand botCommand) throws ArgumentParserException {
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            // Only admins can do dis.
            return;
        }
        Namespace ns = this.parseArgs(event);

        String terms = String.join(" ", ns.getList("terms"));
        List<Integer> episodes = this.parseEpisodes(ns.getString("episodes"));
        String source = ns.getString("source");
        long minAge = ns.getLong("min_age");
        long maxAge = ns.getLong("max_age");
        long minSize = ns.getLong("min_size");
        long maxSize = ns.getLong("max_size");
        long minSeeds = ns.getLong("min_seeds");
        long maxSeeds = ns.getLong("max_seeds");
        long minPeers = ns.getLong("min_peers");
        long maxPeers = ns.getLong("max_peers");

        try {
            List<Torrent> torrents = TorrentSearch.getTorrents(terms, 5);
            event.getChannel().sendMessage("Sample feed: ```\n" + torrents.stream().map(Torrent::toString)
                    .collect(Collectors.joining("\n")) + "```").queue();
        } catch (UnirestException e){
            event.getChannel().sendMessage("Can't parse response").queue();
        }

        if (episodes == null) {
            episodes = new ArrayList<>();
            episodes.add(null);  // so that for loop will run once
        }

        for (Integer episode : episodes) {
            storage.saveTracker(new SeriesTracker(
                    event.getAuthor().getIdLong(),
                    terms,
                    episode == null ? "" : StringUtils.leftPad(episode.toString(), 2, "0"),
                    source,
                    minAge,
                    maxAge,
                    minSize,
                    maxSize,
                    minSeeds,
                    maxSeeds,
                    minPeers,
                    maxPeers,
                    System.currentTimeMillis()
            ));
        }
    }

    private List<Integer> parseEpisodes(String episodesRawString) {
        if (StringUtils.isEmpty(episodesRawString)) {
            return null;
        }

        String[] parts = episodesRawString.split(",");
        List<Integer> episodes = new ArrayList<>();
        Set<Integer> added = new HashSet<>();
        try {
            for (String s: parts) {
                String part = s.replaceAll(" ", "");
                if (part.contains("-")) {
                    String[] epLimits = part.split("-");
                    int start = Integer.parseInt(epLimits[0]);
                    int end = Integer.parseInt(epLimits[1]);
                    if (start > 99 || start < 0 || end > 99 || end < 0) {
                        throw new IllegalArgumentException("Invalid episode numbers");
                    }
                    for (int j = start; j <= end; ++j) {
                        if (!added.contains(j)) {
                            episodes.add(j);
                            added.add(j);
                        }
                    }
                } else {
                    int ep = Integer.parseInt(part);
                    if (!added.contains(ep)) {
                        episodes.add(ep);
                        added.add(ep);
                    }
                }
            }
        } catch (Exception exp) {
            throw new IllegalArgumentException("Invalid episode numbers");
        }
        return episodes;
    }
}

