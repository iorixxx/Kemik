package edu.anadolu;

import java.nio.file.Path;
import java.util.Locale;

public class TRT extends Milliyet {

    static final String[] categories = new String[]{"dunya", "kultursanat", "spor", "ekonomi", "saglik", "turkiye"};

    private final Path p;

    TRT(Path p) {
        super(p);
        this.p = p;

       // String pk = category() + "_" + p.getFileName();
       // String id = pk.substring(0, pk.length() - 4);

      //  if (id.equals(id()))
       //     throw new RuntimeException("ids are not equal");
    }

    @Override
    public String id() {
        String pk = category() + "_" + p.getFileName();
        return pk.substring(0, pk.length() - 4);
    }

    @Override
    public String category() {
        return p.getParent().getFileName().toString().toLowerCase(Locale.US);
    }
}
