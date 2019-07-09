package com.codesignal.csbot.watchers;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

class StockMarketCalendarWatcher {
    private static final Logger logger = LoggerFactory.getLogger(StockMarketCalendarWatcher.class);

    private static HttpClient client = HttpClient.newHttpClient();

    private void getTomorrowEvents() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.investing.com/economic-calendar/Service/getCalendarFilteredData"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, " +
                        "like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                .header("Origin", "https://www.investing.com")
                .header("Referer", "https://www.investing.com/economic-calendar/?utm_source=email&utm_medium=email" +
                        "&utm_campaign=CTA")
                .POST(HttpRequest.BodyPublishers.ofString("country%5B%5D=25&country%5B%5D=32&country%5B%5D=6&country" +
                        "%5B%5D=37&country%5B%5D=72&country%5B%5D=22&country%5B%5D=17&country%5B%5D=39&country%5B%5D" +
                        "=14&country%5B%5D=10&country%5B%5D=35&country%5B%5D=43&country%5B%5D=56&country%5B%5D=36" +
                        "&country%5B%5D=110&country%5B%5D=11&country%5B%5D=26&country%5B%5D=12&country%5B%5D=4" +
                        "&country%5B%5D=5&importance%5B%5D=3&timeZone=8&timeFilter=timeRemain&currentTab=tomorrow" +
                        "&limit_from=0"))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(StockMarketCalendarWatcher::formatTable);
    }

    void run() {
        this.getTomorrowEvents();
    }

    private static void formatTable(String htmlData) {
        JSONObject jsonObj = new JSONObject(new JSONTokener(htmlData));

        String data = (String) jsonObj.get("data");

        String marketCalendarNotice = data
                .replaceAll("</tr>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll(" +", " ");

        Map<String, String> message = Map.of(
                "content", "https://www.investing.com/economic-calendar\n```" + marketCalendarNotice.strip() + "```"
        );

        String YACHT_BOT_WEBHOOK = System.getenv("YACHT_BOT_WEBHOOK");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(YACHT_BOT_WEBHOOK))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject(message).toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(logger::info);
    }
}
