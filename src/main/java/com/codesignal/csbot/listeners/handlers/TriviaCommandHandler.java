package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.listeners.BotCommand;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.RAMDirectory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.util.Pair;

import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TriviaCommandHandler extends AbstractCommandHandler {
    private static final List<String> names = List.of("trivia", "tr", "trv", "quiz");
    private static Map<Integer, List<String>> CATEGORIES = Map.ofEntries(
            Map.entry(9, List.of("General Knowledge", "general", "any", "all")),
            Map.entry(10, List.of("Entertainment: Books", "books")),
            Map.entry(11, List.of("Entertainment: Film", "film", "movie")),
            Map.entry(12, List.of("Entertainment: Music", "music", "song")),
            Map.entry(13, List.of("Entertainment: Musicals & Theatres", "theater")),
            Map.entry(14, List.of("Entertainment: Television", "tv", "television")),
            Map.entry(15, List.of("Entertainment: Video Games", "games")),
            Map.entry(16, List.of("Entertainment: Board Games", "board games", "board")),
            Map.entry(17, List.of("Science & Nature", "science", "nature")),
            Map.entry(18, List.of("Science: Computers", "computers", "PC", "computer science", "cs")),
            Map.entry(19, List.of("Science: Mathematics", "math", "maths", "mathematics")),
            Map.entry(20, List.of("Mythology", "myth")),
            Map.entry(21, List.of("Sports", "sports")),
            Map.entry(22, List.of("Geography", "geo")),
            Map.entry(23, List.of("History", "hist")),
            Map.entry(24, List.of("Politics", "politics")),
            Map.entry(25, List.of("Art", "art")),
            Map.entry(26, List.of("Celebrities", "celeb")),
            Map.entry(27, List.of("Animals", "animals")),
            Map.entry(28, List.of("Vehicles", "cars")),
            Map.entry(29, List.of("Entertainment: Comics", "comics")),
            Map.entry(30, List.of("Science: Gadgets", "gadgets")),
            Map.entry(31, List.of("Entertainment: Japanese Anime & Manga", "anime", "manga", "japan", "japanese")),
            Map.entry(32, List.of("Entertainment: Cartoon & Animations", "cartoon", "animation"))
    );
    private Map<String, Integer> categoryToIndex = new HashMap<>();
    private SpellChecker categorySpellChecker;

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Get a trivia question"; }

    private static final Map<Long, Pair<Integer, String>> pendingAnswers = new HashMap<>();

    public TriviaCommandHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("category")
                .nargs("?")
                .help("What you want the bot to say");
        for (Map.Entry<Integer, List<String>> entry: CATEGORIES.entrySet()) {
            for (String spelling: entry.getValue()) {
                categoryToIndex.put(spelling.toLowerCase(), entry.getKey());
            }
        }
        buildLuceneDictionaryForCategories();
    }

    private void buildLuceneDictionaryForCategories() {
        StringBuilder sb = new StringBuilder();
        for (String spelling: categoryToIndex.keySet()) {
            sb.append(spelling);
            sb.append("\n");
        }
        StringReader reader = new StringReader(sb.toString());

        try {
            PlainTextDictionary words = new PlainTextDictionary(reader);

            // use in-memory lucene spell checker to make the suggestions
            RAMDirectory dir = new RAMDirectory();
            categorySpellChecker = new SpellChecker(dir);
            categorySpellChecker.indexDictionary(words, 10, 10);
        } catch (IOException ignored) {
        }
    }

    public void onMessageReceived(MessageReceivedEvent event, BotCommand botCommand)
            throws ArgumentParserException {
        Namespace ns = this.parseArgs(event);
        String categoryText = ns.getString("category") != null ? ns.getString("category") : "any";

        int category = 9; // Default category
        try {
            if (categoryToIndex.containsKey(categoryText)) {
                category = categoryToIndex.get(categoryText);
            } else {
                String[] suggestions = categorySpellChecker.suggestSimilar(categoryText, 5);
                if (suggestions.length > 0) {
                    category = categoryToIndex.get(suggestions[0]);
                }
            }
        } catch (IOException exp){
            event.getChannel().sendMessage("Unable to find the category.").queue();
            return;
        }

        Long channelId = event.getChannel().getIdLong();
        if (pendingAnswers.containsKey(channelId)) {
            event.getChannel().sendMessage("Answer the current question first").queue();
            return;
        }

        try {
            JSONObject response = Unirest.get("https://opentdb.com/api.php")
                    .queryString("amount", 1)
                    .queryString("category", category)
                    .asJson().getBody().getObject();
            JSONArray results = response.getJSONArray("results");
            for (int i = 0; i < results.length(); ++i) {
                JSONObject result = results.getJSONObject(i);
                final String correctAnswer = StringEscapeUtils.escapeHtml4(result.getString("correct_answer"));
                List<Object> answers = result.getJSONArray("incorrect_answers").toList();
                answers.add(correctAnswer);
                Collections.shuffle(answers);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(new Color(0xF47400));
                eb.setDescription(StringEscapeUtils.unescapeHtml4(result.getString("question") + "\n"
                        + IntStream.range(0, answers.size())
                        .mapToObj(j -> String.format("%d. %s", j + 1, answers.get(j)))
                        .collect(Collectors.joining("\n"))));
                eb.addField("Difficulty", result.getString("difficulty"), true);
                eb.addField("Category", result.getString("category"), true);
                event.getChannel().sendMessage(eb.build()).queue();
                Integer correctAnswerIdx = answers.indexOf(correctAnswer) + 1;
                pendingAnswers.put(channelId, Pair.of(correctAnswerIdx, correctAnswer));
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                if (pendingAnswers.containsKey(channelId)
                                        && pendingAnswers.get(channelId).getSecond().equals(correctAnswer)) {
                                    Pair<Integer, String> answer = pendingAnswers.remove(channelId);
                                    if (answer != null) {
                                        event.getChannel().sendMessage(
                                                "Timeout! The correct answer is " + correctAnswer).queue();
                                    }
                                }
                            }
                        },
                        10000
                );
            }
        } catch (UnirestException exp) {
            event.getChannel().sendMessage("Trivia database is out of reach").queue();
        }
    }

    public void checkAnswer(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().matches("\\d")) {
            if (pendingAnswers.containsKey(event.getChannel().getIdLong())) {
                Pair<Integer, String> answer = pendingAnswers.get(event.getChannel().getIdLong());
                if (answer.getFirst().toString().equals(event.getMessage().getContentRaw().strip())) {
                    event.getChannel().sendMessage(answer.getSecond() + " is correct!").queue();
                } else {
                    event.getChannel().sendMessage(
                            "The correct answer is " + answer.getSecond() + ". Good luck next time!").queue();
                }
                pendingAnswers.remove(event.getChannel().getIdLong());
            }
        }
    }

//    private String getToken() throws UnirestException {
//        return Unirest.get("https://opentdb.com/api_token.php")
//                .queryString("command", "request")
//                .asString().getBody();
//    }
}
