package com.codesignal.csbot.watchers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.GetUserFeedMessage;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.adapters.codesignal.message.challengeservice.GetDetailsMessage;
import com.codesignal.csbot.adapters.codesignal.message.userservice.GetMultipleWithVisibleFieldsMessage;
import com.codesignal.csbot.models.Notification;
import com.codesignal.csbot.storage.Storage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


class ChallengeWatcher {
    private static final Logger log = LoggerFactory.getLogger(ChallengeWatcher.class);

    // Reduce calls to database by caching processed ids.
    private static Set<String> notifiedChallengeIds = new HashSet<>();

    // Cassandra database.
    private Storage storage = new Storage();
    private CodesignalClient csClient = CodesignalClientSingleton.getInstance();

    // If the challenge is too old we shouldn't dig it up.
    private final int MAX_LOOKBACK_TIME_IN_MS = 1000 * 60 * 60;  // 1 hour

    // Setting
    private String tab;
    private Color color;
    private String notificationTag;

    ChallengeWatcher(String tab, Color color, String notificationTag) {
        this.tab = tab;
        this.color = color;
        this.notificationTag = notificationTag;
    }

    @SuppressWarnings("unchecked")
    void run(JDA discordClient) {
        long CHALLENGE_CHANNEL = 388135065621757967L;
        TextChannel channel = discordClient.getTextChannelById(CHALLENGE_CHANNEL);
        if (channel == null) {
            log.error("Channel #challenge can't be found.");
            return;
        }

        Message message = new GetUserFeedMessage(tab);
        csClient.send(message, (ResultMessage resultMessage) -> {
            Map<Object, Object> result = (Map<Object, Object>) resultMessage.getResult();
            List<Map<Object, Object>> feed =
                    (List<Map<Object, Object>>) result.get("feed");
            Map<Object, Object> challenge = (Map<Object, Object>) feed.get(0).get("challenge");

            // Check with db to see if this notification has already been sent out.
            long elapsedSeconds = (Instant.now().toEpochMilli() - (Long) feed.get(0).get("date")) / 1000;

            // If more than an hour ago, skip
            if (elapsedSeconds > MAX_LOOKBACK_TIME_IN_MS) {
                return;
            }
            String challengeId = String.format("%s", challenge.get("_id"));
            if (notifiedChallengeIds.contains(challengeId)) {
                return;
            }
            notifiedChallengeIds.add(challengeId);

            csClient.send(new GetMultipleWithVisibleFieldsMessage(List.of(challenge.get("authorId").toString())),
                    userResultMessage -> {
                        List<Map<Object, Object>> users =
                                (List<Map<Object, Object>>) userResultMessage.getResult();
                        Map<Object, Object> author = users.get(0);
                        csClient.send(new GetDetailsMessage(challengeId),
                                detailsResultMessage -> {
                                    Map<Object, Object> detailsResult =
                                            (LinkedHashMap<Object, Object>) detailsResultMessage.getResult();
                                    Map<Object, Object> task = (Map<Object, Object>) detailsResult.get("task");

                                    Notification notification = storage.lookupNotification(challengeId);
                                    if (notification != null) return;
                                    storage.saveNotification(challengeId);

                                    String challengeLink = String.format(
                                            "<https://app.codesignal.com/challenge/%s>", challengeId
                                    );
                                    channel.sendMessage(
                                            String.format(
                                                    "**NEW CHALLENGE**: **%s**\n%s\n%s",
                                                    challenge.get("name").toString(), challengeLink, notificationTag))
                                            .queue();
                                    String description =
                                            List.of(
                                                    task.get("description").toString()
                                                            .replaceAll("<[^>]*>", "")
                                                            .split("\n"))
                                                    .stream().filter(line -> !"".equals(line))
                                                    .collect(Collectors.joining("\n"));

                                    String pattern = "(?i)\\n[^\\nA-Za-z0-9]*(example|examples|input|output)" +
                                            "[^A-Za-z0-9\\n]*(input|output)?[^A-Za-z0-9\\n]*(\\n|$)";
                                    String[] headers = Pattern.compile(pattern)
                                            .matcher(description)
                                            .results()
                                            .map(MatchResult::group)
                                            .toArray(String[]::new);
                                    String[] parts = description.split(pattern);


                                    for (int i = 0; i < parts.length; i++) {
                                        EmbedBuilder eb = new EmbedBuilder();
                                        if (i == 0) {
                                            eb.setAuthor(
                                                    author.get("username").toString(),
                                                    "https://app.codesignal.com/profile/" + author.get("username").toString(),
                                                    author.get("avatar") != null ? author.get("avatar").toString() : null);
                                            eb.setTitle(challenge.get("name").toString());
                                            eb.setDescription(parts[0].substring(0, Math.min(parts[0].length(), 2000)));
                                        } else if (headers[i - 1].toLowerCase().contains("example")) {
                                            eb.setTitle("Example");
                                        } else if (headers[i - 1].toLowerCase().contains("input")) {
                                            eb.setTitle("I/O");
                                        }

                                        eb.setDescription(parts[i].substring(0, Math.min(parts[i].length(), 2000)));
                                        eb.setColor(color);

                                        if (i == parts.length - 1) {
                                            Map<Object, Object> io = (Map<Object, Object>) task.get("io");
                                            List<Map<Object, Object>> input = (List<Map<Object, Object>>) io.get(
                                                    "input");
                                            Map<Object, Object> output = (Map<Object, Object>) io.get("output");

                                            String inputBlock = input.stream().map(arg ->
                                                    String.format("*%s* `%s`: %s",
                                                            arg.get("type"),
                                                            arg.get("name"),
                                                            StringEscapeUtils.unescapeHtml4(
                                                                    List.of(arg.get("description").toString().split(
                                                                            "\n")).stream().filter(
                                                                                    l -> l.strip().length() > 1
                                                                    ).collect(Collectors.joining("\n"))
                                                            ).replaceAll("</?code>", "```")
                                                    )
                                            ).collect(Collectors.joining("\n"));

                                            eb.addField("Input", inputBlock, true);
                                            eb.addField("Output", String.format("*%s*: %s",
                                                    output.get("type"),
                                                    StringEscapeUtils.unescapeHtml4(
                                                            output.get("description").toString()
                                                    ).replaceAll("</?code>", "```")
                                            ), true);
                                        }

                                        channel.sendMessage(eb.build()).queue();
                                    }

                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.addField("Reward", String.format("%s", challenge.get("reward")), true);
                                    eb.addField("Duration", String.format("%s day(s)",
                                            (int) challenge.get("duration") / 1000 / 3600 / 24),
                                            true);
                                    eb.addField("Visibility", String.format("%s", challenge.get("visibility")),
                                            true);
                                    eb.addField("Problem Type", String.format("%s", challenge.get("generalType")),
                                            true);
                                    eb.addField("Ranking Type", String.format("%s", challenge.get("type")),
                                            true);
                                    eb.addField("Difficulty", String.format("%s", task.get("difficulty")), true);
                                    eb.addField("Task ID", String.format("%s", challenge.get("taskId")), true);
                                    eb.setFooter(
                                            "Have suggestions? Bug reports? Send them to @builder",
                                            "https://cdn.discordapp.com/emojis/493515691941560333.png");
                                    eb.setColor(color);
                                    eb.setTimestamp(Instant.now());
                                    channel.sendMessage(eb.build()).queue();
                                }
                        );
                    }
            );
        });
    }
}
