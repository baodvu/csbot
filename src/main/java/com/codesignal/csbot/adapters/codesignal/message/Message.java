package com.codesignal.csbot.adapters.codesignal.message;


public class Message {
    private String msg;
    private String id;

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

    @Override
    public String toString() {
        return "Message{" +
                "msg='" + msg + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
