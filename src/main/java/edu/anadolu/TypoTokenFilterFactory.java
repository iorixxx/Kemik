package edu.anadolu;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.*;

import java.io.IOException;
import java.util.*;

import static edu.anadolu.Analyzers.getAnalyzedString;
import static edu.anadolu.Analyzers.typo;

public class TypoTokenFilterFactory extends TokenFilterFactory implements ResourceLoaderAware, MultiTermAwareComponent {

    private final HashMap<String, String> normMap = new HashMap<>();
    private final String dictionary;

    public TypoTokenFilterFactory(Map<String, String> args) {
        super(args);
        dictionary = require(args, "dictionary");

        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new TypoTokenFilter(tokenStream, normMap);
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
        return this;
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {

        Set<String> wlist = new HashSet<>();
        List<String> files = splitFileNames(dictionary);

        for (String file : files) {
            List<String> lines = getLines(loader, file.trim());
            wlist.addAll(lines);
        }

        for (String s : wlist) {

            String[] parts = s.split("\t");

            if (parts.length != 2) throw new RuntimeException("more than two words in the phrase " + s);

            if (parts[0].contains(" ") || parts[1].contains(" "))
                throw new RuntimeException("typo parts should not contain spaces " + String.join("_", parts));

            normMap.put(parts[0], parts[1]);
        }


    }


    private static final class TypoTokenFilter extends TokenFilter {

        private final HashMap<String, String> map;

        private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

        TypoTokenFilter(TokenStream input, HashMap<String, String> map) {
            super(input);
            this.map = map;
        }

        @Override
        public boolean incrementToken() throws IOException {

            if (!input.incrementToken()) return false;

            final String term = termAttribute.toString();

            //    boolean matched = false;

            List<String> matchedKeys = new ArrayList<>();

            for (String key : map.keySet()) {
                if (term.startsWith(key)) {
                    matchedKeys.add(key);
                }
            }

            if (matchedKeys.size() == 1) {
                termAttribute.setEmpty().append(map.get(matchedKeys.get(0)));
            } else if (matchedKeys.size() > 1) {
                String minKey = Collections.min(matchedKeys, Comparator.comparingInt(String::length));
                termAttribute.setEmpty().append(map.get(minKey));
                System.out.println(minKey + " " + matchedKeys);
            }

            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        String text = "egzos egzost yunanlı orjinal cimnastik yapmışlar anotomi motorsiklet motorsiklette orjinali orjinalleri";
        System.out.println("--------typo----------------");
        System.out.println(getAnalyzedString(text, typo()));
    }
}
