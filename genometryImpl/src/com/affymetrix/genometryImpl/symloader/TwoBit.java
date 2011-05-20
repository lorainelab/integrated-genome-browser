package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.TwoBitParser;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 */
public class TwoBit extends SymLoader {

	private static final List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("raw");
		pref_list.add("2bit");
	}
	private final Map<BioSeq, SearchableCharIterator> chrMap = new HashMap<BioSeq, SearchableCharIterator>();
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		// BAM files are generally large, so only allow loading visible data.
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
	}
	
	public TwoBit(URI uri, AnnotatedSeqGroup group, String seqName){
		super(uri, "", group);
		this.isResidueLoader = true;
		try {
			BioSeq retseq = TwoBitParser.parse(uri, group, seqName);
			if(retseq != null){
				chrMap.put(retseq, retseq.getResiduesProvider());
				retseq.removeResidueProvider();
			}
		} catch (Exception ex) {
			Logger.getLogger(TwoBit.class.getName()).log(Level.SEVERE, null, ex);
		}
		super.init();
	}
	
	public TwoBit(URI uri, String featureName, AnnotatedSeqGroup group) {
		super(uri, "", group);
		this.isResidueLoader = true;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		try {
			List<BioSeq> seqs = TwoBitParser.parse(uri, group);
			if(seqs != null){
				for(BioSeq seq : seqs){
					chrMap.put(seq, seq.getResiduesProvider());
					seq.removeResidueProvider();
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(TwoBit.class.getName()).log(Level.SEVERE, null, ex);
		}
		super.init();
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	@Override
	public List<BioSeq> getChromosomeList(){
		init();
		return new ArrayList<BioSeq>(chrMap.keySet());
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		init();
		BioSeq seq = span.getBioSeq();
		if(chrMap.containsKey(seq)){
			return chrMap.get(seq).substring(span.getMin(), span.getMax());
		}

		Logger.getLogger(TwoBit.class.getName()).log(Level.WARNING,"Seq {0} not present {1}",new Object[]{seq.getID(), uri.toString()});
		return "";
	}

	@Override
	public List<String> getFormatPrefList() {
		return pref_list;
	}
}
