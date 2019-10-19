package com.codesignal.csbot.adapters.codesignal;

import com.codesignal.csbot.adapters.codesignal.message.Callback;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;
import com.codesignal.csbot.wss.CSWebSocket;
import com.codesignal.csbot.wss.CSWebSocketImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;


class CodesignalClientImpl implements CodesignalClient {
    private static final Logger log = LoggerFactory.getLogger(CodesignalClientImpl.class);
    private CSWebSocket connection = null;

    CodesignalClientImpl() {
        try {
            this.connection = new CSWebSocketImpl().build();
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error("Unable to establish connection to CodeSignal");
        }
    }

    @Override
    public void send(Message message, Callback callback) {
        connection.send(message, callback);
    }

    @Override
    public CompletableFuture<ResultMessage> send(Message message) {
        CompletableFuture<ResultMessage> completableFuture = new CompletableFuture<>();
        connection.send(message, completableFuture::complete);
        return completableFuture;
    }
}