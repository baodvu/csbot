package com.codesignal.csbot.adapters.codesignal.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String msg;
    private String id;
    private JsonNode error;

    Message() {
        this("unknown", "0");
    }

    Message(String msg) {
        this(msg, "0");
    }

    private Message(String msg, String id) {
        this.msg = msg;
        this.id = id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JsonNode getError() {
        return error;
    }

    public void setError(JsonNode error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "Message{" +
                "msg='" + msg + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
