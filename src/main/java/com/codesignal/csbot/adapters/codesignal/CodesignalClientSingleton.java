package com.codesignal.csbot.adapters.codesignal;


public class CodesignalClientSingleton {
    private static volatile CodesignalClient instance = null;
    private static final Object mutex = new Object();

    private CodesignalClientSingleton() {
    }

    public static CodesignalClient getInstance() {
        CodesignalClient result = instance;
        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null)
                    instance = result = new CodesignalClientImpl();
            }
        }
        return result;
    }
}
