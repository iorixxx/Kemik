package edu.anadolu;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Stream;

/**
 * Hello world!
 *
 */
public class App 
{
    private static List<String> readAllLines(Path p) {
        try {
            return Files.readAllLines(p);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void main(String[] args) throws Exception {

        Stream<Path> stream = Files.find(Paths.get("/Users/iorixxx/Downloads/42bin_haber/news"), 3, (Path p, BasicFileAttributes att) -> {

            if (!att.isRegularFile()) return false;

            Path name = p.getFileName();

            return (name != null && name.toString().endsWith(".txt"));

        });


        PrintStream fileStream = new PrintStream(Files.newOutputStream(Paths.get("/Users/iorixxx/Desktop/42bin_haber.arff")));


        fileStream.println("@RELATION 42bin");

        fileStream.println("@ATTRIBUTE pk string");

        fileStream.println("@ATTRIBUTE class {dunya,guncel,planet,spor,yasam,ekonomi,kultur-sanat,saglik,teknoloji,genel,magazin,siyaset,turkiye}");

        fileStream.println("@ATTRIBUTE content string");

        fileStream.println("@data");


        stream.forEach(p -> {

            String category = p.getParent().getFileName().toString();

            String pk = category + "_" + p.getFileName();

            pk = pk.substring(0, pk.length() - 4);


            StringBuilder builder = new StringBuilder();

            for (String line : readAllLines(p)) {

                builder.append(line).append("\n");
            }

            String content = weka.core.Utils.quote(builder.toString().trim());

            fileStream.println(pk + "," + category + "," + content);


        });

        fileStream.close();


    }
}
