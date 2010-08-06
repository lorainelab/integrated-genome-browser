package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.comparator.BioSeqComparator;
import com.affymetrix.genometryImpl.general.SymLoader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class SymLoaderInst extends SymLoader{

	private final List<BioSeq> chromosomeList = new ArrayList<BioSeq>();

	public SymLoaderInst(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
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
