package edu.anadolu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

class Factory {

    static String repo(DocType type) {

        Properties properties = readProperties();

        if (properties.getProperty(type.name()) != null)
            return properties.getProperty(type.name());

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

    private static Properties readProperties() {

        Properties prop = new Properties();

        Path path = Paths.get("config.properties");

        if (Files.exists(path) && Files.isReadable(path))

            try (InputStream input = Files.newInputStream(Paths.get("config.properties"))) {
                prop.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        else
            try (InputStream input = Factory.class.getClassLoader().getResourceAsStream("config.properties")) {

                if (input == null) {
                    System.out.println("Sorry, unable to find config.properties in class path");
                    return null;
                }
                prop.load(input);

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        return prop;
    }
}
