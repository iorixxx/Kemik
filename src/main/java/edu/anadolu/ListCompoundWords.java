/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.anadolu;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.regex.Pattern;

import static edu.anadolu.Factory.categories;

public class ListCompoundWords {

    public static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");

    private static TermStatistics[] highFreqTerms(int numTerms, Comparator<TermStatistics> comparator, DocType type) throws Exception {

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(type.toString())));
        TermStatistics[] terms = getHighFreqTerms(reader, numTerms, "shingle", comparator);
        reader.close();

        return terms;
    }

    public static void main(String[] args) throws Exception {

        for (DocType type : DocType.values()) {
            System.out.println("processing " + type);
            list(type);
        }
    }

    private static void write(PrintWriter out, TermStatistics stat) {
        out.print("\t");
        out.print(stat.totalTermFreq());
        out.print("\t");
        out.print(stat.docFreq());
    }

    private static void list(DocType type) throws Exception {

        TermStatistics[] terms = highFreqTerms(10000000, new DocFreqComparator(), type);

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(type.toString())));

        IndexSearcher searcher = new IndexSearcher(reader);

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString(), type.toString() + ".txt"), StandardCharsets.UTF_8));


        for (TermStatistics term : terms) {

            String[] parts = whiteSpaceSplitter.split(term.term().utf8ToString());
            String merged = String.join("", parts);

            if (isNumeric(merged)) continue;

            Term t = new Term("plain", merged);

            TermStatistics mergedStat = searcher.termStatistics(t, TermContext.build(reader.getContext(), t));

          /*
            Query query = new TermQuery(t);
            BooleanQuery bq = new BooleanQuery.Builder().add(query, BooleanClause.Occur.MUST).build();
            int df = searcher.count(bq);
            */

            if (mergedStat.docFreq() == 0) continue;

            out.print(String.join("_", parts));
            System.out.println(String.join("_", parts));

            for (String s : parts) {
                Term part = new Term("plain", s);
                TermStatistics stat = searcher.termStatistics(part, TermContext.build(reader.getContext(), part));
                write(out, stat);
            }

            write(out, term);
            write(out, mergedStat);
            // out.print(term.term().utf8ToString().replaceAll(" ", "_") + "\t" + term.totalTermFreq() + "\t" + term.docFreq() + "\t" + mergedStat.totalTermFreq() + "\t" + mergedStat.docFreq());

            out.println();
        }

        out.flush();
        out.close();

        for (String category : categories(type)) {

            out = new PrintWriter(Files.newBufferedWriter(Paths.get(type.toString(), category + ".txt"), StandardCharsets.UTF_8));

            final Query filter = new TermQuery(new Term("category", category));
            for (TermStatistics term : terms) {


                String[] parts = whiteSpaceSplitter.split(term.term().utf8ToString());
                String merged = String.join("", parts);

                if (isNumeric(merged)) continue;
                Query query = new TermQuery(new Term("plain", merged));

                BooleanQuery bq = new BooleanQuery.Builder()
                        .add(query, BooleanClause.Occur.MUST)
                        .add(filter, BooleanClause.Occur.FILTER)
                        .build();

                int docFreq = searcher.count(bq);

                if (docFreq == 0) continue;

                out.print(String.join("_", parts));
                write(out, term);
                out.print("\t");
                out.print(docFreq);
                out.println();

            }

            out.flush();
            out.close();
        }
        reader.close();
    }

    private static boolean isNumeric(String strNum) {
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Returns TermStats[] ordered by the specified comparator
     */
    private static TermStatistics[] getHighFreqTerms(IndexReader reader, int numTerms, String field, Comparator<TermStatistics> comparator) throws Exception {
        TermStatsQueue tiq;

        if (field != null) {
            Terms terms = MultiFields.getTerms(reader, field);
            if (terms == null) {
                throw new RuntimeException("field " + field + " not found");
            }

            TermsEnum termsEnum = terms.iterator();
            tiq = new TermStatsQueue(numTerms, comparator);
            tiq.fill(termsEnum);
        } else {
            Fields fields = MultiFields.getFields(reader);
            if (fields.size() == 0) {
                throw new RuntimeException("no fields found for this index");
            }
            tiq = new TermStatsQueue(numTerms, comparator);
            for (String fieldName : fields) {
                Terms terms = fields.terms(fieldName);
                if (terms != null) {
                    tiq.fill(terms.iterator());
                }
            }
        }

        TermStatistics[] result = new TermStatistics[tiq.size()];
        // we want highest first so we read the queue and populate the array
        // starting at the end and work backwards
        int count = tiq.size() - 1;
        while (tiq.size() != 0) {
            result[count] = tiq.pop();
            count--;
        }
        return result;
    }

    /**
     * Compares terms by docTermFreq
     */
    public static final class DocFreqComparator implements Comparator<TermStatistics> {

        @Override
        public int compare(TermStatistics a, TermStatistics b) {
            int res = Long.compare(a.docFreq(), b.docFreq());
            if (res == 0) {
                res = a.term().compareTo(b.term());
            }
            return res;
        }
    }

    /**
     * Compares terms by totalTermFreq
     */
    public static final class TotalTermFreqComparator implements Comparator<TermStatistics> {

        @Override
        public int compare(TermStatistics a, TermStatistics b) {
            int res = Long.compare(a.totalTermFreq(), b.totalTermFreq());
            if (res == 0) {
                res = a.term().compareTo(b.term());
            }
            return res;
        }
    }

    /**
     * Priority queue for TermStats objects
     **/
    static final class TermStatsQueue extends PriorityQueue<TermStatistics> {
        final Comparator<TermStatistics> comparator;

        TermStatsQueue(int size, Comparator<TermStatistics> comparator) {
            super(size);
            this.comparator = comparator;
        }

        @Override
        protected boolean lessThan(TermStatistics termInfoA, TermStatistics termInfoB) {
            return comparator.compare(termInfoA, termInfoB) < 0;
        }

        void fill(TermsEnum termsEnum) throws IOException {
            BytesRef term;
            while ((term = termsEnum.next()) != null) {
                insertWithOverflow(new TermStatistics(BytesRef.deepCopyOf(term), termsEnum.docFreq(), termsEnum.totalTermFreq()));
            }
        }
    }
}
