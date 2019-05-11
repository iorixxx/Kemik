package edu.anadolu;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

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


        System.out.println(typos);

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

            Map<String, Integer> map = new HashMap<>();
            Map<String, Integer> correct = new HashMap<>();

            for (String term : typos.keySet())
                map.put(term.trim(), 0);

            for (String term : map.keySet()) {
                term = term.trim();

                Query query = new TermQuery(new Term("plain", term));

                BooleanQuery bq = new BooleanQuery.Builder()
                        .add(query, BooleanClause.Occur.MUST)
                        .add(filter, BooleanClause.Occur.FILTER)
                        .build();

                int docFreq = searcher.count(bq);
                map.put(term, docFreq);

                query = new TermQuery(new Term("plain", typos.get(term)));

                bq = new BooleanQuery.Builder()
                        .add(query, BooleanClause.Occur.MUST)
                        .add(filter, BooleanClause.Occur.FILTER)
                        .build();

                correct.put(typos.get(term), searcher.count(bq));

            }

            final PrintWriter o = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString(), category + "_typo.txt"), StandardCharsets.UTF_8));
            o.println("typo\tdf\tcorrect\tdf");
            map.entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .forEach((e) -> o.println(e.getKey() + "\t" + e.getValue() + "\t" + typos.get(e.getKey()) + "\t" + correct.get(typos.get(e.getKey()))));

            o.flush();
            o.close();
            map.clear();
        }
        reader.close();
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
