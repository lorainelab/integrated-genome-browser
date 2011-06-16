package com.affymetrix.genometryImpl.symloader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.symloader.BinSearchReader.SequenceSpanReader;
import com.affymetrix.genometryImpl.symloader.LineProcessor;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.tribble.util.LineReader;

/**
 * This SymLoader is intended to be used for data sources that
 * are sorted by seq and position. This SymLoader uses code
 * from the Broad Institute
 */
public class SymLoaderBinSearch extends SymLoader {

	protected final Map<BioSeq, String> seqs = new HashMap<BioSeq, String>();
	private BinSearchReader binSearchReader;
	private final LineProcessor lineProcessor;
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}
	
	public static final String FILE_PREFIX = "file:";
	public SymLoaderBinSearch(URI uri, String featureName, AnnotatedSeqGroup group, LineProcessor lineProcessor, SequenceSpanReader sequenceSpanReader){
		super(uri, featureName, group);
		this.lineProcessor = lineProcessor;
		try {
			String uriString = uri.toString();
			if (uriString.startsWith(FILE_PREFIX)) {
				uriString = uri.getPath();
			}
			this.binSearchReader = new BinSearchReader(uriString, sequenceSpanReader);
		}
		catch (Exception x) {
			this.binSearchReader = null;
		}
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	@Override
	public void init(){
		if (this.isInitialized){
			return;
		}
		super.init();
		lineProcessor.init(uri);
		for (String seqID : binSearchReader.getSequenceNames()) {
			BioSeq seq = group.getSeq(seqID);
			if (seq == null) {
				int length = 1000000000;
				seq = group.addSeq(seqID, length);
				Logger.getLogger(SymLoaderBinSearch.class.getName()).log(Level.INFO,
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
		return lineProcessor.processLines(seq, binSearchReader.query(seqID));
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) {
		init();
		String seqID = seqs.get(overlapSpan.getBioSeq());
		LineReader lineReader = binSearchReader.query(seqID, overlapSpan.getStart() + 1, overlapSpan.getEnd());
		if (lineReader == null) {
			return new ArrayList<SeqSymmetry>();
		}
		return lineProcessor.processLines(overlapSpan.getBioSeq(), lineReader);
    }
	
	public static SymLoader getSymLoader(SymLoader sym){
		try {
			URI uri = new URI(sym.uri.toString());
			if(LocalUrlCacher.isValidURI(uri)){
				String uriString = sym.uri.toString();
				if (uriString.startsWith(FILE_PREFIX)) {
					uriString = sym.uri.getPath();
				}
				return new SymLoaderBinSearch(sym.uri, sym.featureName, sym.group, (LineProcessor)sym, (SequenceSpanReader)sym);
			}
		} catch (URISyntaxException ex) {
			Logger.getLogger(SymLoaderBinSearch.class.getName()).log(Level.SEVERE, null, ex);
		}
		return sym;
	}
}

