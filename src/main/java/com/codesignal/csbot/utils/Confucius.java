package com.codesignal.csbot.utils;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Confucius {
    private List<String> sayings;
    private List<String> funny_sayings;

    public Confucius() {
        sayings = loadFile("confucius/sayings.txt");
        funny_sayings = loadFile("confucius/funny_sayings.txt");
    }

    private List<String> loadFile(String filePath) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(filePath);

        if (in == null) {
            return new ArrayList<>();
        }
        
        try {
            return Arrays.asList(IOUtils.toString(in, StandardCharsets.UTF_8).split("\\r?\\n"));
        } catch (IOException exp) {
            exp.printStackTrace();
            return Lists.newArrayList();
        }
    }

    public String getRandomSaying(boolean funny, boolean quoted) {
        List<String> targetList = funny ? funny_sayings : sayings;

        int index = (int) (targetList.size() * Math.random());
        if (quoted) {
            return String.format("As Confucius once said, '%s'", targetList.get(index));
        }
        return targetList.get(index);
    }
}
