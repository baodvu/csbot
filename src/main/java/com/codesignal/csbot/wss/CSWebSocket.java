package com.codesignal.csbot.wss;

import com.codesignal.csbot.adapters.codesignal.message.Callback;
import com.codesignal.csbot.adapters.codesignal.message.Message;

public interface CSWebSocket {
    // Send a message with a callback
    void send(Message message, Callback callback);

    // Close the websocket
    void close();
}
