package edu.anadolu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Kemik implements IDoc {

    static final String[] categories = new String[]{"dunya", "guncel", "planet", "spor", "yasam", "ekonomi", "kultur-sanat", "saglik", "teknoloji", "genel", "magazin", "siyaset", "turkiye"};

    private String content;
    private String category;
    private String id;

    Kemik(Path p) {

        try {
            category = p.getParent().getFileName().toString();
            String pk = category + "_" + p.getFileName();
            id = pk.substring(0, pk.length() - 4);
            byte[] encoded = Files.readAllBytes(p);
            this.content = new String(encoded, StandardCharsets.UTF_8)
                    .replaceAll("\u2019", "'")
                    .replaceAll("\\s+", " ")
                    .trim();

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public String content() {
        return content;
    }

    public String id() {
        return id;
    }

    public String category() {
        return category;
    }

}
