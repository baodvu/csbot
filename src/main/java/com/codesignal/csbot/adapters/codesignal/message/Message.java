package com.codesignal.csbot.adapters.codesignal.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String msg;
    private String id;
    private JsonNode error;

    public Message(String msg) {
        this.msg = msg;
        this.id = "0";
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
