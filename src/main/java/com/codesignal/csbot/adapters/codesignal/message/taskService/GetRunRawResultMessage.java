package com.codesignal.csbot.adapters.codesignal.message.taskService;

import com.codesignal.csbot.adapters.codesignal.message.MethodMessage;

import java.util.List;
import java.util.Map;

public class GetRunRawResultMessage extends MethodMessage {
    public GetRunRawResultMessage(String sourceCode, String language) {
        super("taskService.getRunRawResult",
                List.of(
                        Map.of(
                                "taskId", "jwr339Kq6e3LQTsfa",
                                "submission", Map.of(
                                        "sources", List.of(
                                                Map.of(
                                                        "source", sourceCode,
                                                        "language", language,
                                                        "path", "main." + language
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
