package edu.anadolu;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class Searcher {

    Query query = new TermQuery(new Term("plain", "sonderece"));

    Query filter = new TermQuery(new Term("category", "ekonomi"));

    BooleanQuery bq = new BooleanQuery.Builder().add(query, BooleanClause.Occur.MUST).build();

    // TODO category filter

    // sadece numFound için özel bir collector var onu kullan kaç dokumanda geçtiğini bul

    // PMI pointwise mutual information -> modifiye edip custom edip

    // ham petrol : docFred (ham petrol) 50 ; docFred (hampetrol) = 50

    // https://svn.spraakdata.gu.se/repos/gerlof/pub/www/Docs/npmi-pfd.pdf

    // Euclidian Distance

    // Geometric Mean




}
