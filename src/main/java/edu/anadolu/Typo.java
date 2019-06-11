package edu.anadolu;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static edu.anadolu.Factory.categories;

public class Typo {

    private static Map<String, String> typos;

    public static void main(String[] args) throws Exception {

        typos = Files.readAllLines(Paths.get("src/main/resources/turkish_typo.txt"), StandardCharsets.UTF_8)
                .stream()
                .map(String::trim)
                .filter(s -> !s.startsWith("#"))
                .map(s -> s.split("\t"))
                .collect(Collectors.toMap(p -> p[0], p -> p[1]));

        for (DocType type : DocType.values()) {
            System.out.println("processing " + type);
            list(type);
        }
    }

    private static void list(DocType type) throws Exception {

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(type.toString())));
        IndexSearcher searcher = new IndexSearcher(reader);


        List<Struct> list = new ArrayList<>();

        for (String term : typos.keySet()) {
            Term t = new Term("plain", term);
            TermStatistics stats = searcher.termStatistics(t, TermContext.build(reader.getContext(), t));
            Struct struct = new Struct(term, stats.totalTermFreq(), stats.docFreq());
            struct.sdf = startsWith(term, searcher);

            t = new Term("plain", typos.get(term));
            stats = searcher.termStatistics(t, TermContext.build(reader.getContext(), t));
            struct.correct = new Struct(typos.get(term), stats.totalTermFreq(), stats.docFreq());
            struct.correct.sdf = startsWith(typos.get(term), searcher);

            list.add(struct);
        }


        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString(), type.toString() + "_typo.txt"), StandardCharsets.UTF_8));
        out.println("typo\ttf\tdf\tsdf\tcorrect\ttf\tdf\tsdf");

        list.sort(Collections.reverseOrder(Comparator.comparingLong(Struct::sdf)));

        list.forEach(out::println);

        out.flush();
        out.close();
        list.clear();

        for (String category : categories(type)) {

            final Query filter = new TermQuery(new Term("category", category));

            list = new ArrayList<>();


            for (String term : typos.keySet()) {


                Struct struct = ttf(reader, searcher, category, term);
                struct.sdf = startsWith(term, filter, searcher);

                if (struct.sdf < struct.df)
                    throw new RuntimeException(term + ": " + struct.sdf + " " + struct.df);

                struct.correct = ttf(reader, searcher, category, typos.get(term));
                struct.correct.sdf = startsWith(typos.get(term), filter, searcher);
                list.add(struct);

            }

            final PrintWriter o = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString(), category + "_typo.txt"), StandardCharsets.UTF_8));
            o.println("typo\ttf\tdf\tsdf\tcorrect\ttf\tdf\tsdf");

            list.sort(Collections.reverseOrder(Comparator.comparingLong(Struct::sdf)));

            list.forEach(o::println);

            o.flush();
            o.close();
        }
        reader.close();
    }

    private static int startsWith(String term, final Query filter, IndexSearcher searcher) throws IOException {
        WildcardQuery query = new WildcardQuery(new Term("plain", term + "*"));

        BooleanQuery bq = new BooleanQuery.Builder()
                .add(query, BooleanClause.Occur.MUST)
                .add(filter, BooleanClause.Occur.FILTER)
                .build();

        return searcher.count(bq);
    }

    private static int startsWith(String term, IndexSearcher searcher) throws IOException {
        WildcardQuery query = new WildcardQuery(new Term("plain", term + "*"));
        return searcher.count(query);
    }

    private static Struct ttf(IndexReader reader, IndexSearcher searcher, String category, String term) throws IOException {

        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, "plain", new Term("plain", term).bytes());

        if (postingsEnum == null)
            return new Struct(term, 0, 0);


        int tf = 0;
        int df = 0;
        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            Document doc = searcher.doc(postingsEnum.docID());

            if (doc.get("category").equals(category)) {
                tf += postingsEnum.freq();
                df++;
            }
        }
        return new Struct(term, tf, df);
    }

    static class Struct {
        final String term;
        final long tf;
        final long df;

        Struct(String term, long tf, long df) {
            this.term = term;
            this.tf = tf;
            this.df = df;
        }

        long df() {
            return df;
        }

        long sdf() {
            return sdf;
        }

        Struct correct;

        long sdf;

        @Override
        public String toString() {
            return term + "\t" + tf + "\t" + df + "\t" + sdf + (correct == null ? "" : "\t" + correct.toString());
        }
    }
}
