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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static edu.anadolu.IndexCompoundWords.analyzerWrapper;

/**
 * Hello world!
 */
public class App {
    private static List<String> readAllLines(Path p) {
        try {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void main(String[] args) throws Exception {

        Stream<Path> stream = Files.find(Paths.get("/Users/iorixxx/Downloads/42bin_haber/news"), 3, matcher);

        PrintStream fileStream = new PrintStream(Files.newOutputStream(Paths.get("/Users/iorixxx/Desktop/42bin_haber.arff")));

        fileStream.println("@RELATION 42bin");

        fileStream.println("@ATTRIBUTE class {dunya,guncel,planet,spor,yasam,ekonomi,kultur-sanat,saglik,teknoloji,genel,magazin,siyaset,turkiye}");

        fileStream.println("@ATTRIBUTE content string");

        fileStream.println("@data");


        stream.forEach(p -> {

            String category = p.getParent().getFileName().toString();

            StringBuilder builder = new StringBuilder();

            for (String line : readAllLines(p)) {

                builder.append(line).append("\n");
            }

            String content = weka.core.Utils.quote(builder.toString().trim());

            fileStream.println(category + "," + content);


        });

        fileStream.flush();
        fileStream.close();

        index();

    }

    static BiPredicate<Path, BasicFileAttributes> matcher = (Path p, BasicFileAttributes att) -> {

        if (!att.isRegularFile()) return false;

        Path name = p.getFileName();

        return (name != null && name.toString().endsWith(".txt"));

    };


    static void index() throws IOException {

        Directory dir = FSDirectory.open(Paths.get("KemikIndex"));
        IndexWriterConfig iwc = new IndexWriterConfig(analyzerWrapper());

        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(256.0);

        IndexWriter writer = new IndexWriter(dir, iwc);


        Stream<Path> stream = Files.find(Paths.get("/Users/iorixxx/Downloads/42bin_haber/news"), 3, matcher);

        stream.parallel().forEach(p -> {

            String category = p.getParent().getFileName().toString();

            String pk = category + "_" + p.getFileName();

            pk = pk.substring(0, pk.length() - 4);

            Document doc = new Document();

            doc.add(new StringField("id", pk, Field.Store.YES));

            doc.add(new StringField("category", category, Field.Store.YES));

            StringBuilder builder = new StringBuilder();
            for (String line : readAllLines(p)) {
                builder.append(line).append("\n");
            }

            String content = builder.toString().trim();

            try {
                doc.add(new TextField("shingle", content, Field.Store.NO));
                doc.add(new TextField("plain", content, Field.Store.NO));
                writer.addDocument(doc);

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

        });

        try {
            writer.commit();
            writer.forceMerge(1);
        } finally {
            writer.close();
        }
    }
}
