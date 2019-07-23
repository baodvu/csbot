package com.codesignal.csbot.adapters.codesignal.message;

import java.util.List;
import java.util.Map;

public class GetUserFeedMessage extends MethodMessage {
    public GetUserFeedMessage(String tab) {
        super("getUserFeed", List.of(Map.ofEntries(
                Map.entry("type", "challenges"),
                Map.entry("tab", tab),
                Map.entry("difficulty", "all"),
                Map.entry("generalType", "all"),
                Map.entry("offset", 0),
                Map.entry("limit", 1)
        )));
    }
}
