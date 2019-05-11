package edu.anadolu;

import java.nio.file.Path;

class AOF extends Kemik {

    static final String[] categories = new String[]{"felsefe", "sosyoloji", "lojistik", "ilahiyat"};

    private final Path p;

    AOF(Path p) {
        super(p);
        this.p = p;
    }

    @Override
    public String id() {
        String fileName = p.getFileName().toString();
        return fileName.substring(0, fileName.length() - 4);
    }

    @Override
    public String category() {
        String fileName = p.getFileName().toString();
        return fileName.substring(3);
    }
}
