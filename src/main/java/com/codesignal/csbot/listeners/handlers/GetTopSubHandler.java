package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.adapters.codesignal.message.taskleaderboardservice.GetSubmissionMessage;
import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class GetTopSubHandler extends CodeSignalCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(GetTopSubHandler.class);
    private static final List<String> PROGRAMMING_LANGUAGES = List.of(
            "clj",
            "coffee",
            "lisp",
            "c",
            "cpp",
            "cs",
            "d",
            "dart",
            "exs",
            "erl",
            "pas",
            "for",
            "fs",
            "go",
            "groovy",
            "hs",
            "java",
            "js",
            "jl",
            "kt",
            "lua",
            "nim",
            "objc",
            "ocaml",
            "octave",
            "perl",
            "php",
            "py",
            "py3",
            "r",
            "rb",
            "rs",
            "scala",
            "st",
            "swift",
            "tcl",
            "ts",
            "vb"
    );
    private static final List<String> names = List.of("cstop", "cs-top", "cst");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Retrieve CodeSignal top solutions"; }

    public GetTopSubHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("-l", "--lang")
                .choices(PROGRAMMING_LANGUAGES).setDefault("")
                .help("Specify language to filter on");
        parser.addArgument("-cid", "--challenge-id")
                .help("Challenge ID").required(false);
    }

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException{
        Namespace ns = this.parseArgs(event);

        final String challengeId;
        if (ns.getString("challenge-id") == null) {
            try {
                challengeId = getDailyChallengeId().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return;
            }
        } else {
            challengeId = ns.getString("challenge-id");
        }

        String language = ns.getString("lang");

        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        Message message = new GetSubmissionMessage(challengeId, language);
        csClient.send(message, (ResultMessage resultMessage) -> {
            JsonNode submissions = resultMessage.getResult().get("submissions");
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Top 10 solutions", "https://app.codesignal.com/challenge/" + challengeId);
            submissions.forEach((JsonNode submission) -> {
                long time = Long.parseLong(submission.get("time").toString());
                eb.addField(
                        submission.get("authorUsername").asText(),
                        String.format("⟢ `%20s` ⟡ `%02d:%02d:%02d` ⟡ `%s↑` ⟣",
                                submission.get("language").toString() + " " + submission.get("chars").asText(),
                                time / (3600 * 1000),
                                time % (3600 * 1000) / (60 * 1000),
                                time % (60 * 1000) / 1000,
                                submission.get("voteScore").asInt()
                        ), false);
            });
            eb.setColor(new Color(0xF45964));
            event.getChannel().sendMessage(eb.build()).queue();
        });
    }

}
