package com.codesignal.csbot.adapters.codesignal.message;

import java.util.List;
import java.util.Map;

public class GetUserFeedMessage extends MethodMessage {
    public GetUserFeedMessage(String tab, int offset, int limit) {
        super("getUserFeed", List.of(Map.ofEntries(
                Map.entry("type", "challenges"),
                Map.entry("tab", tab),
                Map.entry("difficulty", "all"),
                Map.entry("generalType", "all"),
                Map.entry("offset", offset),
                Map.entry("limit", limit)
        )));
    }
}
