package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.models.DiscordMessageVersioned;
import com.codesignal.csbot.storage.Storage;
import com.codesignal.csbot.utils.Confucius;
import com.datastax.driver.core.utils.UUIDs;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.*;
import java.util.stream.Collectors;


public class UndeleteCommandHandler extends AbstractCommandHandler {
    private static final List<String> names = List.of("undelete", "ud", "undel", "tail");
    private final Storage storage = new Storage();

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Shows deleted/edited messages"; }

    public UndeleteCommandHandler() {
        ArgumentParser parser = this.buildArgParser();

        parser.addArgument("-n", "--messages")
                .type(Integer.class)
                .help("display the last n messages");
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
        Integer messageCount = ns.getInt("messages");

        TextChannel channel = event.getTextChannel();
        channel.sendMessage(String.format("Here are %s edited messages within the last hour:",
                messageCount == null ? "all" : messageCount.toString())).queue();

        int character_count = 0;

        List<String> buffer = new ArrayList<>();
        List<DiscordMessageVersioned> messages = storage.getEditedMessagesFromLastHour(event.getChannel().getIdLong());

        if (messageCount != null) {
            Set<Long> selectedMessageIds = new HashSet<>();
            for (int i = messages.size() - 1; i >= 0; i--) {
                selectedMessageIds.add(messages.get(i).getMessageId());
                if (selectedMessageIds.size() >= messageCount) break;
            }
            messages = messages.stream().filter(
                    discordMessageVersioned -> selectedMessageIds.contains(discordMessageVersioned.getMessageId()))
            .collect(Collectors.toList());
        }

        for (DiscordMessageVersioned message : messages) {
            User author = event.getJDA().getUserById(message.getAuthorId());
            long unix_timestamp = UUIDs.unixTimestamp(message.getCreatedAt());

            String s = String.format(
                    "`%s` `[%d/%s]`: %s",
                    author != null ? author.getName() : "",
                    message.getMessageId(),
                    new Date(unix_timestamp).toString(),
                    message.getContent()
            );

            if (character_count + s.length() > 2000) {
                channel.sendMessage(String.join("\n", buffer)).queue();
                buffer.clear();
                character_count = 0;
            }

            buffer.add(s);
            character_count += s.length() + 2;
        }

        if (!buffer.isEmpty()) {
            channel.sendMessage(String.join("\n", buffer)).queue();
            buffer.clear();
        }
    }
}
