package com.codesignal.csbot.watchers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.adapters.codesignal.message.challenges.GetDashboard;
import com.codesignal.csbot.adapters.codesignal.message.challengeservice.GetDetailsMessage;
import com.codesignal.csbot.adapters.codesignal.message.userservice.GetMultipleWithVisibleFieldsMessage;
import com.codesignal.csbot.models.Notification;
import com.codesignal.csbot.storage.Storage;
import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
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
    private final int MAX_LOOKBACK_TIME_IN_SECONDS = 60 * 60;  // 1 hour

    // Setting
    private final String visibility;
    private final Color color;
    private final String notificationTag;

    ChallengeWatcher(String visibility, Color color, String notificationTag) {
        this.visibility = visibility;
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

        Message message = new GetDashboard(visibility, 1);
        csClient.send(message, (ResultMessage resultMessage) -> {
            JsonNode firstChallenge = resultMessage.getResult().at("/challenges/0");
            JsonNode challenge = firstChallenge.get("challengeDoc");

            // Check with db to see if this notification has already been sent out.
            long elapsedSeconds = (Instant.now().toEpochMilli() - challenge.get("date").asLong()) / 1000;

            // If more than an hour ago, skip
            if (elapsedSeconds > MAX_LOOKBACK_TIME_IN_SECONDS) {
                return;
            }
            String challengeId = challenge.get("_id").textValue();
            if (notifiedChallengeIds.contains(challengeId)) {
                return;
            }
            notifiedChallengeIds.add(challengeId);

            JsonNode author = firstChallenge.get("authorDoc");
            JsonNode task = firstChallenge.get("taskDoc");

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
                    eb.setDescription(StringUtils.abbreviate(parts[0], 2048));
                } else if (headers[i - 1].toLowerCase().contains("example")) {
                    eb.setTitle("Example");
                } else if (headers[i - 1].toLowerCase().contains("input")) {
                    eb.setTitle("I/O");
                }

                eb.setDescription(StringUtils.abbreviate(parts[i], 2048));
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

            int duration = (challenge.get("expireDate").intValue()
                    - challenge.get("startDate").intValue());
            EmbedBuilder eb = new EmbedBuilder();
            eb.addField(
                    "Reward",
                    challenge.get("reward") != null
                            ? String.format("%d", challenge.get("reward").intValue())
                            : "N/A",
                    true);
            eb.addField(
                    "Duration",
                    String.format("%s day(s)", duration / 1000 / 3600 / 24),
                    true);
            eb.addField("Visibility", challenge.get("visibility").textValue(),
                    true);
            eb.addField("Problem Type", challenge.get("generalType").textValue(),
                    true);
            eb.addField("Ranking Type", challenge.get("type").textValue(),
                    true);
            eb.addField("Task ID", challenge.get("taskId").textValue(), true);
            eb.setFooter(
                    "Have suggestions? Bug reports? Send them to @builder",
                    "https://cdn.discordapp.com/emojis/493515691941560333.png");
            eb.setColor(color);
            eb.setTimestamp(Instant.now());
            channel.sendMessage(eb.build()).queue();
        });
    }
}
