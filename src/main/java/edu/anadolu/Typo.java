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

            t = new Term("plain", typos.get(term));
            stats = searcher.termStatistics(t, TermContext.build(reader.getContext(), t));
            struct.correct = new Struct(typos.get(term), stats.totalTermFreq(), stats.docFreq());

            list.add(struct);
        }


        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString(), type.toString() + "_typo.txt"), StandardCharsets.UTF_8));
        out.println("typo\ttf\tdf\tcorrect\ttf\tdf");

        list.sort(Collections.reverseOrder(Comparator.comparingLong(Struct::df)));

        list.forEach(out::println);

        out.flush();
        out.close();

        for (String category : categories(type)) {

            final Query filter = new TermQuery(new Term("category", category));

            list = new ArrayList<>();


            for (String term : typos.keySet()) {


                Query query = new TermQuery(new Term("plain", term));

                BooleanQuery bq = new BooleanQuery.Builder()
                        .add(query, BooleanClause.Occur.MUST)
                        .add(filter, BooleanClause.Occur.FILTER)
                        .build();

                int df = searcher.count(bq);
                long tf = ttf(reader, searcher, category, term);
                Struct struct = new Struct(term, tf, df);

                System.out.println("df=" + df);


                query = new TermQuery(new Term("plain", typos.get(term)));

                bq = new BooleanQuery.Builder()
                        .add(query, BooleanClause.Occur.MUST)
                        .add(filter, BooleanClause.Occur.FILTER)
                        .build();

                struct.correct = new Struct(typos.get(term), ttf(reader, searcher, category, typos.get(term)), searcher.count(bq));
                list.add(struct);

            }

            final PrintWriter o = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString(), category + "_typo.txt"), StandardCharsets.UTF_8));
            o.println("typo\ttf\tdf\tcorrect\ttf\tdf");

            list.sort(Collections.reverseOrder(Comparator.comparingLong(Struct::df)));

            list.forEach(o::println);

            o.flush();
            o.close();
        }
        reader.close();
    }

    private static int ttf(IndexReader reader, IndexSearcher searcher, String category, String term) throws IOException {

        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, "pain", new Term("plain", term).bytes());

        if (postingsEnum == null) return 0;

        int freq = 0;
        int df = 0;
        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            Document doc = searcher.doc(postingsEnum.docID());

            if (doc.get("category").equals(category)) {
                freq += postingsEnum.freq();
                df++;
            }
        }

        System.out.println("df=" + df);
        return freq;
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

        Struct correct;


        @Override
        public String toString() {
            return term + "\t" + tf + "\t" + df + (correct == null ? "" : "\t" + correct.toString());
        }
    }
}
