package com.codesignal.csbot.adapters.codesignal.message;

import java.util.List;
import java.util.Map;

/**
 * Run the code on all tests (including hidden).
 *
 * Example:
 *    new SubmitTaskAnswerMessage("DeLmLJNEE8sgsmGyZ", "FhdrdQNXPsToAbs7M","return `eval(dir()[0])`[0] > 'a'", "py")
 */
public class SubmitTaskAnswerMessage extends MethodMessage {
    public SubmitTaskAnswerMessage(String taskId, String challengeId, String sourceCode, String language) {
        super("submitTaskAnswer",
                List.of(
                        Map.of(
                                "sources", List.of(
                                        Map.of(
                                                "source", sourceCode,
                                                "language", language
                                        )
                                ),
                                "recordingKey", taskId
                        ),
                        Map.of(
                                "challengeId", challengeId,
                                "language", language,
                                "userSource", sourceCode
                        )
                )
        );
    }
}
