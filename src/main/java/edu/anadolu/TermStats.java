package edu.anadolu;

import org.apache.lucene.util.BytesRef;

/**
 * Holder for a term along with its statistics
 * ({@link #docFreq} and {@link #totalTermFreq}).
 */
final class TermStats {
    BytesRef termtext;
    String field;
    int docFreq;
    long totalTermFreq;

    TermStats(String field, BytesRef termtext, int df, long tf) {
        this.termtext = BytesRef.deepCopyOf(termtext);
        this.field = field;
        this.docFreq = df;
        this.totalTermFreq = tf;
    }

    String getTermText() {
        return termtext.utf8ToString();
    }

    @Override
    public String toString() {
        return ("TermStats: term=" + termtext.utf8ToString() + " docFreq=" + docFreq + " totalTermFreq=" + totalTermFreq);
    }
}