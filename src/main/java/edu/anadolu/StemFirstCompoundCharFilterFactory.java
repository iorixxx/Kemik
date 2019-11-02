package edu.anadolu;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.MappingCharFilterFactory;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.util.*;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.anadolu.Analyzers.getAnalyzedString;

public class StemFirstCompoundCharFilterFactory extends CharFilterFactory implements ResourceLoaderAware, MultiTermAwareComponent {

    private NormalizeCharMap normMap = null;
    private final String mapping;
    private final boolean decompose;
    private final Analyzer stemmer;

    /**
     * Creates a new StemFirstCompoundCharFilterFactory
     */
    public StemFirstCompoundCharFilterFactory(Map<String, String> args) throws IOException {
        super(args);
        mapping = require(args, "mapping");
        decompose = getBoolean(args, "decompose", false);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
        stemmer = CustomAnalyzer.builder()
                .addCharFilter(MappingCharFilterFactory.class, "mapping", "turkish_mapping_typo.txt")
                .withTokenizer("keyword")
                .addTokenFilter(org.apache.lucene.analysis.tr.Zemberek3StemFilterFactory.class)
                .build();
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {

        Set<String> duplicates = new HashSet<>();

        Set<String> wlist = new HashSet<>();
        List<String> files = splitFileNames(mapping);

        for (String file : files) {
            List<String> lines = getLines(loader, file.trim());
            wlist.addAll(lines);
        }

        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();

        for (String s : wlist) {

            String[] parts = s.split("_");

            if (parts.length != 2) throw new RuntimeException("more than two words in the phrase " + s);

            parts[0] = getAnalyzedString(parts[0], stemmer);
            parts[1] = getAnalyzedString(parts[1], stemmer);

            if (parts[0].contains(" ") || parts[1].contains(" "))
                throw new RuntimeException("stemmed parts should not contain spaces " + String.join("_", parts));

            String key = String.join("_", parts);
            if (duplicates.contains(key))
                continue;
            else
                duplicates.add(key);

            if (decompose)
                builder.add(String.join("", parts), String.join(" ", parts));
            else
                builder.add(String.join(" ", parts), String.join("", parts));
        }

        normMap = builder.build();
        duplicates.clear();
    }

    @Override
    public Reader create(Reader input) {
        return normMap == null ? input : new MappingCharFilter(normMap, input);
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
        return this;
    }
}
