package com.codesignal.csbot.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageAbbreviation {
    private static List<String> rawLanguageList = List.of(
            "clj clojure",
            "coffee coffeescript",
            "lisp",
            "c clang",
            "cpp c++",
            "cs csharp c#",
            "d",
            "dart",
            "exs elixir",
            "erl erlang",
            "pas pascal freepascal",
            "for fortran",
            "fs f# fsharp",
            "go",
            "groovy",
            "hs haskell",
            "java",
            "js javascript",
            "jl julia",
            "kt kotlin",
            "lua",
            "nim",
            "objc objective-c objectivec",
            "ocaml",
            "octave gnuoctave",
            "perl",
            "php",
            "py python py2 python2 python2.7",
            "py3 python3",
            "r",
            "rb ruby",
            "rs rust",
            "scala",
            "st smalltalk",
            "swift swift4",
            "tcl",
            "ts typescript",
            "vb visualbasic");

    private static Map<String, String> toStandardized;
    static {
        toStandardized = new HashMap<>();
        for (String rawString: rawLanguageList) {
            String[] allAbbrevs = rawString.split(" ");
            String standard = allAbbrevs[0];
            toStandardized.put(standard, standard);
            for (int i = 1; i < allAbbrevs.length; ++i) {
                toStandardized.put(allAbbrevs[i], standard);
            }
        }
    }

    public static String toStandardized(String unstandardizedName) {
        return toStandardized.get(unstandardizedName.replace(" ", "").toLowerCase());
    }
}
