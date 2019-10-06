package com.codesignal.csbot.adapters.codesignal.message;


import com.fasterxml.jackson.databind.JsonNode;

public class ResultMessage extends Message {
    private JsonNode result;

    public ResultMessage() {
        this(null);
    }

    public ResultMessage(JsonNode result) {
        super("result");
        this.result = result;
    }

    public JsonNode getResult() {
        return result;
    }

    public void setResult(JsonNode result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return super.toString() + "\nResultMessage{" +
                "result=" + result +
                '}';
    }
}
