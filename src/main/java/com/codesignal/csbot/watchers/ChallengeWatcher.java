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

    ChallengeWatcher(String tab, Color color) {
        this.tab = tab;
        this.color = color;
    }

    @SuppressWarnings("unchecked")
    void run(JDA discordClient) {
        long CHALLENGE_CHANNEL = 420829252842291210L;
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
                                                    "**NEW CHALLENGE**: **%s**\n%s",
                                                    challenge.get("name").toString(), challengeLink)).queue();
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

                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.setAuthor(
                                            author.get("username").toString(),
                                            "https://app.codesignal.com/profile/" + author.get("username").toString(),
                                            author.get("avatar").toString());
                                    eb.setTitle("Problem Statement");
                                    eb.setDescription(parts[0].substring(0, Math.min(parts[0].length(), 2000)));
                                    eb.setColor(new Color(0xF4CA3A));
                                    channel.sendMessage(eb.build()).queue();

                                    for (int i = 1; i < parts.length; i++) {
                                        eb = new EmbedBuilder();
                                        if (headers[i - 1].toLowerCase().contains("example")) {
                                            eb.setTitle("Example");
                                        } else if (headers[i - 1].toLowerCase().contains("input")) {
                                            eb.setTitle("Input/Output");
                                        }

                                        eb.setDescription(parts[i].substring(0, Math.min(parts[i].length(), 2000)));
                                        eb.setColor(new Color(0xF4CA3A));
                                        channel.sendMessage(eb.build()).queue();
                                    }

                                    eb = new EmbedBuilder();
                                    eb.addField("Reward", String.format("%s", challenge.get("reward")), true);
                                    eb.addField("Duration", String.format("%s day(s)",
                                            (int) challenge.get("duration") / 1000 / 3600 / 24),
                                            true);
                                    eb.addField("Visibility", String.format("%s", challenge.get("visibility")),
                                            true);
                                    eb.addField("Problem Type", String.format("%s", challenge.get("type")), true);
                                    eb.addField("Ranking Type", String.format("%s", challenge.get("generalType")),
                                            true);
                                    eb.addField("Difficulty", String.format("%s", task.get("difficulty")), true);
                                    eb.addField("Task ID", String.format("%s", challenge.get("taskId")), true);
                                    eb.addField("Challenge ID",
                                            String.format("[%s](%s)", challengeId, challengeLink), true);
                                    eb.addField("Author ID", String.format("%s", challenge.get("authorId")), true);
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
