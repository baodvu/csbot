package com.codesignal.csbot.adapters.codesignal.message.challengeservice;

import com.codesignal.csbot.adapters.codesignal.message.MethodMessage;

import java.util.List;

public class GetDetailsMessage extends MethodMessage {
    public GetDetailsMessage(String challengeId) {
        super("challengeService.getDetails", List.of(challengeId, ""));
    }
}
