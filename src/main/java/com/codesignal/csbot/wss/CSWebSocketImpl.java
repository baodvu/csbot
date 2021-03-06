package com.codesignal.csbot.wss;

import com.codesignal.csbot.adapters.codesignal.message.*;
import com.codesignal.csbot.models.CodesignalUser;
import com.codesignal.csbot.storage.Storage;
import com.codesignal.csbot.utils.Randomizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class CSWebSocketImpl implements CSWebSocket {
    private static final Logger log = LoggerFactory.getLogger(CSWebSocketImpl.class);
    private final static AtomicInteger wssCount = new AtomicInteger(0);

    private static final List<CodesignalUser> CODESIGNAL_USERS;
    public static final int MAX_CONCURRENT;
    static {
        Storage storage = new Storage();
        CODESIGNAL_USERS = storage.getAllUsers();
        MAX_CONCURRENT = CODESIGNAL_USERS.size();
    }

    private static final int TIMEOUT_IN_MS = 5000;
    private static final int LINE_LIMIT = 240;
    private final ConcurrentHashMap<Long, Callback> callbacks = new ConcurrentHashMap<>();
    private volatile AtomicInteger messageId;
    private volatile CountDownLatch isBooting;
    private volatile long millisecondsSinceEpoch = System.currentTimeMillis();

    private WebSocket ws;

    private WebSocketFactory factory = new WebSocketFactory();

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    public CSWebSocketImpl build() throws Exception {
        factory.setConnectionTimeout(TIMEOUT_IN_MS);
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
                log.error("Unable to reset connection: {}", exception.getMessage());
                return;
            }
        }

        try {
            if (!isBooting.await(15, TimeUnit.SECONDS)) {
                log.error("Time waiting for a connected signal from wss elapsed past 15 seconds.");
                return;
            }
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

    private void sendWithoutChecking(Message message, Callback callback) {
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

    private void sendWithoutChecking(Message message) {
        sendWithoutChecking(message, (ResultMessage result) -> {});
    }

    private void resetConnection() throws Exception {
        messageId = new AtomicInteger(0);
        isBooting = new CountDownLatch(1);
        if (ws != null && ws.isOpen()) close();
        log.info("Resetting connection to Codesignal");

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
                log.info(StringUtils.abbreviate("[WS-received] " + message, LINE_LIMIT));
                if (message.equals("a[\"{\\\"msg\\\":\\\"ping\\\"}\"]")) {
                    millisecondsSinceEpoch = System.currentTimeMillis();
                    ws.sendText("[\"{\\\"msg\\\":\\\"pong\\\"}\"]");
                } else if (message.equals("a[\"{\\\"msg\\\":\\\"pong\\\"}\"]")) {
                    millisecondsSinceEpoch = System.currentTimeMillis();
                } else if (message.startsWith("a[\"{\\\"msg\\\":\\\"connected\\\"")) {
                    millisecondsSinceEpoch = System.currentTimeMillis();
                    sendWithoutChecking(new GetServerTimeMessage());
                    CodesignalUser user = CODESIGNAL_USERS.get(wssCount.getAndIncrement() % CODESIGNAL_USERS.size());
                    sendWithoutChecking(new LoginMessage(
                            user.getEmail(),
                            DigestUtils.sha256Hex(user.getPass()),
                            "sha-256"
                    ));
                    isBooting.countDown();
                    service.scheduleWithFixedDelay(
                            () -> {
                                log.info("[WS-sending] ping");
                                ws.sendText("[\"{\\\"msg\\\":\\\"ping\\\"}\"]");
                            }, 30, 45, TimeUnit.SECONDS);
                } else if (message.startsWith("a[\"{\\\"msg\\\":\\\"result\\\"")) {
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
        log.info("Connect to the server and do a handshake");
        ws.connect();
        log.info("Send <connect> message");
        ws.sendText("[\"{\\\"msg\\\":\\\"connect\\\",\\\"version\\\":\\\"1\\\"," +
                "\\\"support\\\":[\\\"1\\\",\\\"pre2\\\",\\\"pre1\\\"]}\"]");
        log.info("<connect> sent");
    }

    public void close() {
        service.shutdown();
        ws.sendClose();
    }
}
