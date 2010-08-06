package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.comparator.BioSeqComparator;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import java.util.List;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author hiralv
 */
public class SymLoaderInstNC extends SymLoader{

	private final List<BioSeq> chromosomeList = new ArrayList<BioSeq>();
	
	private static List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.GENOME);
	}
	
	public SymLoaderInstNC(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();
		getGenome();
		chromosomeList.addAll(group.getSeqList());
		Collections.sort(chromosomeList,new BioSeqComparator());
	}

	@Override
	public List<BioSeq> getChromosomeList(){
		init();
		return chromosomeList;
	}
	
}
