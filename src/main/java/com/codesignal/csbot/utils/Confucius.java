package com.codesignal.csbot.utils;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Confucius {
    private List<String> sayings;
    private List<String> funny_sayings;

    public Confucius() {
        sayings = loadFile("confucius/sayings.txt");
        funny_sayings = loadFile("confucius/funny_sayings.txt");
    }

    private List<String> loadFile(String filePath) {
        URL fileUrl = getClass().getClassLoader().getResource(filePath);
        if (fileUrl == null) {
            return Lists.newArrayList();
        }

        File file = new File(fileUrl.getFile());

        try (Stream<String> lines = Files.lines(file.toPath())) {
            return lines.collect(Collectors.toList());
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
