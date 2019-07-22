package com.codesignal.csbot.adapters.codesignal.message.userservice;

import com.codesignal.csbot.adapters.codesignal.message.MethodMessage;

import java.util.List;

public class GetMultipleWithVisibleFieldsMessage extends MethodMessage {
    public GetMultipleWithVisibleFieldsMessage(List<String> userIds) {
        super("userService.getMultipleWithVisibleFields", List.of(userIds));
    }
}

