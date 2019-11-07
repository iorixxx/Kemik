package edu.anadolu;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.util.*;

import java.io.IOException;
import java.util.*;

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
        private final KeywordAttribute keywordAttribute = addAttribute(KeywordAttribute.class);

        TypoTokenFilter(TokenStream input, HashMap<String, String> map) {
            super(input);
            this.map = map;
        }

        @Override
        public boolean incrementToken() throws IOException {

            if (!input.incrementToken()) return false;
            if (keywordAttribute.isKeyword()) return true;

            final String term = termAttribute.toString();

            boolean matched = false;
            for (String key : map.keySet()) {
                if (term.startsWith(key)) {
                    if (matched) {
                        System.out.println("key " + key);
                    }
                    termAttribute.setEmpty().append(map.get(key));
                    matched = true;
                }
            }
            return true;
        }
    }
}
