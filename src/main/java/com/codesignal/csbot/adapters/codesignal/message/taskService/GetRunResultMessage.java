package com.codesignal.csbot.adapters.codesignal.message.taskService;

import com.codesignal.csbot.adapters.codesignal.message.MethodMessage;

import java.util.List;
import java.util.Map;

public class GetRunResultMessage extends MethodMessage {
    public GetRunResultMessage(String taskId, String sourceCode, String language, String mode, boolean runHidden) {
        super("taskService.getRunResult",
                List.of(
                        Map.of(
                                "taskId", taskId,
                                "mode", mode,
                                "customTests", List.of(),
                                "runHidden", runHidden,
                                "submission", Map.of(
                                        "sources", List.of(
                                                Map.of(
                                                        "source", sourceCode,
                                                        "language", language,
                                                        "path", "main." + language
                                                )
                                        ),
                                        "recordingKey", taskId
                                )
                        )
                )
        );
    }
}
