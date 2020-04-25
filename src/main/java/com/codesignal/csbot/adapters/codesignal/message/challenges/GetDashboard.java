package com.codesignal.csbot.adapters.codesignal.message.challenges;

import com.codesignal.csbot.adapters.codesignal.message.MethodMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetDashboard extends MethodMessage {
    public GetDashboard(String visibility, int page) {
        super("challenges.getDashboard", List.of(
                getChallengeConfig(visibility),
                "",
                page
        ));
    }

    private static Map<String, String> getChallengeConfig(String visibility) {
        Map<String, String> params = new HashMap<>();
        params.put("type", null);
        params.put("status", null);
        params.put("difficulty", null);
        params.put("visibility", visibility);
        return params;
    }
}
