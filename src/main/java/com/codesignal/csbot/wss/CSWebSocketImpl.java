package com.codesignal.csbot.wss;

import com.codesignal.csbot.CSBot;
import com.codesignal.csbot.adapters.codesignal.message.Callback;
import com.codesignal.csbot.adapters.codesignal.message.GetServerTimeMessage;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.utils.Randomizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CSWebSocketImpl implements CSWebSocket {
    private static final Logger log = LoggerFactory.getLogger(CSBot.class);

    private final int TIMEOUT_IN_MS = 5000;
    private final ConcurrentHashMap<Long, Callback> callbacks = new ConcurrentHashMap<>();
    private long messageId;
    private volatile boolean isReady = false;

    private WebSocket ws;

    public CSWebSocketImpl build() throws Exception {
        WebSocketFactory factory = new WebSocketFactory();
        String randomString = Randomizer.getAlphaNumericString(8);

        ws = factory.createSocket("wss://app.codesignal.com/sockjs/0/" + randomString + "/websocket",
                TIMEOUT_IN_MS);
        ws.addHeader("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/75.0.3770.100 Safari/537.36");

        ws.addListener(new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                log.info("[WS] Connected");
            }

            @Override
            public void onTextMessage(WebSocket websocket, String message) {
                log.info("[WS-received] {}", message);
                if (message.equals("a[\"{\\\"msg\\\":\\\"ping\\\"}\"]")) {
                    ws.sendText("[\"{\\\"msg\\\":\\\"pong\\\"}\"]");
                } else if (message.contains("a[\"{\\\"msg\\\":\\\"connected\\\"")) {
                    isReady = true;
                    send(new GetServerTimeMessage());
                } else if (message.contains("a[\"{\\\"msg\\\":\\\"result\\\"")) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        // substring(1) to remove the prefix "a" from codesignal server.
                        // It probably stands for "array" but our JSON parser won't recognize that syntax.
                        String[] messages = objectMapper.readValue(message.substring(1), String[].class);
                        ResultMessage resultMessage = objectMapper.readValue(messages[0], ResultMessage.class);
                        Callback callback = callbacks.remove(Long.parseLong(resultMessage.getId()));
                        callback.onSuccess(resultMessage);
                    } catch (Exception exp) {
                        log.error(exp.getMessage());
                    }
                }
            }
        });

        // Connect to the server and perform an opening handshake.
        // This method blocks until the opening handshake is finished.
        ws.connect();
        ws.sendText(
                "[\"{\\\"msg\\\":\\\"connect\\\",\\\"version\\\":\\\"1\\\"," +
                        "\\\"support\\\":[\\\"1\\\",\\\"pre2\\\",\\\"pre1\\\"]}\"]");

        messageId = 0;
        return this;
    }

    public void send(Message message, Callback callback) {
        while (!isReady) {
            try {
                Thread.sleep(1000);
            }
            catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        messageId++;
        message.setId(messageId + "");
        callbacks.put(messageId, callback);
        try {
            ObjectMapper om = new ObjectMapper();
            String toSent = om.writeValueAsString(List.of(om.writeValueAsString(message)));
            log.info("[WS-sending] " + toSent);
            ws.sendText(toSent);
        } catch (JsonProcessingException exception) {
            // pass
        }
    }

    public void send(Message message) {
        send(message, (ResultMessage result) -> {});
    }
}
