package com.codesignal.csbot.utils;

public class Randomizer {
    public static String getAlphaNumericString(int n) {
        String characterSet  = "0123456789abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index = (int) (characterSet.length() * Math.random());
            sb.append(characterSet.charAt(index));
        }

        return sb.toString();
    }
}
