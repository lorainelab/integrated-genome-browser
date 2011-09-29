package com.affymetrix.genometryImpl.symloader;

import java.util.List;
import java.util.ArrayList;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithResidues;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;

/**
 *
 * @author hiralv
 */
public class ResidueTrackSymLoader extends SymLoader {
	
	private final SymLoader symL;
	
	public ResidueTrackSymLoader(SymLoader loader) {
		super(loader.uri, loader.featureName, loader.group);
		this.symL = loader;
	}
	
	@Override
	public void init() throws Exception {
		symL.init();
		this.isInitialized = true;
	}
	
	@Override
	public List<LoadStrategy> getLoadChoices() {
		return symL.getLoadChoices();
	}

	@Override
	public List<BioSeq> getChromosomeList() throws Exception {
		init();
		return symL.getChromosomeList();
	}
	
	@Override
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
		init();
		return getResidueTrack(new SimpleSeqSpan(0, seq.getLength(), seq));
    }
	
	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
		init();
		return getResidueTrack(overlapSpan);
    }
	
	private List<? extends SeqSymmetry> getResidueTrack(SeqSpan span) throws Exception{
		List<SeqSymmetry> list = new ArrayList<SeqSymmetry>();
		list.add(new SimpleSymWithResidues(uri.toString(), span.getBioSeq(), span.getStart(), span.getEnd(), "", 
				0.0f, span.isForward(), span.getStart(), span.getEnd(), null, null, symL.getRegionResidues(span)));
		return list;
	}
}
