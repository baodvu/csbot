package com.codesignal.csbot.adapters.codesignal;

import com.codesignal.csbot.adapters.codesignal.message.Callback;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;

import java.util.concurrent.CompletableFuture;

public interface CodesignalClient {
    void send(Message message, Callback callback);

    CompletableFuture<ResultMessage> send(Message message);
}
