package com.gene.searchmodelucene;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
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

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.IStatus;

public class SearchModeLucene implements ISearchModeSym {
	private static final int SEARCH_ALL_ORDINAL = 1000;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("searchmodelucene");
	private static final int MAX_HITS = 1000;
	static Pattern escaper = Pattern.compile("([\\\\\\+\\-\\&\\|\\!\\(\\)\\{\\}\\[\\]\\^\"\\~\\*\\?\\:\\ ])");
	protected IGBService igbService;

	public SearchModeLucene(IGBService igbService) {
		super();
		this.igbService = igbService;
	}

	/* for testing only */
	public static void main(String[] args) throws Exception {
		SearchModeLucene s = new SearchModeLucene(null);
		String uri = args.length > 1 ? args[0] : "file:/C:/Program Files (x86)/Apache Software Foundation/Apache2.2/htdocs/quickload2/H_sapiens_Feb_2009/IGIS Gene Models.gff.gz";
//		String uri = args.length > 1 ? args[0] : "http://localhost/Bed/bed_01.bed";
		String searchTerm = args.length > 2 ? args[1] : "ZNF19*";
		String seqName = args.length > 3 ? args[2] : null;
		List<SeqSymmetry> results = s.searchIndex(uri, searchTerm, seqName);
		for (SeqSymmetry result : results) {
			System.out.println(result.getID() + " @ " + result.getSpan(0));
		}
	}

	private String massageSearchTerm(String searchTerm) {
		return escaper.matcher(searchTerm).replaceAll("\\\\$1").toLowerCase() + "*";
	}

	private SeqSymmetry getSeqSymmetry(Document doc, String path) {
        SimpleSymWithProps sym = new SimpleSymWithProps();
        // load properties
        for (Fieldable field : doc.getFields()) {
        	String value = doc.get(field.name());
            sym.setProperty(field.name(), value);
        }
        // load span
        String seqName = doc.get("seq");
        BioSeq seq;
        if (GenometryModel.getGenometryModel().getSelectedSeqGroup() == null) {
        	seq = new BioSeq(seqName, "", 0);
        }
        else {
            seq = GenometryModel.getGenometryModel().getSelectedSeqGroup().getSeq(seqName);
        }
        int start = Integer.parseInt(doc.get("start"));
        int end = Integer.parseInt(doc.get("end"));
        sym.addSpan(new SimpleSeqSpan(start, end, seq));
        return sym;
	}

	private List<SeqSymmetry> searchIndex(String uri, String searchTerm, String seqName) {
		List<SeqSymmetry> searchResults = new ArrayList<SeqSymmetry>();
		String path = null;
		try {
			Directory directory;
			if (uri.toLowerCase().startsWith("http")) {
				path = uri;
				directory = new HttpDirectory(FileUtil.getInstance().getIndexName(uri));
			}
			else {
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
			TopDocs results = searcher.search(query, MAX_HITS);
		    for (ScoreDoc match : results.scoreDocs) {
		        Document doc = searcher.doc(match.doc);
		        searchResults.add(getSeqSymmetry(doc, path));
		    }
		}
		catch (NoSuchDirectoryException x) {
			searchResults = null;			
		} // no lucene index is OK
		catch (Exception x) {
			x.printStackTrace(System.out);
			searchResults = null;			
		}
		return searchResults;
	}

	@Override
	public String getName() {
		return BUNDLE.getString("searchLucene");
	}

	@Override
	public int searchAllUse() {
		return SEARCH_ALL_ORDINAL;
	}

	@Override
	public String getTooltip() {
		return BUNDLE.getString("searchLuceneTooltip");
	}

	@Override
	public boolean useGenomeInSeqList() {
		return true;
	}

	@Override
	public String checkInput(String search_text, BioSeq vseq, String seq) {
		return null;
	}

	@Override
	public List<SeqSymmetry> searchTrack(String search_text, final BioSeq chrFilter, TypeContainerAnnot contSym, IStatus statusHolder, boolean option) {
		return searchIndex(contSym.getType(), search_text, chrFilter == null ? null : chrFilter.toString());
	}

	@Override
	public List<SeqSymmetry> search(String search_text, BioSeq chrFilter, IStatus statusHolder, boolean option) {
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		if (search_text != null && !search_text.isEmpty()) {
			AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
			for (GenericVersion gVersion : group.getEnabledVersions()) {
				if (gVersion.gServer.serverType == ServerTypeI.LocalFiles || gVersion.gServer.serverType == ServerTypeI.QuickLoad) {
					for (GenericFeature feature : gVersion.getFeatures()) {
						if (feature.isVisible() && feature.symL != null) {
							if (statusHolder != null) {
								statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchSearching"), feature.symL.uri.toString(), search_text));
							}
							List<SeqSymmetry> results = searchIndex(feature.symL.uri.toString(), search_text, chrFilter == null ? null : chrFilter.toString());
							if (results != null) {
								syms.addAll(results);
							}
						}
					}
				}
			}
		}
		if (syms.isEmpty()) {
			statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchNoResults"), search_text));
			return null;
		}
		statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchResults"), search_text, "" + syms.size()));
		return syms;
	}

	@Override
	public List<SeqSymmetry> getAltSymList() {
		return null;
	}
}
