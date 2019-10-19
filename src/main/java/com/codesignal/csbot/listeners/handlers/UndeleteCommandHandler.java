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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class UndeleteCommandHandler extends AbstractCommandHandler {
    private static final List<String> names = List.of("undelete", "ud", "undel", "tail");
    private final Storage storage = new Storage();

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Shows deleted/edited messages"; }

    public UndeleteCommandHandler() {
        ArgumentParser parser = this.buildArgParser();

        parser.addArgument("-n", "--lines")
                .type(Integer.class)
                .setDefault(200)
                .help("display the last n lines");
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
        int lineCount = ns.getInt("lines");

        TextChannel channel = event.getTextChannel();
        channel.sendMessage("Here are the edited messages within the last hour:").queue();

        int message_count = 0;
        int character_count = 0;

        List<String> buffer = new ArrayList<>();

        for (DiscordMessageVersioned message : storage.getEditedMessagesFromLastHour(event.getChannel().getIdLong())) {
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
            message_count++;

            if (message_count >= lineCount) {
                channel.sendMessage(String.join("\n", buffer)).queue();
                buffer.clear();
                break;
            }
        }

        if (!buffer.isEmpty()) {
            channel.sendMessage(String.join("\n", buffer)).queue();
            buffer.clear();
        }
    }
}
