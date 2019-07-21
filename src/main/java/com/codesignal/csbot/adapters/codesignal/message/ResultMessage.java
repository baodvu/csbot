package com.codesignal.csbot.adapters.codesignal.message;


public class ResultMessage extends Message {
    private Object result;

    public ResultMessage() {
        this(null);
    }

    public ResultMessage(Object result) {
        super("result");
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return super.toString() + "\nResultMessage{" +
                "result=" + result +
                '}';
    }
}
