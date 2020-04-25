package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.adapters.codesignal.message.challenges.GetDashboard;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

abstract class CodeSignalCommandHandler extends AbstractCommandHandler {
     Future<String> getDailyChallengeId() {
        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        Message message = new GetDashboard("public", 1);
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        csClient.send(message, (ResultMessage resultMessage) ->
            completableFuture.complete(
                    resultMessage.getResult().at("/challenges/0/challengeDoc/_id").textValue())
        );

        return completableFuture;
    }
}
