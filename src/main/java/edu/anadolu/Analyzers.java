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

    static Analyzer decompose(boolean decompose, boolean typo) throws IOException {

        if (typo)
            return CustomAnalyzer.builder()
                    .addCharFilter(StemFirstCompoundCharFilterFactory.class, "mapping", "compound.txt,compound_close.txt,compound_open.txt,compound_4b.txt,compound_m.txt,compound_ttc.txt", "decompose", Boolean.toString(decompose))
                    .withTokenizer("standard")
                    .addTokenFilter("turkishlowercase")
                    .addTokenFilter(TypoTokenFilterFactory.class, "dictionary", "turkish_typo.txt")
                    .build();

        else
            return CustomAnalyzer.builder()
                    .addCharFilter(StemFirstCompoundCharFilterFactory.class, "mapping", "compound.txt,compound_close.txt,compound_open.txt,compound_4b.txt,compound_m.txt,compound_ttc.txt", "decompose", Boolean.toString(decompose))
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

    static Analyzer zemberek() throws IOException {
        return CustomAnalyzer.builder()
                .addCharFilter(MappingCharFilterFactory.class, "mapping", "turkish_mapping_typo.txt")
                .withTokenizer("standard")
                .addTokenFilter("turkishlowercase")
                .addTokenFilter(org.apache.lucene.analysis.tr.Zemberek3StemFilterFactory.class)
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

        getAnalyzedTokens(text, decompose(true, false));

        System.out.println("----------decompose=false--------------");
        text = "masa üstü new york cat walk hamam böceği genel kurmay genelkurmay köpek balığı";

        System.out.println(getAnalyzedString(text, decompose(false, false)));

        text = "yunanlı orjinal cimnastik yapmışlar anotomi motorsiklet motorsiklette orjinali orjinalleri";
        System.out.println("--------typo----------------");
        System.out.println(getAnalyzedString(text, typo()));

        System.out.println("---------mapping_typo---------------");
        System.out.println(getAnalyzedString(text, mapping_typo()));

        System.out.println("---------mapping_typo---------------");
        System.out.println(getAnalyzedString("psikiyatrist psikiyatristi psikiyatristin psikiyatristiniz psikiyatristler psikiyatristlerin", mapping_typo()));

        text = "teröristbaşının orjinal cimnastik masaüstü newyork catwalk hamamböceği hamamböceklerini genel kurmay genelkurmay süper market süper marketler süpermarket süpermarketler";
        System.out.println("----------decompose=true,typo=true--------------");
        System.out.println(getAnalyzedString(text, decompose(true, true)));

        System.out.println("----------decompose=false,typo=true--------------");
        text = "teröristbaşının orjinal cimnastik masa üstü new york cat walk hamam böceği genel kurmay genelkurmay köpek balığı hamamböceklerini süper market süper marketler süpermarket süpermarketler";

        System.out.println(getAnalyzedString(text, decompose(false, true)));

        text = "teröristbaşının terörist başının orjinal cimnastik masa üstü new york cat walk hamam böceği genel kurmay genelkurmay köpek balığı hamamböceklerini süper market süper marketler süpermarket süpermarketler";

        stemFirst(text);
    }


    private static void stemFirst(String text) throws IOException {

        text = text + "altetmek\n" +
                "arzetmek\n" +
                "azatetmek\n" +
                "dansetmek\n" +
                "eletmek\n" +
                "göçetmek\n" +
                "ilanetmek\n" +
                "kabuletmek\n" +
                "kuletmek\n" +
                "kulolmak\n" +
                "notetmek\n" +
                "oyunetmek\n" +
                "sözetmek\n" +
                "terketmek\n" +
                "varolmak\n" +
                "yoketmek\n" +
                "yokolmak\n" +
                "adabalığı\n" +
                "ateşbalığı\n" +
                "dilbalığı\n" +
                "fulyabalığı\n" +
                "kedibalığı\n" +
                "kılıçbalığı\n" +
                "köpekbalığı\n" +
                "tonbalığı\n" +
                "yılanbalığı\n" +
                "acıbalık\n" +
                "bıyıklıbalık\n" +
                "dikenlibalık\n" +
                "ardıçkuşu\n" +
                "arıkuşu\n" +
                "çalıkuşu\n" +
                "devekuşu\n" +
                "muhabbetkuşu\n" +
                "sakakuşu\n" +
                "tarlakuşu\n" +
                "yağmurkuşu\n" +
                "alıcıkuş\n" +
                "boğmaklıkuş\n" +
                "makaralıkuş\n" +
                "ağustosböceği\n" +
                "ateşböceği\n" +
                "cırcırböceği cırcır böceği\n" +
                "hamamböceği\n" +
                "ipekböceği\n" +
                "uçuçböceği\n" +
                "uğurböceği\n" +
                "ağılıböcek\n" +
                "çalgıcıböcek\n" +
                "sümüklüböcek\n" +
                "atsineği\n" +
                "etsineği\n" +
                "meyvesineği\n" +
                "sığırsineği\n" +
                "susineği\n" +
                "uyuzsineği\n" +
                "denizyılanı\n" +
                "okyılanı\n" +
                "suyılanı\n" +
                "Ankarakeçisi\n" +
                "dağkeçisi\n" +
                "yabankeçisi\n" +
                "fındıkfaresi\n" +
                "tarlafaresi\n" +
                "dağsıçanı\n" +
                "tarlasıçanı\n" +
                "beçtavuğu\n" +
                "dağtavuğu\n" +
                "adatavşanı\n" +
                "yabantavşanı\n" +
                "kayaörümceği\n" +
                "şeytanörümceği\n" +
                "balarısı\n" +
                "yaprakarısı\n" +
                "pekinördeği\n" +
                "denizördeği\n" +
                "ankarakedisi\n" +
                "bozkırkedisi\n" +
                "afrikadomuzu\n" +
                "yerdomuzu\n" +
                "ayrıkotu\n" +
                "beşparmakotu\n" +
                "çörekotu\n" +
                "eğreltiotu\n" +
                "güzelavratotu\n" +
                "kelebekotu\n" +
                "ökseotu\n" +
                "pisipisiotu\n" +
                "taşkıranotu\n" +
                "yüksükotu\n" +
                "acıot\n" +
                "sütlüot\n" +
                "ateşçiçeği\n" +
                "çuhaçiçeği\n" +
                "güzelhatunçiçeği\n" +
                "ipekçiçeği\n" +
                "küpeçiçeği\n" +
                "lavantaçiçeği\n" +
                "mumçiçeği\n" +
                "yaylaçiçeği\n" +
                "yıldızçiçeği\n" +
                "ölmezçiçek\n" +
                "avizeağacı\n" +
                "banağacı\n" +
                "dantelağacı\n" +
                "kâğıtağacı\n" +
                "mantarağacı\n" +
                "ödağacı\n" +
                "pelesenkağacı\n" +
                "tespihağacı\n" +
                "altınkökü\n" +
                "eğirkökü\n" +
                "helvacıkökü\n" +
                "meyankökü\n" +
                "ekkök\n" +
                "saçakkök\n" +
                "yumrukök\n" +
                "dağelması\n" +
                "yerelması\n" +
                "çalıdikeni\n" +
                "devedikeni\n" +
                "köpeküzümü\n" +
                "kuşüzümü\n" +
                "çakalarmudu\n" +
                "dağarmudu\n" +
                "atkestanesi\n" +
                "kuzukestanesi\n" +
                "caneriği\n" +
                "gövemeriği\n" +
                "kuzumantarı\n" +
                "yermantarı\n" +
                "sukamışı\n" +
                "şekerkamışı\n" +
                "dağnanesi\n" +
                "taşnanesi\n" +
                "ayıgülü\n" +
                "japongülü\n" +
                "antepfıstığı\n" +
                "çamfıstığı\n" +
                "sırıkfasulyesi\n" +
                "soyafasulyesi\n" +
                "amerikanbademi\n" +
                "taşbademi\n" +
                "afrikamenekşesi\n" +
                "denizmenekşesi\n" +
                "japonsarmaşığı\n" +
                "kuzusarmaşığı\n" +
                "hintinciri\n" +
                "kavakinciri\n" +
                "armutkurusu\n" +
                "kayısıkurusu\n" +
                "kayasarımsağı\n" +
                "köpeksarımsağı\n" +
                "şekerpancarı\n" +
                "yabanpancarı\n" +
                "kurufasulye\n" +
                "kuruincir\n" +
                "kurusoğan\n" +
                "kuruüzüm\n" +
                "alçıtaşı\n" +
                "bileğitaşı\n" +
                "çakmaktaşı\n" +
                "hacıbektaştaşı\n" +
                "kireçtaşı\n" +
                "lületaşı\n" +
                "oltutaşı\n" +
                "süngertaşı\n" +
                "yılantaşı\n" +
                "buzultaş\n" +
                "damlataş\n" +
                "dikilitaş\n" +
                "kayağantaş\n" +
                "yapraktaş\n" +
                "arapsabunu\n" +
                "elsabunu\n" +
                "kahvedeğirmeni\n" +
                "yeldeğirmeni\n" +
                "kahvedolabı\n" +
                "sudolabı\n" +
                "müzikodası\n" +
                "oturmaodası\n" +
                "duvarsaati\n" +
                "kolsaati\n" +
                "duvartakvimi\n" +
                "masatakvimi\n" +
                "krizmasası\n" +
                "yemekmasası\n" +
                "itfaiyearacı\n" +
                "kurtarmaaracı\n" +
                "masaörtüsü\n" +
                "yatakörtüsü\n" +
                "elkitabı\n" +
                "okumakitabı\n" +
                "frenkgömleği\n" +
                "ingilizanahtarı\n" +
                "ingilizsicimi\n" +
                "altgeçit\n" +
                "tüpgeçit\n" +
                "üstgeçit\n" +
                "çekmedemir\n" +
                "çekmekat\n" +
                "dolmakalem\n" +
                "dönmedolap\n" +
                "kesmekaya\n" +
                "topluiğne\n" +
                "vurmalıçalgılar\n" +
                "vurmalısazlar\n" +
                "yapmaçiçek\n" +
                "afyonruhu\n" +
                "katranruhu\n" +
                "lokmanruhu\n" +
                "naneruhu\n" +
                "tuzruhu\n" +
                "arnavutkaldırımı\n" +
                "çevreyolu\n" +
                "denizyolu\n" +
                "havayolu\n" +
                "karayolu\n" +
                "keçiyolu\n" +
                "köprüyol\n" +
                "açıkoturum\n" +
                "açıköğretim\n" +
                "anadili\n" +
                "aytutulması\n" +
                "başağrısı\n" +
                "başbelası\n" +
                "başdönmesi\n" +
                "çıkışyolu\n" +
                "çözümyolu\n" +
                "dilbirliği\n" +
                "dinbirliği\n" +
                "güçbirliği\n" +
                "işbirliği\n" +
                "işbölümü\n" +
                "maddebaşı\n" +
                "sesuyumu\n" +
                "yerçekimi\n" +
                "anlambilimi\n" +
                "dilbilimi\n" +
                "edebiyatbilimi\n" +
                "gökbilimi\n" +
                "halkbilimi\n" +
                "ruhbilimi\n" +
                "toplumbilimi\n" +
                "toprakbilimi\n" +
                "yerbilimi\n" +
                "dilbilgisi\n" +
                "halkbilgisi\n" +
                "sesbilgisi\n" +
                "şekilbilgisi\n" +
                "gözyuvarı\n" +
                "havayuvarı\n" +
                "ısıyuvarı\n" +
                "ışıkyuvarı\n" +
                "renkyuvarı\n" +
                "yeryuvarı\n" +
                "havaküre\n" +
                "ışıkküre\n" +
                "suküre\n" +
                "taşküre\n" +
                "yarıküre\n" +
                "yarımküre\n" +
                "bohçaböreği\n" +
                "talaşböreği\n" +
                "bademyağı\n" +
                "kuyrukyağı\n" +
                "arpasuyu\n" +
                "madensuyu\n" +
                "tulumpeyniri\n" +
                "beyazpeynir\n" +
                "adanakebabı\n" +
                "taskebabı\n" +
                "inegölköftesi\n" +
                "izmirköftesi\n" +
                "ezogelinçorbası\n" +
                "yoğurtçorbası\n" +
                "irmikhelvası\n" +
                "kozhelva\n" +
                "kemalpaşatatlısı\n" +
                "yoğurttatlısı\n" +
                "bademşekeri\n" +
                "kestaneşekeri\n" +
                "balıkyumurtası\n" +
                "lopyumurta\n" +
                "burgumakarna\n" +
                "yüksükmakarna\n" +
                "kakaolukek\n" +
                "üzümlükek\n" +
                "çiğköfte\n" +
                "içliköfte\n" +
                "dolmabiber\n" +
                "sivribiber\n" +
                "esmerşeker\n" +
                "kesmeşeker\n" +
                "süzmeyoğurt\n" +
                "yarmaşeftali\n" +
                "kuruyemiş\n" +
                "çobanyıldızı\n" +
                "kervanyıldızı\n" +
                "kutupyıldızı\n" +
                "uyrukluyıldız\n" +
                "göktaşı\n" +
                "havataşı\n" +
                "meteortaşı\n" +
                "patlakgöz\n" +
                "süzgüngöz\n" +
                "aşıkkemiği\n" +
                "elmacıkkemiği\n" +
                "serçeparmak\n" +
                "şehadetparmağı\n" +
                "yüzükparmağı\n" +
                "azıdişi\n" +
                "köpekdişi\n" +
                "sütdişi\n" +
                "kuyruksokumu\n" +
                "safrakesesi\n" +
                "çatmakaş\n" +
                "takmadiş\n" +
                "takmakirpik\n" +
                "takmakol\n" +
                "ekşisurat\n" +
                "kepçesurat\n" +
                "gagaburun\n" +
                "kargaburun\n" +
                "kepçekulak\n" +
                "çetinceviz\n" +
                "çöpsüzüzüm\n" +
                "eskikurt\n" +
                "sarıçıyan\n" +
                "sağmalinek\n" +
                "eskitoprak\n" +
                "eskitüfek\n" +
                "karamaşa\n" +
                "sapsızbalta\n" +
                "çakırpençe\n" +
                "demiryumruk\n" +
                "kurukemik\n" +
                "bağbozumu\n" +
                "geceyarısı\n" +
                "günortası\n" +
                "haftabaşı\n" +
                "haftasonu\n" +
                "bakarkör\n" +
                "çalarsaat\n" +
                "çıkaryol\n" +
                "dönersermaye\n" +
                "güleryüz\n" +
                "koşaradım\n" +
                "yazarkasa\n" +
                "yetersayı\n" +
                "çıkmazsokak\n" +
                "geçmezakçe\n" +
                "görünmezkaza\n" +
                "ölmezçiçek\n" +
                "tükenmezkalem\n" +
                "akanyıldız\n" +
                "doyuranbuhar\n" +
                "uçandaire\n" +
                "balrengi\n" +
                "dumanrengi\n" +
                "gümüşrengi\n" +
                "portakalrengi\n" +
                "samanrengi\n" +
                "ateşkırmızısı\n" +
                "boncukmavisi\n" +
                "çivitmavisi\n" +
                "gecemavisi\n" +
                "limonsarısı\n" +
                "safrayeşili\n" +
                "sütkırı\n" +
                "açıkmavi\n" +
                "açıkyeşil\n" +
                "karasarı\n" +
                "kirlisarı\n" +
                "koyumavi\n" +
                "koyuyeşil\n" +
                "batıtrakya\n" +
                "doğuanadolu\n" +
                "güneykutbu\n" +
                "kuzeyamerika\n" +
                "güneydoğuanadolu\n" +
                "aşağıayrancı\n" +
                "yukarıayrancı\n" +
                "ortaanadolu\n" +
                "ortaasya\n" +
                "ortadoğu\n" +
                "içasya\n" +
                "içanadolu\n" +
                "yakındoğu\n" +
                "uzakdoğu\n" +
                "ahlakdışı\n" +
                "çağdışı\n" +
                "dindışı\n" +
                "kanundışı\n" +
                "olağandışı\n" +
                "yasadışı\n" +
                "ceviziçi\n" +
                "haftaiçi\n" +
                "yurtiçi\n" +
                "aklısıra\n" +
                "ardısıra\n" +
                "peşisıra\n" +
                "yanısıra\n" +
                "derialtı\n" +
                "sualtı\n" +
                "toprakaltı\n" +
                "yeraltı\n" +
                "tepeüstü\n" +
                "altkurul\n" +
                "altyazı\n" +
                "üstkat\n" +
                "üstküme\n" +
                "anadili\n" +
                "önsöz\n" +
                "önyargı\n" +
                "artdamak\n" +
                "artniyet\n" +
                "arkaplan\n" +
                "arkateker\n" +
                "yancümle\n" +
                "yanetki\n" +
                "karşıgörüş\n" +
                "karşıoy\n" +
                "içsavaş\n" +
                "içtüzük\n" +
                "dışborç\n" +
                "dışhat\n" +
                "ortakulak\n" +
                "ortaoyunu\n" +
                "büyükdalga\n" +
                "büyükdefter\n" +
                "küçükharf\n" +
                "küçükparmak\n" +
                "sağaçık\n" +
                "sağbek\n" +
                "solaçık\n" +
                "solbek\n" +
                "peşinfikir\n" +
                "peşinhüküm\n" +
                "birgözeli\n" +
                "birhücreli\n" +
                "ikianlamlı\n" +
                "ikieşeyli\n" +
                "tekeşli\n" +
                "tekhücreli\n" +
                "çokdüzlemli\n" +
                "çokhücreli\n" +
                "çiftayaklılar\n" +
                "çiftkanatlılar";

        System.out.println("-----------sample text--------------------");
        System.out.println(text);
        String stemmed = getAnalyzedString(text, zemberek());

        System.out.println("-----------stemmed text--------------------");
        System.out.println(stemmed);

        System.out.println("----------decompose=false,typo=true,mapping=true--------------");
        System.out.println(getAnalyzedString(stemmed, decompose(false, true)));

        System.out.println("----------decompose=true,typo=true,mapping=true--------------");
        System.out.println(getAnalyzedString(stemmed, decompose(true, true)));

    }
}
