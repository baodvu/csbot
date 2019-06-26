package com.codesignal.csbot.wss;

import com.codesignal.csbot.CSBot;
import com.neovisionaries.ws.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


public class CSWebSocket {
    private static final Logger logger = LoggerFactory.getLogger(CSBot.class);

    private final int TIMEOUT_IN_MS = 5000;

    private WebSocket ws;

    public CSWebSocket build() throws Exception {
        WebSocketFactory factory = new WebSocketFactory();

        ws = factory.createSocket("wss://app.codesignal.com/sockjs/239/bbblncw7/websocket", TIMEOUT_IN_MS);
        ws.addHeader("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/75.0.3770.100 Safari/537.36");

        ws.addListener(new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                logger.info("[WS] Connected");
            }

            @Override
            public void onTextMessage(WebSocket websocket, String message) {
                logger.info("[WS-received] {}", message);
                if (message.equals("a[\"{\\\"msg\\\":\\\"ping\\\"}\"]")) {
                    ws.sendText("[\"{\\\"msg\\\":\\\"pong\\\"}\"]");
                }
            }
        });

        // Connect to the server and perform an opening handshake.
        // This method blocks until the opening handshake is finished.
        ws.connect();
        ws.sendText(
                "[\"{\\\"msg\\\":\\\"connect\\\",\\\"version\\\":\\\"1\\\"," +
                        "\\\"support\\\":[\\\"1\\\",\\\"pre2\\\",\\\"pre1\\\"]}\"]");

        return this;
    }
}
