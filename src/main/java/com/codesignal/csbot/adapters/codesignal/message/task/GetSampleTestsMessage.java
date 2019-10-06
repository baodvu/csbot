package com.codesignal.csbot.adapters.codesignal.message.task;

import com.codesignal.csbot.adapters.codesignal.message.MethodMessage;

import java.util.List;


public class GetSampleTestsMessage extends MethodMessage {
    public GetSampleTestsMessage(String taskId) {
        super("taskSolveService.getSampleTests", List.of(
            taskId, "challenge"
        ));
    }
}
