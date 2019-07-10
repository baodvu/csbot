package com.codesignal.csbot.watchers;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class StockMarketCalendarWatcher {
    private static final Logger logger = LoggerFactory.getLogger(StockMarketCalendarWatcher.class);

    void run() throws UnirestException {
        this.getTomorrowEvents();
    }

    private void getTomorrowEvents() throws UnirestException {
        String data = Unirest
                .post("https://www.investing.com/economic-calendar/Service/getCalendarFilteredData")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, " +
                        "like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                .header("Origin", "https://www.investing.com")
                .header("Referer", "https://www.investing.com/economic-calendar/?utm_source=email&utm_medium=email" +
                        "&utm_campaign=CTA")
                .field("country[]", "25")
                .field("country[]", "32")
                .field("country[]", "6")
                .field("country[]", "37")
                .field("country[]", "72")
                .field("country[]", "22")
                .field("country[]", "17")
                .field("country[]", "39")
                .field("country[]", "14")
                .field("country[]", "10")
                .field("country[]", "35")
                .field("country[]", "43")
                .field("country[]", "56")
                .field("country[]", "36")
                .field("country[]", "110")
                .field("country[]", "11")
                .field("country[]", "26")
                .field("country[]", "12")
                .field("country[]", "4")
                .field("country[]", "5")
                .field("importance[]", "2")
                .field("timeZone", "8")
                .field("timeFilter", "timeRemain")
                .field("currentTab", "tomorrow")
                .field("limit_from", "0")
                .asJson().getBody().getObject().get("data").toString();

        this.process(data);
    }

    private void process(String data) throws UnirestException{
        String marketCalendarNotice = data
                .replaceAll("</tr>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll(" +", " ");

        String content = "https://www.investing.com/economic-calendar\n```" + marketCalendarNotice.strip() + "```";

        String YACHT_BOT_WEBHOOK = System.getenv("YACHT_BOT_WEBHOOK");

        String response = Unirest.post(YACHT_BOT_WEBHOOK)
                .header("Accept", "application/json")
                .field("content", content)
                .asString().getBody();

        logger.info(response);
    }
}
