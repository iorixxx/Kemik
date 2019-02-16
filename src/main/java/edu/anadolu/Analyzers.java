package edu.anadolu;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.FlattenGraphFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class Analyzers {

    private static Analyzer plain() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("standard")
                //  .addTokenFilter("apostrophe")
                .addTokenFilter("turkishlowercase")
                .build();
    }

    private static Analyzer shingle() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("turkishlowercase")
                .addTokenFilter(ShingleFilterFactory.class,
                        "minShingleSize", "2",
                        "maxShingleSize", "5",
                        "outputUnigrams", "false",
                        "outputUnigramsIfNoShingles", "false")
                .addTokenFilter(FlattenGraphFilterFactory.class)
                .build();
    }

    static PerFieldAnalyzerWrapper analyzerWrapper() throws IOException {
        Map<String, Analyzer> analyzerMap = new HashMap<>();
        analyzerMap.put("shingle", shingle());
        analyzerMap.put("plain", plain());

        return new PerFieldAnalyzerWrapper(plain(), analyzerMap);
    }
}
