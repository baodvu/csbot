package com.codesignal.csbot.adapters.codesignal;

import com.codesignal.csbot.adapters.codesignal.message.Callback;
import com.codesignal.csbot.adapters.codesignal.message.Message;

public interface CodesignalClient {
    void send(Message message, Callback callback);
}
