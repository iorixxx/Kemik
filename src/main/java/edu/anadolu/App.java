package edu.anadolu;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static edu.anadolu.Analyzers.analyzerWrapper;
import static edu.anadolu.Factory.*;

public class App {

    public static void main(String[] args) throws Exception {

        for (DocType type : DocType.values()) {
            arrf(type);
            analyzedArrf(type);
            index(type);
        }
    }


    private static void arrf(DocType type) throws Exception {

        Stream<Path> stream = Files.find(Paths.get(repo(type)), 3, matcher);

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString() + ".arff"), StandardCharsets.UTF_8));

        out.println("@RELATION " + type.toString());

        out.println("@ATTRIBUTE class {" + String.join(",", categories(type)) + "}");

        out.println("@ATTRIBUTE content string");

        out.println("@data");


        final Map<String, Integer> categories = new HashMap<>();

        stream.forEach(p -> {

            IDoc iDoc = factory(type, p);

            String category = iDoc.category();

            int count = categories.getOrDefault(category, 0);
            categories.put(category, ++count);

            if (DocType.Milliyet405bin.equals(type) && ("astro".equals(category) || "tv".equals(category) || "sanat".equals(category))) {
                return;
            }

            String content = weka.core.Utils.quote(iDoc.content().trim());
            out.println(category + "," + content);

        });

        out.flush();
        out.close();
        categories.entrySet().forEach(System.out::println);

    }

    private static void analyzedArrf(DocType type) throws Exception {

        Stream<Path> stream = Files.find(Paths.get(repo(type)), 3, matcher);

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString() + "_plain.arff"), StandardCharsets.UTF_8));

        out.println("@RELATION " + type.toString() + "Plain");

        out.println("@ATTRIBUTE class {" + String.join(",", categories(type)) + "}");

        out.println("@ATTRIBUTE content string");

        out.println("@data");


        final Map<String, Integer> categories = new HashMap<>();

        stream.forEach(p -> {

            IDoc iDoc = factory(type, p);

            String category = iDoc.category();

            int count = categories.getOrDefault(category, 0);
            categories.put(category, ++count);

            if (DocType.Milliyet405bin.equals(type) && ("astro".equals(category) || "tv".equals(category) || "sanat".equals(category))) {
                return;
            }

            String content = weka.core.Utils.quote(Analyzers.getAnalyzedString(iDoc.content(), Analyzers.plain()));
            out.println(category + "," + content);

        });

        out.flush();
        out.close();
        categories.entrySet().forEach(System.out::println);
    }


    private static BiPredicate<Path, BasicFileAttributes> matcher = (Path p, BasicFileAttributes att) -> {

        if (!att.isRegularFile()) return false;

        Path name = p.getFileName();

        return (name != null && name.toString().endsWith(".txt"));

    };


    private static void index(DocType type) throws IOException {

        Directory dir = FSDirectory.open(Paths.get(type.toString()));
        IndexWriterConfig iwc = new IndexWriterConfig(analyzerWrapper());

        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(256.0);

        IndexWriter writer = new IndexWriter(dir, iwc);


        Stream<Path> stream = Files.find(Paths.get(repo(type)), 3, matcher);

        stream.parallel().forEach(p -> {

            IDoc iDoc = factory(type, p);

            String category = iDoc.category();
            if (DocType.Milliyet405bin.equals(type) && ("astro".equals(category) || "tv".equals(category) || "sanat".equals(category))) {
                return;
            }

            Document doc = new Document();

            doc.add(new StringField("id", iDoc.id(), Field.Store.YES));
            doc.add(new StringField("category", category, Field.Store.YES));

            String content = iDoc.content();

            try {
                doc.add(new TextField("shingle", content, Field.Store.NO));
                doc.add(new TextField("plain", content, Field.Store.NO));
                writer.addDocument(doc);

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

        });

        int numDocs;
        try {
            writer.commit();
            writer.forceMerge(1);
            numDocs = writer.numDocs();
        } finally {
            writer.close();
        }

        System.out.println(numDocs + " indexed.");
    }
}
