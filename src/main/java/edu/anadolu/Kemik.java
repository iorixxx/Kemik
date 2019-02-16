package edu.anadolu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Kemik implements IDoc {

    static final String[] categories = new String[]{"dunya", "guncel", "planet", "spor", "yasam", "ekonomi", "kultur-sanat", "saglik", "teknoloji", "genel", "magazin", "siyaset", "turkiye"};

    private String content;
    private String category;
    private String id;

    public Kemik(Path p) {

        try {
            category = p.getParent().getFileName().toString();
            String pk = category + "_" + p.getFileName();
            id = pk.substring(0, pk.length() - 4);

            StringBuilder builder = new StringBuilder();

            for (String line : Files.readAllLines(p)) {
                line = line.trim();
                if (line.length() == 0) continue;
                builder.append(line).append("\n");
            }
            this.content = builder.toString().trim();

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

    @Override
    public DocType type() {
        return DocType.Kemik42bin;
    }

}
