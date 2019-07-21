package com.codesignal.csbot.adapters.codesignal.message.taskleaderboardservice;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.MethodMessage;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.adapters.codesignal.message.challengeservice.GetDetailsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


public class GetSubmissionMessage extends MethodMessage {
    private static final Logger log = LoggerFactory.getLogger(GetSubmissionMessage.class);
    private volatile String taskId;
    private final CountDownLatch countDownLatch;

    @SuppressWarnings("unchecked")
    public GetSubmissionMessage(String challengeId, String lang) {
        super("taskLeaderboardService.getSubmissions");
        countDownLatch = new CountDownLatch(1);

        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        Message message = new GetDetailsMessage(challengeId);
        csClient.send(message, (ResultMessage resultMessage) -> {
            LinkedHashMap<Object, Object> result = (LinkedHashMap<Object, Object>) resultMessage.getResult();
            LinkedHashMap<Object, Object> task = (LinkedHashMap<Object, Object>) result.get("task");

            log.info("Found task " + task.get("_id"));
            taskId = (String) task.get("_id");
            countDownLatch.countDown();
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException exp) {
            // pass
        }

        super.setParams(List.of(
                Map.of(
                        "context", Map.of("mode", "challenge", "challengeId", challengeId),
                        "taskId", taskId,
                        "language", lang
                ),
                Map.of(
                        "skip", 0,
                        "sort", List.of("chars", "asc")
                )
        ));
    }
}
