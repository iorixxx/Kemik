package edu.anadolu;

public interface IDoc {

    String content();

    String id();

    String category();

    default String normalize(String s) {
        return s.replaceAll("\u2019", "'")
                .replaceAll("\u2018", "'")
                .replaceAll("\u02BC", "'")
                .replaceAll("`", "'")
                .replaceAll("´", "'")

                .replaceAll("&rsquo;", "'")
                .replaceAll("&quot;", "\"")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&#226;", "a")

                .replaceAll("\\s+", " ")
                .trim();
    }
}
