package com.codesignal.csbot.adapters.codesignal;

import com.codesignal.csbot.adapters.codesignal.message.Callback;
import com.codesignal.csbot.adapters.codesignal.message.Message;
import com.codesignal.csbot.adapters.codesignal.message.ResultMessage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface CodesignalClient {
    void send(Message message, Callback callback);

    // Not currently usable. There's some bug making it very inefficient.
    @Deprecated
    ResultMessage send(Message message, int timeout)
            throws InterruptedException, ExecutionException, TimeoutException;
}
