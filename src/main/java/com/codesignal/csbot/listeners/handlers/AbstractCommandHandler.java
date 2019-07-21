package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


abstract class AbstractCommandHandler implements CommandHandler {
    private ArgumentParser parser;

    ArgumentParser buildArgParser() {
        parser = ArgumentParsers.newArgumentParser(this.getNames().get(0))
                .defaultHelp(true)
                .description(this.getShortDescription());
        return parser;
    }

    private ArgumentParser getParser() {
        return parser;
    }

    Namespace parseArgs(MessageReceivedEvent event) throws ArgumentParserException {
        String receivedCommand = event.getMessage().getContentRaw();
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(receivedCommand, " ");
        tokenizer.nextToken(); // Skip first token because it's just the command name
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }

        try {
            return parser.parseArgs(tokens.toArray(String[]::new));
        } catch (ArgumentParserException e) {
            event.getTextChannel().sendMessage(
                    "```error: " + e.getLocalizedMessage() + "\n" + this.getParser().formatUsage() + "```"
            ).queue();
            throw e;
        }
    }
}
