package com.codesignal.csbot.utils;

import com.codesignal.csbot.models.SeriesTracker;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TorrentSearch {
    private static final String site = StringUtils.toEncodedString(Base64.getDecoder().decode(
            "aHR0cDovL2JpdGxvcmRzZWFyY2guY29t"), StandardCharsets.UTF_8);

    public static List<Torrent> getTorrentsWithFilters(
            String searchTerms,
            @Nullable String source,
            long minAge,
            long maxAge,
            long minSize,
            long maxSize,
            long minSeeds,
            long maxSeeds,
            long minPeers,
            long maxPeers
    ) throws UnirestException {
        List<Torrent> torrents = TorrentSearch.getTorrents(searchTerms, 10);
        List<Torrent> filtered = new ArrayList<>();
        for (Torrent torrent : torrents) {
            if (source != null && !torrent.getSource().toLowerCase().equals(source.toLowerCase())) continue;
            long currentAge = System.currentTimeMillis() / 1000 - torrent.getCreatedAt();
            if (minAge > currentAge) continue;
            if (maxAge < currentAge) continue;
            if (minSize > torrent.getSizeMBs()) continue;
            if (maxSize < torrent.getSizeMBs()) continue;
            if (minSeeds > torrent.getSeeds()) continue;
            if (maxSeeds < torrent.getSeeds()) continue;
            if (minPeers > torrent.getPeers()) continue;
            if (maxPeers < torrent.getPeers()) continue;
            filtered.add(torrent);
        }
        return filtered;
    }

    public static List<Torrent> getTorrents(String searchTerms, int limit) throws UnirestException {
        JSONArray content = Unirest.post(site + "/get_list")
                .header("accept", "application/json")
                .header("cache-control", "no-cache")
                .header("content_type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("x-request-token", TorrentSearch.getAuthToken(searchTerms))
                .header("x-requested-with", "XMLHttpRequest")
                .field("query", searchTerms.replace(" ", "+"))
                .field("offset", 0)
                .field("limit", limit)
                .field("filters[field]", "seeds")  // sort by seeds
                .field("filters[sort]", "desc")  // in desc order
                .field("filters[time]", 4)  // for all times
                .field("filters[category]", "all")  // for all categories
                .field("filters[adult]", "false")
                .field("filters[risky]", "false")
                .asJson().getBody().getObject().getJSONArray("content");
        List<Torrent> torrents = new ArrayList<>();
        for (int i = 0; i < content.length(); ++i) {
            JSONObject result = content.getJSONObject(i);
            torrents.add(new Torrent(
                    result.getString("name"),
                    result.getString("magnet"),
                    result.getString("source"),
                    result.getLong("age"),
                    Long.parseLong(result.getString("size")),
                    result.getLong("seeds"),
                    result.getLong("peers")
            ));
        }
        return torrents;
    }

    private static String getAuthToken(String searchTerms) throws UnirestException {
        String token = Pattern.compile("([A-Za-z0-9+/_-]+)';</script>")
                .matcher(Unirest.get(site + "/search")
                        .queryString("query", searchTerms.replace(" ", "+"))
                        .asString().getBody())
                .results()
                .map(matchResult -> matchResult.group(1))
                .collect(Collectors.joining());
        if (token.length() < 40 || token.length() > 100) {
            throw new RuntimeException("Token from bl seems invalid");
        }
        return token;
    }
}

