package edu.anadolu;

import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Stats {

    public static void main(String[] args) throws IOException {

        for (DocType type : DocType.values()) {
            System.out.println("processing " + type);
            unique(Paths.get(type.toString()), "plain");
        }
    }


    /**
     * Prints the number of unique terms in a Lucene index
     *
     * @param indexPath Lucene index
     * @throws IOException should not happen
     */
    private static void unique(Path indexPath, String field) throws IOException {

        System.out.println("Opening Lucene index directory '" + indexPath.toAbsolutePath() + "'...");

        try (final Directory dir = FSDirectory.open(indexPath);
             IndexReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            CollectionStatistics statistics = searcher.collectionStatistics(field);

            final Terms terms = MultiFields.getTerms(reader, field);
            if (terms == null) {
                System.out.println("MultiFields.getTerms returns null. Wrong field ? " + field);
                return;
            }

            TermsEnum termsEnum = terms.iterator();
            long c = 0;
            while (termsEnum.next() != null) {
                c++;
            }

            System.out.println("The number of documents : " + statistics.docCount());
            System.out.println("The number of terms : " + statistics.sumTotalTermFreq());
            System.out.println("The number of unique terms : " + c);

        } catch (IndexNotFoundException e) {
            System.out.println("IndexNotFound in " + indexPath.toAbsolutePath());
        }
    }
}
