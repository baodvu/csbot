package com.codesignal.csbot.wss;

import com.codesignal.csbot.adapters.codesignal.message.*;
import com.codesignal.csbot.utils.Randomizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class CSWebSocketImpl implements CSWebSocket {
    private static final Logger log = LoggerFactory.getLogger(CSWebSocketImpl.class);
    private final static AtomicInteger wssCount = new AtomicInteger(0);
    private static final List<String> CODESIGNAL_USERS = List.of(
            "sele_tester@tuta.io",
            "sele_tester1@tuta.io",
            "sele_tester2@tuta.io",
            "incandescant_2007@yahoo.com",
            "cplusplusguy7@gmail.com",
            "grandgrant_92@yahoo.com",
            "themaxreversal35a@gmail.com",
            "cicade12312fs@tuta.io",
            "lipsandscissors@gmail.com"
    );
    public static final int MAX_CONCURRENT = CODESIGNAL_USERS.size();

    private final int TIMEOUT_IN_MS = 5000;
    private final ConcurrentHashMap<Long, Callback> callbacks = new ConcurrentHashMap<>();
    private volatile AtomicInteger messageId;
    private volatile CountDownLatch isBooting;
    private volatile long millisecondsSinceEpoch = System.currentTimeMillis();

    private WebSocket ws;


    public CSWebSocketImpl build() throws Exception {
        resetConnection();
        return this;
    }

    public void send(Message message, Callback callback) {
        // Check if connection is healthy; millisecondsSinceEpoch is refreshed by server pings.
        // If last ping was older than one minute than reset connection.
        if (System.currentTimeMillis() - millisecondsSinceEpoch > 1000 * 60) {
            try {
                this.resetConnection();
            } catch (Exception exception) {
                exception.printStackTrace();
                return;
            }
        }

        try {
            isBooting.await();
        } catch (InterruptedException exception) {
            log.error("Thread is interrupted. Return prematurely.");
            return;
        }

        long snapshotMessageId = messageId.getAndIncrement();
        message.setId(snapshotMessageId + "");
        callbacks.put(snapshotMessageId, callback);
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

    private void resetConnection() throws Exception {
        messageId = new AtomicInteger(0);
        isBooting = new CountDownLatch(1);
        if (ws != null && ws.isOpen()) close();
        log.info("Resetting connection to Codesignal");

        WebSocketFactory factory = new WebSocketFactory();
        String randomString = Randomizer.getAlphaNumericString(8);

        ws = factory.createSocket(
                String.format("wss://app.codesignal.com/sockjs/0/%s/websocket", randomString),
                TIMEOUT_IN_MS);
        ws.addHeader("User-Agent", Randomizer.getUserAgent());

        ws.addListener(new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                log.info("[WS] Connected");
            }

            @Override
            public void onTextMessage(WebSocket websocket, String message) {
                log.info("[WS-received] {}", message);
                if (message.equals("a[\"{\\\"msg\\\":\\\"ping\\\"}\"]")) {
                    millisecondsSinceEpoch = System.currentTimeMillis();
                    ws.sendText("[\"{\\\"msg\\\":\\\"pong\\\"}\"]");
                } else if (message.contains("a[\"{\\\"msg\\\":\\\"connected\\\"")) {
                    millisecondsSinceEpoch = System.currentTimeMillis();
                    isBooting.countDown();
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
                        exp.printStackTrace();
                        log.error(exp.getMessage());
                    }
                }
            }
        });

        // Connect to the server and perform an opening handshake.
        // This method blocks until the opening handshake is finished.
        ws.connect();
        ws.sendText("[\"{\\\"msg\\\":\\\"connect\\\",\\\"version\\\":\\\"1\\\"," +
                "\\\"support\\\":[\\\"1\\\",\\\"pre2\\\",\\\"pre1\\\"]}\"]");
        send(new GetServerTimeMessage());
        send(new LoginMessage(
                CODESIGNAL_USERS.get(wssCount.getAndIncrement() % CODESIGNAL_USERS.size()),
                DigestUtils.sha256Hex(System.getenv("USER_PASS")),
                "sha-256"
        ));
    }

    public void close() {
        ws.sendClose();
    }
}
