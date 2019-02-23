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
                .replaceAll("&lsquo;", "'")
                .replaceAll("&quot;", "\"")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&#226;", "a")

                // https://www.degraeve.com/reference/specialcharacters.php
                .replaceAll("&uuml;", "ü")
                .replaceAll("&ccedil;", "ç")

                .replaceAll("&ouml;", "ö")
                .replaceAll("&oacute;", "o")
                .replaceAll("&iacute;", "i")
                .replaceAll("&uacute;", "u")
                .replaceAll("&euml;", "e")
                .replaceAll("&ecirc;", "e")
                .replaceAll("&hellip;", "...")
                .replaceAll("&atilde;", "a")
                .replaceAll("&mdash;", "-")

                .replaceAll("&bull;", ".")
                .replaceAll("&middot;", ".")
                .replaceAll("&amp;", "&")



                .replaceAll("\\s+", " ")
                .trim();
    }
}
