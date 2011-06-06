
package com.affymetrix.genometryImpl.symloader;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.tribble.readers.TabixReader;

/**
 * This SymLoader is intended to be used for data sources that
 * are indexed with a tabix file. This SymLoader uses the TabixReader
 * from the Broad Institute
 * 
 * @author lfrohman
 */
public class SymLoaderTabix extends SymLoader {

	protected final Map<BioSeq, String> seqs = new HashMap<BioSeq, String>();
	private TabixReader tabixReader;
	private final LineProcessor lineProcessor;
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}
	
	/**
	 * file types that use tabix must implement this interface
	 * there should be one LineProcessor for each file type
	 * that performs the parsing of data lines
	 */
	public interface LineProcessor {
		/**
		 * this is the main method. The TabixReader will return
		 * the lines that are in the span (Seq, start, end) requested
		 * and those lines will be passed in here to be parsed
		 * into SeqSymmetry
		 * @param seq the sequence
		 * @param lineReader the LineReader from TabixReader
		 * @return the SeqSymmetry list from the parsing
		 */
		public List<? extends SeqSymmetry> processLines(BioSeq seq, TabixReader.TabixLineReader lineReader);
		/**
		 * perform any initialization here
		 * @param uri the uri of the data source
		 */
		public void init(URI uri);
		/**
		 * @return the pref list (file extensions) 
		 */
		public List<String> getFormatPrefList();
	}

	public static final String FILE_PREFIX = "file:";
	public SymLoaderTabix(URI uri, String featureName, AnnotatedSeqGroup group, LineProcessor lineProcessor){
		super(uri, featureName, group);
		this.lineProcessor = lineProcessor;
		try {
			String uriString = uri.toString();
			if (uriString.startsWith(FILE_PREFIX)) {
				uriString = uri.getPath();
			}
			this.tabixReader = new TabixReader(uriString);
		}
		catch (Exception x) {
			this.tabixReader = null;
		}
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	/**
	 * @return if this SymLoader is valid, there is a readable
	 * tabix file for the data source
	 */
	public boolean isValid() {
		return tabixReader != null;
	}

	@Override
	public void init(){
		if (!isValid()) {
			throw new IllegalStateException("tabix file does not exist or was not read");
		}
		if (this.isInitialized){
			return;
		}
		super.init();
		lineProcessor.init(uri);
		for (String seqID : tabixReader.getSequenceNames()) {
			BioSeq seq = group.getSeq(seqID);
			if (seq == null) {
				int length = 1000000000;
				seq = group.addSeq(seqID, length);
				Logger.getLogger(SymLoaderTabix.class.getName()).log(Level.INFO,
						"Sequence not found. Adding {0} with default length {1}",
						new Object[]{seqID,length});
			}
			seqs.put(seq, seqID);
		}
	}

	public LineProcessor getLineProcessor() {
		return lineProcessor;
	}

	@Override
	public List<String> getFormatPrefList() {
		return lineProcessor.getFormatPrefList();
	}

	@Override
	public List<BioSeq> getChromosomeList(){		
		init();
		return new ArrayList<BioSeq>(seqs.keySet());
	}

	@Override
	 public List<? extends SeqSymmetry> getGenome() {
		init();
		List<BioSeq> allSeq = getChromosomeList();
		List<SeqSymmetry> retList = new ArrayList<SeqSymmetry>();
		for(BioSeq seq : allSeq){
			retList.addAll(getChromosome(seq));
		}
		return retList;
	 }

	@Override
	public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
		init();
		String seqID = seqs.get(seq);
		return lineProcessor.processLines(seq, tabixReader.query(seqID));
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) {
		init();
		String seqID = seqs.get(overlapSpan.getBioSeq());
		TabixReader.TabixLineReader tabixLineReader = tabixReader.query(seqID + ":" + (overlapSpan.getStart() + 1) + "-" + overlapSpan.getEnd());
		if (tabixLineReader == null) {
			return new ArrayList<SeqSymmetry>();
		}
		return lineProcessor.processLines(overlapSpan.getBioSeq(), tabixLineReader);
    }
	
	public static SymLoader getSymLoader(SymLoader sym){
		String uriString = sym.uri.getPath();
		if (uriString.startsWith(FILE_PREFIX)) {
			uriString = uriString.substring(FILE_PREFIX.length());
		}
		if (TabixReader.isTabix(uriString)) {
			return new SymLoaderTabix(sym.uri, sym.featureName, sym.group, (LineProcessor)sym);
		}
		return sym;
	}
}

