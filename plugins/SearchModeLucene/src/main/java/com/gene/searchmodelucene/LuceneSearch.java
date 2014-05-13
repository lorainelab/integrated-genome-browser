package com.gene.searchmodelucene;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.affymetrix.genometryImpl.util.GeneralUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;

public abstract class LuceneSearch<T> {

    static Pattern escaper = Pattern.compile("([\\\\\\+\\-\\&\\|\\!\\(\\)\\{\\}\\[\\]\\^\"\\~\\*\\?\\:\\ ])");

    protected String massageSearchTerm(String searchTerm) {
        return escaper.matcher(searchTerm).replaceAll("\\\\$1").toLowerCase() + "*";
    }

    public List<T> searchIndex(String uri, String searchTerm, int max_hits) {
        List<T> searchResults = new ArrayList<T>();
        String path = null;
        try {
            Directory directory;
            if (uri.toLowerCase().startsWith("http")) {
                path = uri;
                directory = new HttpDirectory(FileUtil.getInstance().getIndexName(uri));
            } else {
                path = GeneralUtils.fixFileName(uri);
                directory = FSDirectory.open(new File(FileUtil.getInstance().getIndexName(path)));
            }
            IndexReader ir = IndexReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(ir);
            Analyzer analyzer = new KeywordAnalyzer();//new StandardAnalyzer(Version.LUCENE_35);
            List<String> fieldInfoList = new ArrayList<String>();
            ReaderUtil.getMergedFieldInfos(ir);
            for (FieldInfo fieldInfo : ReaderUtil.getMergedFieldInfos(ir)) {
                fieldInfoList.add(fieldInfo.name);
            }
            String[] fields = fieldInfoList.toArray(new String[]{});
            QueryParser parser = new MultiFieldQueryParser(Version.LUCENE_35, fields, analyzer);
            parser.setLowercaseExpandedTerms(false);
            Query query = parser.parse(massageSearchTerm(searchTerm));
            TopDocs results = searcher.search(query, max_hits);
            for (ScoreDoc match : results.scoreDocs) {
                Document doc = searcher.doc(match.doc);
                T t = processSearch(doc);
                if (t != null) {
                    searchResults.add(t);
                }
            }
        } catch (NoSuchDirectoryException x) {
            searchResults = null;
        } // no lucene index is OK
        catch (Exception x) {
            x.printStackTrace(System.out);
            searchResults = null;
        }
        return searchResults;
    }

    public abstract T processSearch(Document doc);
}
