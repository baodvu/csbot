package com.codesignal.csbot.listeners.handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


class HelpArgumentException extends ArgumentParserException {
    HelpArgumentException(ArgumentParser parser) {
        super(parser);
    }
}


class HelpArgumentAction implements ArgumentAction {
    @Override
    public void run(ArgumentParser parser, Argument arg,
                    Map<String, Object> attrs, String flag, Object value)
            throws ArgumentParserException {
        throw new HelpArgumentException(parser);
    }

    @Override
    public boolean consumeArgument() {
        return false;
    }

    @Override
    public void onAttach(Argument arg) {
    }

}


abstract class AbstractCommandHandler implements CommandHandler {
    private ArgumentParser parser;

    ArgumentParser buildArgParser() {
        parser = ArgumentParsers.newArgumentParser(this.getNames().get(0), false)
                .defaultHelp(true)
                .description(this.getShortDescription());

        parser.addArgument( "-h", "--help")
                .action(new HelpArgumentAction())
                .help("show this help message and exit");
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
            Namespace ns = parser.parseArgs(tokens.toArray(String[]::new));
            if (ns.getBoolean("help")) {
                event.getTextChannel().sendMessage(parser.formatHelp()).queue();
                throw new ArgumentParserException("user requested help", parser);
            }
            return ns;
        } catch (HelpArgumentException e) {
            event.getTextChannel().sendMessage(
                    "```\n" + this.getParser().formatHelp() + "\n```"
            ).queue();
            throw e;
        } catch (ArgumentParserException e) {
            event.getTextChannel().sendMessage(
                    "```error: " + e.getLocalizedMessage() + "\n" + this.getParser().formatUsage() + "```"
            ).queue();
            throw e;
        }
    }
}
