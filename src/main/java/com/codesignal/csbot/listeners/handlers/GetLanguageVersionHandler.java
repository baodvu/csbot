package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.adapters.codesignal.CodesignalClient;
import com.codesignal.csbot.adapters.codesignal.CodesignalClientSingleton;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.adapters.codesignal.message.taskService.GetRunResultMessage;
import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GetLanguageVersionHandler extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(GetCSDailyHandler.class);
    private static final List<String> PROGRAMMING_LANGUAGES = List.of(
            "cpp",
            "java",
            "jl",
            "lua",
            "perl",
            "php",
            "py3",
            "rs"
    );
    private static final List<String> names = List.of("cslv", "cs-lang-version");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Get language version"; }

    public GetLanguageVersionHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("-l", "--lang")
                .choices(PROGRAMMING_LANGUAGES)
                .help("Specify the programming language").required(true);
    }

    public void onMessageReceived(MessageReceivedEvent event)
            throws ArgumentParserException {
        Namespace ns = this.parseArgs(event);
        String language = ns.getString("lang");

        CodesignalClient csClient = CodesignalClientSingleton.getInstance();

        ResultMessage resultMessage;
        try {
            resultMessage = csClient.send(new GetRunResultMessage(
                    "jwr339Kq6e3LQTsfa",
                    loadFile("csversioncheck/" + language + ".txt"),
                    language,
                    "arcade",
                    false)).get();
        } catch (InterruptedException|ExecutionException exp) {
            return;
        }

        JsonNode res = resultMessage.getResult();
        String version = res.get("testResults").get("1").get("consoleOutput").asText().split("\n")[0];
        EmbedBuilder eb = new EmbedBuilder();
        eb.addField(language + " version", version, false);
        eb.setColor(new Color(0xF45964));
        event.getChannel().sendMessage(eb.build()).queue();
    }

    private String loadFile(String filePath) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(filePath);
        if (in == null) {
            return null;
        }
        try {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException exp) {
            exp.printStackTrace();
            return null;
        }
    }
}

