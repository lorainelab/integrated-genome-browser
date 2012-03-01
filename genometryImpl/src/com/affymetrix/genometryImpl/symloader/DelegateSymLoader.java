
package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LoadUtils;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class DelegateSymLoader extends QuickLoadSymLoader {
	private final Operator operator;
	private final List<String> symsStr;
	
	public DelegateSymLoader(URI uri, String featureName, GenericVersion version,
			Operator operator, List<String> symsStr) {
		super(uri, featureName, version, null);
		this.operator = operator;
		this.symsStr = symsStr;
	}
	
	private static final List<LoadUtils.LoadStrategy> strategyList = new ArrayList<LoadUtils.LoadStrategy>();
	static {
		strategyList.add(LoadUtils.LoadStrategy.NO_LOAD);
		strategyList.add(LoadUtils.LoadStrategy.VISIBLE);
	}
	
	/**
	 * Return possible strategies to load this URI.
	 * @return
	 */
	@Override
	public List<LoadUtils.LoadStrategy> getLoadChoices() {
		return strategyList;
	}
	
	/**
	 * Get list of chromosomes used in the file/uri.
	 * Especially useful when loading a file into an "unknown" genome
	 * @return List of chromosomes
	 */
	@Override
	public List<BioSeq> getChromosomeList() throws Exception {
		return version.group.getSeqList();
	}
	
	
	/**
     * Get a region of the chromosome.
     * @param seq - chromosome
     * @param overlapSpan - span of overlap
     * @return List of symmetries satisfying requirements
     */
	@Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
		List<SeqSymmetry> result = new ArrayList<SeqSymmetry>();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		
		for(String symStr : symsStr){
			if(overlapSpan.getBioSeq().getAnnotation(symStr) == null)
				return result;
			
			syms.add(overlapSpan.getBioSeq().getAnnotation(symStr));
		}
		
		SymWithProps sym = (SymWithProps) operator.operate(overlapSpan.getBioSeq(), syms);
		sym.setProperty("method", uri.toString());
		sym.setProperty("meth", uri.toString());
		sym.setProperty("id", uri.toString());
		result.add(sym);
		return result;
    }
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean loadFeatures(final SeqSpan overlapSpan, final GenericFeature feature)
			throws OutOfMemoryError, IOException {
		if (feature.typeObj instanceof List) {
			List<GenericFeature> features = (List<GenericFeature>) feature.typeObj;
			for (GenericFeature f : features) {
				while (f.optimizeRequest(overlapSpan) != null) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException ex) {
						Logger.getLogger(DelegateSymLoader.class.getName()).log(Level.SEVERE, null, ex);
						return false;
					}
				}
			}

		}
		return super.loadFeatures(overlapSpan, feature);
	}
	
	@Override
	protected boolean addSymmtries(final SeqSpan span, List<? extends SeqSymmetry> results, GenericFeature feature) {
		if (results == null || results.isEmpty()) {
			return false;
		}
		
		if (results.get(0) instanceof GraphSym) {
			GraphSym graphSym = (GraphSym)results.get(0);
			if (results.size() == 1 && graphSym.isSpecialGraph()) {
				BioSeq seq = graphSym.getGraphSeq();
				seq.addAnnotation(graphSym);
			}
			else {
				// We assume that if there are any GraphSyms, then we're dealing with a list of GraphSyms.
				for(SeqSymmetry feat : results) {
					//grafs.add((GraphSym)feat);
					if (feat instanceof GraphSym) {
						GraphSymUtils.addChildGraph((GraphSym) feat, uri.toString(), ((GraphSym) feat).getGraphName(), uri.toString(), span);
					}
				}
			}

			return true;
		}

		
		BioSeq seq = span.getBioSeq();
		if(seq.getAnnotation(uri.toString()) != null){
			//TODO: Check for other top level glyphs.
			TypeContainerAnnot sym = (TypeContainerAnnot) seq.getAnnotation(uri.toString());
			sym.clear();
		}
		
		for (SeqSymmetry feat : results) {
			seq.addAnnotation(feat);
		}
		
		return true;
	}
}
