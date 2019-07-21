package com.codesignal.csbot.wss;

import com.codesignal.csbot.adapters.codesignal.message.Callback;
import com.codesignal.csbot.adapters.codesignal.message.Message;

public interface CSWebSocket {
    // Send a message with a callback
    void send(Message message, Callback callback);

    // Send a message with no callback (note that the message could fail to send and we won't do anything)
    void send(Message message);
}
