package edu.anadolu;

import java.nio.file.Path;

class AOF extends Kemik {

    static final String[] categories = new String[]{"felsefe", "sosyoloji", "lojistik", "ilahiyat"};

    AOF(Path p) {
        super(p);
    }

    public String id() {
        return super.id().replaceAll(" ", "_");
    }

    public String category() {
        return super.category().replaceAll(" ", "_");
    }
}
