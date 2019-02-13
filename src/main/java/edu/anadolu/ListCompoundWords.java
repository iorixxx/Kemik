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

/**
 * <code>ListCompoundWords</code> class extracts the top n most frequent terms
 * (by document frequency) from an existing Lucene index and reports their
 * document frequency.
 * <p>
 * If the -t flag is given, both document frequency and total tf (total
 * number of occurrences) are reported, ordered by descending total tf.
 */
public class ListCompoundWords {


    private static TermStats[] highFreqTerms(int numTerms, Comparator<TermStats> comparator) throws Exception {

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("KemikIndex")));
        TermStats[] terms = getHighFreqTerms(reader, numTerms, "shingle", comparator);
        reader.close();

        return terms;
    }

    public static void main(String[] args) throws Exception {

        TermStats[] terms = highFreqTerms(10000000, new DocFreqComparator());

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("KemikIndex")));

        IndexSearcher searcher = new IndexSearcher(reader);

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get("kemik42bin.txt"), StandardCharsets.UTF_8));


        for (TermStats term : terms) {

            String merged = term.termtext.utf8ToString().replaceAll(" ", "");
            if (isNumeric(merged)) continue;
            Query query = new TermQuery(new Term("plain", merged));

            BooleanQuery bq = new BooleanQuery.Builder().add(query, BooleanClause.Occur.MUST).build();

            int count = searcher.count(bq);

            if (count == 0) continue;

            out.println(term.termtext.utf8ToString().replaceAll(" ", "_") + "\t" + term.totalTermFreq + "\t" + term.docFreq + "\t" + count);

        }

        out.flush();
        out.close();

        for (String category : new String[]{"dunya", "guncel", "planet", "spor", "yasam", "ekonomi", "kultur-sanat", "saglik", "teknoloji", "genel", "magazin", "siyaset", "turkiye"}) {

            out = new PrintWriter(Files.newBufferedWriter(Paths.get(category + ".txt"), StandardCharsets.UTF_8));

            final Query filter = new TermQuery(new Term("category", category));
            for (TermStats term : terms) {


                String merged = term.termtext.utf8ToString().replaceAll(" ", "");

                if (isNumeric(merged)) continue;
                Query query = new TermQuery(new Term("plain", merged));

                BooleanQuery bq = new BooleanQuery.Builder()
                        .add(query, BooleanClause.Occur.MUST)
                        .add(filter, BooleanClause.Occur.FILTER)
                        .build();

                int count = searcher.count(bq);

                if (count == 0) continue;

                out.println(term.termtext.utf8ToString().replaceAll(" ", "_") + "\t" + term.totalTermFreq + "\t" + term.docFreq + "\t" + count);

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
    public static TermStats[] getHighFreqTerms(IndexReader reader, int numTerms, String field, Comparator<TermStats> comparator) throws Exception {
        TermStatsQueue tiq;

        if (field != null) {
            Terms terms = MultiFields.getTerms(reader, field);
            if (terms == null) {
                throw new RuntimeException("field " + field + " not found");
            }

            TermsEnum termsEnum = terms.iterator();
            tiq = new TermStatsQueue(numTerms, comparator);
            tiq.fill(field, termsEnum);
        } else {
            Fields fields = MultiFields.getFields(reader);
            if (fields.size() == 0) {
                throw new RuntimeException("no fields found for this index");
            }
            tiq = new TermStatsQueue(numTerms, comparator);
            for (String fieldName : fields) {
                Terms terms = fields.terms(fieldName);
                if (terms != null) {
                    tiq.fill(fieldName, terms.iterator());
                }
            }
        }

        TermStats[] result = new TermStats[tiq.size()];
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
    public static final class DocFreqComparator implements Comparator<TermStats> {

        @Override
        public int compare(TermStats a, TermStats b) {
            int res = Long.compare(a.docFreq, b.docFreq);
            if (res == 0) {
                res = a.field.compareTo(b.field);
                if (res == 0) {
                    res = a.termtext.compareTo(b.termtext);
                }
            }
            return res;
        }
    }

    /**
     * Compares terms by totalTermFreq
     */
    public static final class TotalTermFreqComparator implements Comparator<TermStats> {

        @Override
        public int compare(TermStats a, TermStats b) {
            int res = Long.compare(a.totalTermFreq, b.totalTermFreq);
            if (res == 0) {
                res = a.field.compareTo(b.field);
                if (res == 0) {
                    res = a.termtext.compareTo(b.termtext);
                }
            }
            return res;
        }
    }

    /**
     * Priority queue for TermStats objects
     **/
    static final class TermStatsQueue extends PriorityQueue<TermStats> {
        final Comparator<TermStats> comparator;

        TermStatsQueue(int size, Comparator<TermStats> comparator) {
            super(size);
            this.comparator = comparator;
        }

        @Override
        protected boolean lessThan(TermStats termInfoA, TermStats termInfoB) {
            return comparator.compare(termInfoA, termInfoB) < 0;
        }

        protected void fill(String field, TermsEnum termsEnum) throws IOException {
            BytesRef term = null;
            while ((term = termsEnum.next()) != null) {
                insertWithOverflow(new TermStats(field, term, termsEnum.docFreq(), termsEnum.totalTermFreq()));
            }
        }
    }
}
