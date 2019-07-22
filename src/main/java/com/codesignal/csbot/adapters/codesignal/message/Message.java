package com.codesignal.csbot.adapters.codesignal.message;


import java.util.Map;

public class Message {
    private String msg;
    private String id;
    private Map<String, Object> error;

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

    public Map<String, Object> getError() {
        return error;
    }

    public void setError(Map<String, Object> error) {
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
