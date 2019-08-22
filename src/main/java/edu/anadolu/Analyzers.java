package edu.anadolu;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.charfilter.MappingCharFilterFactory;
import org.apache.lucene.analysis.core.FlattenGraphFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Analyzers {

    static Analyzer plain() throws IOException {
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
                        "maxShingleSize", "4",
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

    private static Analyzer compound() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("turkishlowercase")
                .addTokenFilter("DictionaryCompoundWord", "dictionary", "dictionary.txt")
                .build();
    }

    static Analyzer decompose(boolean decompose, boolean typo, boolean mapping) throws IOException {

        if (typo)
            if (mapping)
                return CustomAnalyzer.builder()
                        .addCharFilter(MappingCharFilterFactory.class, "mapping", "turkish_mapping_typo.txt")
                        .addCharFilter(CompoundCharFilterFactory.class, "mapping", "compound.txt,compound_close.txt,compound_open.txt,compound_4b.txt,compound_m.txt,compound_ttc.txt", "decompose", Boolean.toString(decompose))
                        .withTokenizer("standard")
                        .addTokenFilter("turkishlowercase")
                        .build();
            else
                return CustomAnalyzer.builder()
                        .addCharFilter(CompoundCharFilterFactory.class, "mapping", "compound.txt,compound_close.txt,compound_open.txt,compound_4b.txt,compound_m.txt,compound_ttc.txt", "decompose", Boolean.toString(decompose))
                        .withTokenizer("standard")
                        .addTokenFilter("turkishlowercase")
                        .addTokenFilter("stemmeroverride", "dictionary", "turkish_typo.txt")
                        .build();

        else
            return CustomAnalyzer.builder()
                    .addCharFilter(CompoundCharFilterFactory.class, "mapping", "compound.txt,compound_close.txt,compound_open.txt,compound_4b.txt,compound_m.txt,compound_ttc.txt", "decompose", Boolean.toString(decompose))
                    .withTokenizer("standard")
                    .addTokenFilter("turkishlowercase")
                    .build();

    }

    static Analyzer typo() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("turkishlowercase")
                .addTokenFilter("stemmeroverride", "dictionary", "turkish_typo.txt")
                .build();
    }

    static Analyzer mapping_typo() throws IOException {
        return CustomAnalyzer.builder()
                .addCharFilter(MappingCharFilterFactory.class, "mapping", "turkish_mapping_typo.txt")
                .withTokenizer("standard")
                .addTokenFilter("turkishlowercase")
                .build();
    }


    /**
     * Modified from : http://lucene.apache.org/core/4_10_2/core/org/apache/lucene/analysis/package-summary.html
     */
    public static List<String> getAnalyzedTokens(String text, Analyzer analyzer) {

        final List<String> list = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream("content", new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);

            final TypeAttribute typeAtt = ts.addAttribute(TypeAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                list.add(termAtt.toString());
                System.out.println(termAtt.toString() + " " + typeAtt.type());
            }

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return list;
    }

    static String getAnalyzedString(String text, Analyzer analyzer) {

        final StringBuilder builder = new StringBuilder();

        try (TokenStream ts = analyzer.tokenStream("content", new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);

            final TypeAttribute typeAtt = ts.addAttribute(TypeAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                builder.append(termAtt.buffer(), 0, termAtt.length());
                builder.append(' ');
            }

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }


        return builder.toString().trim();
    }

    public static void main(String[] args) throws IOException {

        String text = "masaüstü newyork catwalk hamamböceği genel kurmay genelkurmay";

        getAnalyzedTokens(text, decompose(true, false, false));

        System.out.println("----------decompose=false--------------");
        text = "masa üstü new york cat walk hamam böceği genel kurmay genelkurmay köpek balığı";

        System.out.println(getAnalyzedString(text, decompose(false, false, false)));

        text = "yunanlı orjinal cimnastik yapmışlar anotomi motorsiklet motorsiklette orjinali orjinalleri";
        System.out.println("--------typo----------------");
        System.out.println(getAnalyzedString(text, typo()));

        System.out.println("---------mapping_typo---------------");
        System.out.println(getAnalyzedString(text, mapping_typo()));

        System.out.println("---------mapping_typo---------------");
        System.out.println(getAnalyzedString("psikiyatrist psikiyatristi psikiyatristin psikiyatristiniz psikiyatristler psikiyatristlerin", mapping_typo()));

        text = "orjinal cimnastik masaüstü newyork catwalk hamamböceği genel kurmay genelkurmay";
        System.out.println("----------decompose=true,typo=true--------------");
        System.out.println(getAnalyzedString(text, decompose(true, true, false)));

        System.out.println("----------decompose=false,typo=true--------------");
        text = "orjinal cimnastik masa üstü new york cat walk hamam böceği genel kurmay genelkurmay köpek balığı";

        System.out.println(getAnalyzedString(text, decompose(false, true, false)));

    }
}
