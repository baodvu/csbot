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
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.stream.StreamSupport;


class ChallengeWatcher {
    private static final Logger log = LoggerFactory.getLogger(ChallengeWatcher.class);

    // Reduce calls to database by caching processed ids.
    private static final Set<String> notifiedChallengeIds = new HashSet<>();

    // Cassandra database.
    private final Storage storage = new Storage();
    private final CodesignalClient csClient = CodesignalClientSingleton.getInstance();

    // If the challenge is too old we shouldn't dig it up.
    private final int MAX_LOOKBACK_TIME_IN_MS = 1000 * 60 * 60;  // 1 hour

    // Setting
    private final String tab;
    private final Color color;
    private final String notificationTag;

    ChallengeWatcher(String tab, Color color, String notificationTag) {
        this.tab = tab;
        this.color = color;
        this.notificationTag = notificationTag;
    }

    void run(JDA discordClient) {
        String ENV = System.getenv("ENV");
        long CHALLENGE_CHANNEL = null != ENV && ENV.equals("prod") ? 388135065621757967L : 420829252842291210L;
        TextChannel channel = discordClient.getTextChannelById(CHALLENGE_CHANNEL);
        if (channel == null) {
            log.error("Channel #challenge can't be found.");
            return;
        }

        Message message = new GetUserFeedMessage(tab, 0, 1);
        csClient.send(message, (ResultMessage resultMessage) -> {
            JsonNode feed = resultMessage.getResult().get("feed");
            JsonNode challenge = feed.get(0).get("challenge");

            // Check with db to see if this notification has already been sent out.
            long elapsedSeconds = (Instant.now().toEpochMilli() - feed.get(0).get("date").asLong()) / 1000;

            // If more than an hour ago, skip
            if (elapsedSeconds > MAX_LOOKBACK_TIME_IN_MS) {
                return;
            }
            String challengeId = challenge.get("_id").textValue();
            if (notifiedChallengeIds.contains(challengeId)) {
                return;
            }
            notifiedChallengeIds.add(challengeId);

            csClient.send(new GetMultipleWithVisibleFieldsMessage(List.of(challenge.get("authorId").textValue())),
                    userResultMessage -> {
                        JsonNode users = userResultMessage.getResult();
                        JsonNode author = users.get(0);
                        csClient.send(new GetDetailsMessage(challengeId),
                                detailsResultMessage -> {
                                    JsonNode task = detailsResultMessage.getResult().get("task");

                                    Notification notification = storage.lookupNotification(challengeId);
                                    if (notification != null) return;
                                    storage.saveNotification(challengeId);

                                    String challengeLink = String.format(
                                            "<https://app.codesignal.com/challenge/%s>", challengeId
                                    );
                                    channel.sendMessage(
                                            String.format(
                                                    "**NEW CHALLENGE**: **%s**\n%s\n%s",
                                                    challenge.get("name").textValue(), challengeLink, notificationTag))
                                            .queue();
                                    String description =
                                            List.of(
                                                    task.get("description").textValue()
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
                                                    author.get("username").textValue(),
                                                    "https://app.codesignal.com/profile/" + author.get("username").textValue(),
                                                    author.get("avatar") != null ? author.get("avatar").textValue() : null);
                                            eb.setTitle(challenge.get("name").textValue());
                                            eb.setDescription(parts[0].substring(0, Math.min(parts[0].length(), 2000)));
                                        } else if (headers[i - 1].toLowerCase().contains("example")) {
                                            eb.setTitle("Example");
                                        } else if (headers[i - 1].toLowerCase().contains("input")) {
                                            eb.setTitle("I/O");
                                        }

                                        eb.setDescription(parts[i].substring(0, Math.min(parts[i].length(), 2000)));
                                        eb.setColor(color);

                                        if (i == parts.length - 1) {
                                            JsonNode io = task.get("io");
                                            JsonNode input = io.get("input");
                                            JsonNode output = io.get("output");

                                            String inputBlock = StreamSupport.stream(input.spliterator(), false).map(arg ->
                                                    String.format("*%s* `%s`: %s",
                                                            arg.get("type").textValue(),
                                                            arg.get("name").textValue(),
                                                            StringEscapeUtils.unescapeHtml4(
                                                                    List.of(arg.get("description").textValue().split(
                                                                            "\n")).stream().filter(
                                                                                    l -> l.strip().length() > 1
                                                                    ).collect(Collectors.joining("\n"))
                                                            ).replaceAll("</?code>", "```")
                                                    )
                                            ).collect(Collectors.joining("\n"));

                                            eb.addField("Input", inputBlock, true);
                                            eb.addField("Output", String.format("*%s*: %s",
                                                    output.get("type").textValue(),
                                                    StringEscapeUtils.unescapeHtml4(
                                                            output.get("description").textValue()
                                                    ).replaceAll("</?code>", "```")
                                            ), true);
                                        }

                                        channel.sendMessage(eb.build()).queue();
                                    }

                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.addField(
                                            "Reward",
                                            challenge.get("reward") != null
                                                    ? challenge.get("reward").asText()
                                                    : "N/A",
                                            true);
                                    eb.addField(
                                            "Duration",
                                            String.format("%s day(s)",
                                            challenge.get("duration").intValue() / 1000 / 3600 / 24),
                                            true);
                                    eb.addField("Visibility", challenge.get("visibility").textValue(),
                                            true);
                                    eb.addField("Problem Type", challenge.get("generalType").textValue(),
                                            true);
                                    eb.addField("Ranking Type", challenge.get("type").textValue(),
                                            true);
                                    eb.addField(
                                            "Difficulty",
                                            challenge.get("difficulty") != null
                                                    ? challenge.get("difficulty").asText()
                                                    : "N/A",
                                            true);
                                    eb.addField("Task ID", challenge.get("taskId").textValue(), true);
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
