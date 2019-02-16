package edu.anadolu;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Milliyet implements IDoc {

    static final String[] categories = new String[]{"yasam", "siyaset", "yazar", "spor", "magazin", "dunya", "ekonomi", "guncel"};

    private String title;
    private String url;
    private String text;
    private String id;

    public Milliyet(Path p) {

        try {
            byte[] encoded = Files.readAllBytes(p);
            String s = new String(encoded, StandardCharsets.UTF_8);
            id = StringUtils.substringBetween(s, "<DOCNO>", "</DOCNO>").trim();
            url = StringUtils.substringBetween(s, "<URL>", "</URL>").trim();
            title = StringUtils.substringBetween(s, "<HEADLINE>", "</HEADLINE>").trim();
            text = StringUtils.substringBetween(s, "<TEXT>", "</TEXT>").trim();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public String content() {
        return title + " " + text;
    }

    public String id() {
        return id;
    }

    public String category() {
        String first = StringUtils.substringBeforeLast(url, "/");
        String second = StringUtils.substringAfterLast(first, "/");

        if (!"son".equals(second))
            return second;
        // else if ("son".equals(second)) return second;

        String t = StringUtils.substringAfterLast(url, "/");

        t = StringUtils.substringBeforeLast(t, ".htm");

        if (!t.startsWith("son")) throw new RuntimeException("does not start with son " + t);
        t = t.substring(3);

        for (int i = t.length() - 1; i >= 0; i--) {

            final char c = t.charAt(i);

            if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9')
                continue;

            t = t.substring(0, i + 1);

            for (String cat : categories)
                if (cat.startsWith(t)) {
                    return cat;
                }

            if ("tur".equals(t))
                return "turkiye";
            else
                throw new RuntimeException("cannot understand " + t);

        }

        throw new RuntimeException("cannot resolve " + t);

    }

    @Override
    public DocType type() {
        return DocType.Milliyet405bin;
    }

}
