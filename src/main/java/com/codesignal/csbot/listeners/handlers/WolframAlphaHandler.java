package com.codesignal.csbot.listeners.handlers;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.InputStream;
import java.util.List;

public class WolframAlphaHandler extends AbstractCommandHandler {
    private static final String WA_APP_ID = "33JG8A-83ALGXGR86";
    private static List<String> names = List.of("wolfram", "wolframalpha", "wa", "eval", "calc");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Query Wolfram Alpha API"; }

    public WolframAlphaHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("query").nargs("*")
                .help("the query expression to send to WA");
    }

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        Namespace ns = this.parseArgs(event);
        String query = String.join(" ", ns.getList("query"));

        try {
            String response = Unirest
                .get("http://api.wolframalpha.com/v1/result")
                .field("appid", WA_APP_ID)
                .field("i", query).asString().getBody();

            while (!response.isEmpty()) {
                int end = Math.min(2000, response.length());
                String chunk = response.substring(0, end);
                event.getTextChannel().sendMessage(chunk).queue();
                response = response.substring(end);
            }
        } catch (UnirestException e) {
            event.getTextChannel().sendMessage("Can't parse text response").queue();
            return;
        }

        try {
            InputStream responseStream = Unirest
                    .get("http://api.wolframalpha.com/v1/simple")
                    .field("appid", WA_APP_ID)
                    .field("i", query).asBinary().getRawBody();
            event.getTextChannel().sendFile(responseStream, "response.gif").queue();
        } catch (UnirestException e) {
            event.getTextChannel().sendMessage("Can't parse image response").queue();
        }
    }
}
