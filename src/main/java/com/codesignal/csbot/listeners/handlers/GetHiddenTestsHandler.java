package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.adapters.codesignal.message.SubmitTaskAnswerMessage;
import com.codesignal.csbot.adapters.codesignal.message.challengeservice.GetDetailsMessage;
import com.codesignal.csbot.adapters.codesignal.message.task.GetSampleTestsMessage;
import com.codesignal.csbot.utils.Randomizer;
import com.codesignal.csbot.wss.CSWebSocket;
import com.codesignal.csbot.wss.CSWebSocketImpl;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class GetHiddenTestsHandler extends AbstractCommandHandler {
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
                .help("The index of the hidden test (default 0)");
        parser.addArgument("-d", "--delay")
                .type(Integer.class)
                .setDefault(10)
                .help("A delay (in seconds) is needed as CodeSignal rate-limits (default 10)");
    }

    @SuppressWarnings("unchecked")
    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        if (!event.getAuthor().getName().equals("ephemeraldream") && (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR))) {
            // Only admins can do dis.
            event.getChannel().sendMessage("hack it yourself, bai.").queue();
            return;
        }
        Namespace ns = this.parseArgs(event);

        String challengeId = ns.getString("challenge_id");
        int testNumber = ns.getInt("test_number");
        int delay = ns.getInt("delay") * 1000;
        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        csClient.send(
                new GetDetailsMessage(challengeId),
                detailsResult -> {
                    Map<Object, Object> result = (Map<Object, Object>) detailsResult.getResult();
                    Map<Object, Object> challenge = (Map<Object, Object>) result.get("challenge");
                    String challengeType = (String) challenge.get("type");
                    if (challengeType.equals("shortestSolution")) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Challenge", null);
                        eb.addField("Task ID", String.format("%s", challenge.get("taskId")), true);
                        eb.addField("Challenge ID", String.format("%s", challenge.get("_id")), true);
                        eb.addField("Name", String.format("%s", challenge.get("name")), true);
                        eb.setColor(new Color(0xF4EB41));
                        event.getChannel().sendMessage(eb.build()).queue();

                        String taskId = (String) challenge.get("taskId");
                        csClient.send(new GetSampleTestsMessage(taskId),
                                sampleResult -> extractHiddenTestData(
                                        sampleResult, event, taskId, challengeId, testNumber, delay)
                        );
                    } else {
                        event.getChannel().sendMessage(
                                "This command can only be used on golfing challenges").queue();
                    }
                }
        );
    }

    @SuppressWarnings("unchecked")
    private void extractHiddenTestData(
            ResultMessage resultMessage,
            MessageReceivedEvent event,
            String taskId,
            String challengeId,
            int testNumber,
            int delay
            )
    {
        Message message =
                event.getChannel().sendMessage("Hacking into the mainframe...").complete();
        ArrayList<CSWebSocket> wss = new ArrayList<>();
        try {
            // 1. Retrieve sample tests
            int sampleCount = 0;
            int hiddenCount = 0;
            Map<Object, Object> inputToOutput = new LinkedHashMap<>();
            List<Map<Object, Object>> sampleTests = (List<Map<Object, Object>>) resultMessage.getResult();
            ObjectMapper objMapper = new ObjectMapper();
            objMapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
            objMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, false);
            for (Map<Object, Object> test : sampleTests) {
                if (test.get("isHidden") != null && (boolean) test.get("isHidden")) {
                    hiddenCount++;
                    continue;
                }
                if ((boolean) test.get("truncated")) {
                    throw new RuntimeException("I/O is truncated. Current version doesn't support this.");
                }
                sampleCount++;
            }
            String baseMessage = message.getContentRaw()
                    + String.format("\nFound %s visible and %s hidden tests...", sampleCount, hiddenCount)
                    + "\nExtracting data from hidden test #" + testNumber + "...";
            message.editMessage(baseMessage).queue();

            if (testNumber >= hiddenCount || testNumber < 0) {
                event.getChannel().sendMessage("Test number is not valid.").queue();
                return;
            }

            // 2. Construct code that will reverse engineer the hidden test cases.
            int base = sampleCount + 1;  // We can use any number between 0 and sampleCount correct tests
            int neededIteration = (int) Math.ceil(Math.log(128.0) / Math.log((double) base));

            List<String> code = new ArrayList<>();
            code.add(String.format("visibleIORaw = %s",
                    objMapper.writeValueAsString(objMapper.writeValueAsString(sampleTests))));
            code.add("visibleIO = [(item['input'], item['output']) for item in json.loads(visibleIORaw, " +
                    "object_hook=asTreeOrList) if 'isHidden' not in item or not item['isHidden']]");
            code.add("filteredTestInput = sorted([testInput for testInput in testInputData if all(testInput != " +
                    "visibleInput for visibleInput, visibleOutput in visibleIO)], key=str)");
            code.add(String.format("alwaysIncorrectOutput = \"%s\"",
                    Randomizer.getAlphaNumericString(20)));
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

            for (int i = 0; i < neededIteration; i++)
                wss.add(new CSWebSocketImpl().build());

            StringBuilder recoveredInput = new StringBuilder();
            for (int processed_character = 0; processed_character < 50; processed_character++) {
                CountDownLatch countDownLatch = new CountDownLatch(neededIteration);
                ConcurrentHashMap<Integer, Integer> bits = new ConcurrentHashMap<>();
                for (int i = 0; i < neededIteration; i++) {
                    final int idx = i;
                    wss.get(i).send(

                            new SubmitTaskAnswerMessage(
                                    taskId,
                                    challengeId,
                                    compiled
                                            .replace("CHARACTER_TO_CHECK",
                                                    Integer.toString(processed_character))
                                            .replace("DIGIT_TO_CHECK", Integer.toString(i)),
                                    "py"),
                            submitTaskResponse -> {
                                Map<Object, Object> result =
                                        (Map<Object, Object>) submitTaskResponse.getResult();
                                bits.put(idx, Integer.parseInt(result.get("correctTestCount").toString()));
                                countDownLatch.countDown();
                            }
                    );
                }

                try {
                    countDownLatch.await(15, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                    message.editMessage("Encountered unexpected exception. Make sure that you specify " +
                            "long enough delay.").queue();
                    log.info("InterruptedException! Input: " + recoveredInput);
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
                if (characterOrd == 0) {
                    message.editMessage("Hacking complete. The input is " + recoveredInput).queue();
                    log.info("hacking complete");
                    return;
                }
                char character = Character.toChars(characterOrd)[0];
                recoveredInput.append(character);
                message.editMessage(baseMessage + "\nRecovered input: " + recoveredInput + "...").queue();
                log.info("Recovered character: " + character);
                Thread.sleep(delay);
            }
            message.editMessage("We ran out of steam, sorry. The input is just too long. Here's what " +
                    "we've recovered so far: " + recoveredInput).queue();
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
