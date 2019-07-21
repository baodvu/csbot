package com.codesignal.csbot.adapters.codesignal.message;

import java.util.List;

public class MethodMessage extends Message {
    private String method;
    private List<Object> params;

    MethodMessage(String method, List<Object> params) {
        super("method");
        this.method = method;
        this.params = params;
    }

    MethodMessage(String method) {
        super("method");
        this.method = method;
        this.params = List.of();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }
}
