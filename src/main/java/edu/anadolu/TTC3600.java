package edu.anadolu;

import java.nio.file.Path;

public class TTC3600 extends Kemik {

    static final String[] categories = new String[]{"spor", "ekonomi", "kultursanat", "saglik", "teknoloji", "siyaset"};

    TTC3600(Path p) {
        super(p);
    }

    @Override
    public DocType type() {
        return DocType.TTC3600;
    }
}
