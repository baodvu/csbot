package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.adapters.codesignal.message.SubmitTaskAnswerMessage;
import com.codesignal.csbot.adapters.codesignal.message.challengeservice.GetDetailsMessage;
import com.codesignal.csbot.adapters.codesignal.message.task.GetSampleTestsMessage;
import com.codesignal.csbot.utils.Confucius;
import com.codesignal.csbot.wss.CSWebSocket;
import com.codesignal.csbot.wss.CSWebSocketImpl;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class GetHiddenTestsHandler extends CodeSignalCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(GetHiddenTestsHandler.class);
    private static List<String> names = List.of("cshidden", "cs-hidden", "csh");

    public List<String> getNames() {
        return names;
    }

    public String getShortDescription() {
        return "[mod-only] Reveal hidden tests on codesignal";
    }

    public GetHiddenTestsHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("-cid", "--challenge-id")
                .help("Challenge ID").required(true);
        parser.addArgument("-t", "--test-number")
                .type(Integer.class)
                .setDefault(0)
                .help("The index of the hidden test");
        parser.addArgument("-b", "--batch-size")
                .type(Integer.class)
                .setDefault(5)
                .help("Number of chars to recover in a batch");
        parser.addArgument("-d", "--delay")
                .type(Integer.class)
                .setDefault(0)
                .help("A delay (in seconds) is needed as CodeSignal rate-limits");
    }

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            // Only admins can do dis.
            event.getChannel().sendMessage(
                    String.format("As Confucius once said:\n> %s",
                            new Confucius().getRandomSaying(true, false))
            ).queue();
            return;
        }
        Namespace ns = this.parseArgs(event);

        final String challengeId;
        if (ns.getString("challenge-id") == null) {
            try {
                challengeId = getDailyChallengeId().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return;
            }
        } else {
            challengeId = ns.getString("challenge-id");
        }
        int testNumber = ns.getInt("test_number");
        int batchSize = ns.getInt("batch_size");
        // Here we divided by MAX_CONCURRENT so delay would be the time it would take us back to
        // using the same account again.
        int delay = ns.getInt("delay") * 1000 / CSWebSocketImpl.MAX_CONCURRENT;
        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        csClient.send(
                new GetDetailsMessage(challengeId),
                detailsResult -> {
                    JsonNode challenge = detailsResult.getResult().get("challenge");
                    String challengeType = challenge.get("type").textValue();
                    if (challengeType.equals("shortestSolution")) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setAuthor(String.format("%s", challenge.get("name").textValue()), null,
                                "https://cdn.discordapp.com/emojis/580592986136641536.png");
                        eb.addField("Task ID", String.format("%s", challenge.get("taskId").textValue()), true);
                        eb.addField("Challenge ID", String.format("%s", challenge.get("_id").textValue()), true);
                        eb.addField("Name", String.format("%s", challenge.get("name").textValue()), true);
                        eb.setColor(new Color(0xF4EB41));
                        event.getChannel().sendMessage(eb.build()).queue();
                        Message message =
                                event.getChannel().sendMessage("<a:load:508808716376866826> Hacking into the mainframe...").complete();

                        String taskId = challenge.get("taskId").textValue();
                        csClient.send(new GetSampleTestsMessage(taskId),
                                sampleResult -> {
                                    Thread extractTestThread = new Thread(() ->
                                        extractHiddenTestData(
                                                sampleResult, event, taskId, challengeId, testNumber, delay,
                                                batchSize, message)
                                    );
                                    extractTestThread.start();
                                }
                        );
                    } else {
                        event.getChannel().sendMessage(
                                "This command can only be used on golfing challenges").queue();
                    }
                }
        );
    }

    private void extractHiddenTestData(
            ResultMessage resultMessage,
            MessageReceivedEvent event,
            String taskId,
            String challengeId,
            int testNumber,
            int delay,
            final int batchSize,
            Message message
            )
    {
        ArrayList<CSWebSocket> wss = new ArrayList<>();
        try {
            // 1. Retrieve sample tests
            int sampleCount = 0;
            int hiddenCount = 0;
            JsonNode sampleTests = resultMessage.getResult();
            ObjectMapper objMapper = new ObjectMapper();
            objMapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
            objMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, false);
            for (JsonNode test : sampleTests) {
                if (test.get("isHidden") != null && test.get("isHidden").asBoolean()) {
                    hiddenCount++;
                    continue;
                }
                if (test.get("truncated").asBoolean()) {
                    throw new RuntimeException("I/O is truncated. Current version doesn't support this.");
                }
                sampleCount++;
            }

            message.editMessage("✓ Hacking into the mainframe..." +
                    "\n<a:load:508808716376866826> Checking for hidden tests...").queue();
            String baseMessage = "✓ Hacking into the mainframe..."
                    + String.format("\n✓ Found %s visible and %s hidden tests...", sampleCount, hiddenCount)
                    + "\n<a:load:508808716376866826> Extracting data from hidden test #" + testNumber + ":\n";

            if (testNumber >= hiddenCount || testNumber < 0) {
                event.getChannel().sendMessage("Test number is not valid.").queue();
                return;
            }

            // 2. Construct code that will reverse engineer the hidden test cases.
            int base = sampleCount + 1;  // We can use any number between 0 and sampleCount correct tests
            int neededIteration = (int) Math.ceil(Math.log(128.0) / Math.log(base));

            List<String> code = new ArrayList<>();
            code.add(String.format("visibleIORaw = %s",
                    objMapper.writeValueAsString(objMapper.writeValueAsString(sampleTests))));
            code.add("visibleIO = [(item['input'], item['output']) for item in json.loads(visibleIORaw, " +
                    "object_hook=asTreeOrList) if 'isHidden' not in item or not item['isHidden']]");
            code.add("filteredTestInput = sorted([testInput for testInput in testInputData if all(testInput != " +
                    "visibleInput for visibleInput, visibleOutput in visibleIO)], key=str)");
            code.add("alwaysIncorrectOutput = None");
            code.add("currentTargetIndex = " + testNumber);
            code.add("currentTargetInput = json.dumps(filteredTestInput[currentTargetIndex], separators=(',', ':'))");
            code.add("currentTargetCharacter = currentTargetInput[CHARACTER_TO_CHECK]");
            code.add("currentTargetOrd = ord(currentTargetCharacter)");
            code.add(String.format(
                    "augmented_count = currentTargetOrd / %d**DIGIT_TO_CHECK %% %d",
                    base, base));
            code.add("for inp, out in visibleIO[:int(augmented_count)]:");
            code.add("    if inp == eval(dir()[0]):");
            code.add("        return out");
            code.add("return alwaysIncorrectOutput");
            String compiled = String.join("\n", code);

            log.info("Needed number of iterations for 1 character = " + neededIteration);

            for (int i = 0; i < CSWebSocketImpl.MAX_CONCURRENT; i++)
                wss.add(new CSWebSocketImpl().build());
            int wssIndex = 0;
            // On first send, we will trigger a nice animation.
            boolean firstSend = true;

            StringBuilder recoveredInput = new StringBuilder();
            for (int batchIdx = 0; batchIdx < 50; batchIdx++) {
                AtomicInteger charCount = new AtomicInteger();
                AtomicBoolean failed = new AtomicBoolean(false);
                CountDownLatch batchCountDown = new CountDownLatch(batchSize);

                // Process 5 characters in a batch
                ConcurrentHashMap<Integer, Character> recoveredChars = new ConcurrentHashMap<>();
                for (int processedCharacter = 0; processedCharacter < batchSize; processedCharacter++) {
                    final int finalProcessedCharacter = processedCharacter;
                    CountDownLatch bitsCountDown = new CountDownLatch(neededIteration);
                    ConcurrentHashMap<Integer, Integer> bits = new ConcurrentHashMap<>();
                    for (int i = 0; i < neededIteration; i++) {
                        Thread.sleep(delay);
                        final int idx = i;
                        wss.get(wssIndex++ % wss.size()).send(
                                new SubmitTaskAnswerMessage(
                                        taskId,
                                        challengeId,
                                        compiled
                                                .replace("CHARACTER_TO_CHECK",
                                                        Integer.toString(processedCharacter + batchIdx * batchSize))
                                                .replace("DIGIT_TO_CHECK", Integer.toString(i)),
                                        "py"),
                                submitTaskResponse -> {
                                    bits.put(idx, submitTaskResponse.getResult().get("correctTestCount").intValue());
                                    bitsCountDown.countDown();
                                }
                        );
                        if (firstSend) {
                            firstSend = false;
                            message.editMessage(baseMessage).queue();
                        }
                    }

                    new Thread(() -> {
                        try {
                            boolean cdReturnValue = bitsCountDown.await(30, TimeUnit.SECONDS);
                            if (!cdReturnValue) {
                                // Timeout waiting on bits to come back.
                                failed.set(true);
                                return;
                            }
                            bits.forEach((k, v) -> log.info(k + ": " + v));

                            // Reconstruct the character from bits
                            int characterOrd = 0;
                            int power = 1;
                            for (int i = 0; i < neededIteration; i++) {
                                characterOrd += bits.get(i) * power;
                                power *= base;
                            }

                            // Terminal characters don't count towards char count
                            if (characterOrd != 0) {
                                charCount.getAndIncrement();
                                char character = Character.toChars(characterOrd)[0];
                                log.info("Recovered character: " + character);
                                recoveredChars.put(finalProcessedCharacter, character);
                            }
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                            failed.set(true);
                        } finally {
                            batchCountDown.countDown();
                        }
                    }).start();
                }

                batchCountDown.await();
                if (failed.get()) {
                    message.editMessage(
                            "Encountered error. Make sure that you specify long enough delay.").queue();
                    return;
                }

                for (int i = 0; i < charCount.get(); i++) {
                    recoveredInput.append(recoveredChars.get(i) == null ? '?' : recoveredChars.get(i));
                }

                if (charCount.get() < batchSize) {
                    message.editMessage(
                            "✓ Hacking complete. The input is `" + recoveredInput + "`").queue();
                    log.info("hacking complete");
                    return;
                }
                message.editMessage(baseMessage + "`" + recoveredInput + "`").queue();
            }
            message.editMessage("The input is just too long. Here's what " +
                    "we've uncovered so far: " + recoveredInput).queue();
            log.info("Too many chars! Input: " + recoveredInput);
        } catch (Exception exception) {
            exception.printStackTrace();
            message.editMessage("Encountered unexpected exception. Make sure that you specify " +
                    "long enough delay").queue();
        } finally {
            for (CSWebSocket webSocket: wss)
                webSocket.close();
        }
    }
}
