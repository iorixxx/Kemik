package edu.anadolu;

import java.nio.file.Path;

class Factory {

    static String repo(DocType type) {
        switch (type) {
            case Milliyet405bin:
                return "/Users/iorixxx/Documents/MilliyetCollectionZipFiles";
            case TTC3600:
                return "/Users/iorixxx/Desktop/TTC-3600-master/TTC-3600_Orj";
            case Kemik42bin:
                return "/Users/iorixxx/Downloads/42bin_haber/news";
            default:
                throw new AssertionError(type);
        }
    }

    static IDoc factory(DocType type, Path p) {
        switch (type) {
            case Milliyet405bin:
                return new Milliyet(p);
            case TTC3600:
                return new TTC3600(p);
            case Kemik42bin:
                return new Kemik(p);
            default:
                throw new AssertionError(type);
        }
    }

    static String[] categories(DocType type) {
        switch (type) {
            case Milliyet405bin:
                return Milliyet.categories;
            case TTC3600:
                return TTC3600.categories;
            case Kemik42bin:
                return Kemik.categories;
            default:
                throw new AssertionError(type);
        }
    }
}
