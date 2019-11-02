package com.codesignal.csbot.utils;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


public class Deluge {
    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS).cookieHandler(this.cookieManager).build();

    private int id = 0;

    public Deluge() throws Exception {
        sendMessage("auth.login", "[\"" + System.getenv("DELUGE_TOKEN") + "\"]");
    }

    public String addMagnet(String magnetLink) throws Exception {
        return sendMessage("core.add_torrent_magnet", "[\"" + magnetLink + "\", {}]");
    }

    private String sendMessage(String method, String params) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8112/json"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(String.format(
                        "{\"method\": \"%s\", \"params\": %s, \"id\": %d}", method, params, ++id
                )))
                .timeout(Duration.ofMinutes(2))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
