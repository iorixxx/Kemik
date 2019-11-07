package edu.anadolu;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.*;

import java.io.IOException;
import java.util.*;

import static edu.anadolu.Analyzers.getAnalyzedString;
import static edu.anadolu.Analyzers.getAnalyzedTokens;

public class CompoundTokenFilterFactory extends TokenFilterFactory implements ResourceLoaderAware, MultiTermAwareComponent {


    private final String mapping;
    private final boolean decompose;

    public CompoundTokenFilterFactory(Map<String, String> args) {
        super(args);
        mapping = require(args, "mapping");
        decompose = getBoolean(args, "decompose", false);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
        return this;
    }

    private HashMap<String, String> map = new HashMap<>();

    @Override
    public void inform(ResourceLoader loader) throws IOException {

        Set<String> duplicates = new HashSet<>();

        Set<String> wlist = new HashSet<>();
        List<String> files = splitFileNames(mapping);

        for (String file : files) {
            List<String> lines = getLines(loader, file.trim());
            wlist.addAll(lines);
        }


        for (String s : wlist) {

            String[] parts = s.split("_");

            if (parts.length != 2) throw new RuntimeException("more than two words in the phrase " + s);


            if (parts[0].contains(" ") || parts[1].contains(" "))
                throw new RuntimeException("stemmed parts should not contain spaces " + String.join("_", parts));

            String key = String.join("_", parts);
            if (duplicates.contains(key))
                continue;
            else
                duplicates.add(key);

            // yurt_dışından -> yurt dışından
            map.put(key, String.join(" ", parts));

        }

        duplicates.clear();

    }

    @Override
    public TokenStream create(TokenStream input) {
        return new CompoundTokenFilter(input, map);
    }

    private static final class CompoundTokenFilter extends TokenFilter {

        private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

        HashMap<String, String> map;

        CompoundTokenFilter(TokenStream input, HashMap<String, String> map) {
            super(input);
            this.map = map;
        }

        @Override
        public boolean incrementToken() throws IOException {

            if (!input.incrementToken()) return false;

            final String term = termAttribute.toString();


            List<String> matchedKeys = new ArrayList<>();

            for (String k : map.keySet()) {

                String key = k.replaceAll("_", "");

                if (term.startsWith(key)) {
                    matchedKeys.add(k);
                }
            }

            if (matchedKeys.size() == 1) {
                termAttribute.setEmpty().append(map.get(matchedKeys.get(0)));
            } else if (matchedKeys.size() > 1) {
                String minKey = Collections.min(matchedKeys, Comparator.comparingInt(String::length));
                termAttribute.setEmpty().append(map.get(minKey));
             //   System.out.println("INFO " + minKey + " " + matchedKeys);
            }

            return true;

        }
    }

    public static void main(String[] args) throws Exception {


        String text = "a b c d e f g";

        System.out.println(getAnalyzedTokens(text, CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("apostrophe")
                .addTokenFilter("turkishlowercase")
                .addTokenFilter(ShingleFilterFactory.class,
                        "minShingleSize", "2",
                        "maxShingleSize", "2",
                        "outputUnigrams", "true",
                        "outputUnigramsIfNoShingles", "false",
                        "tokenSeparator", "_")
                .build()));

        text = "altyapılarda altyapısıyla alt yapısıyla";
        System.out.println("--------typo----------------");
        System.out.println(getAnalyzedString(text,

                CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("apostrophe")
                .addTokenFilter("turkishlowercase")
                .addTokenFilter(CompoundTokenFilterFactory.class, "mapping", "compound.txt,compound_close.txt,compound_open.txt,compound_4b.txt,compound_m.txt,compound_ttc.txt")
                .build()));

    }

}
