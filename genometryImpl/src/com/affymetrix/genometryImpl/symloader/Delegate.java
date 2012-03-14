
package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.symmetry.*;
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
public class Delegate extends QuickLoadSymLoader {
	public static final String EXT = "igbtrack";
	private static final List<LoadUtils.LoadStrategy> defaultStrategyList = new ArrayList<LoadUtils.LoadStrategy>();
	static {
		defaultStrategyList.add(LoadUtils.LoadStrategy.NO_LOAD);
		defaultStrategyList.add(LoadUtils.LoadStrategy.VISIBLE);
	}
	
	
	private final Operator operator;
	private final List<DelegateParent> dps;
	private final List<LoadUtils.LoadStrategy> strategyList;
	
	public Delegate(URI uri, String featureName, GenericVersion version,
			Operator operator, List<DelegateParent> dps) {
		super(uri, featureName, version, null);
		this.operator = operator;
		this.dps = dps;
		this.extension = EXT;
		strategyList = new ArrayList<LoadUtils.LoadStrategy>();
		strategyList.addAll(defaultStrategyList);
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
		
		if(dps.isEmpty())
			return result;
		
		for(DelegateParent dp : dps){
			if(overlapSpan.getBioSeq().getAnnotation(dp.name) == null)
				return result;
			
			syms.add(dp.getSeqSymmetry(overlapSpan.getBioSeq()));
		}
		
		SymWithProps sym = (SymWithProps) operator.operate(overlapSpan.getBioSeq(), syms);
		sym.setProperty("method", uri.toString());
		sym.setProperty("meth", uri.toString());
		sym.setProperty("id", uri.toString());
		result.add(sym);
		return result;
    }
	
	@Override
	public boolean loadFeatures(final SeqSpan overlapSpan, final GenericFeature feature)
			throws OutOfMemoryError, IOException {
		boolean notUpdatable = false;

		if (dps == null || dps.isEmpty()) {
			return false;
		}

		for (DelegateParent dp : dps) {
			if (!dp.feature.isVisible()) {
				notUpdatable = true;
				Thread.currentThread().interrupt();
				break;
			}

			while (dp.feature.optimizeRequest(overlapSpan) != null) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					Logger.getLogger(Delegate.class.getName()).log(Level.SEVERE, null, ex);
					return false;
				}
			}
		}

		if (notUpdatable) {
			for(DelegateParent dp : dps){
				dp.clear();
			}
			dps.clear();
			strategyList.remove(LoadUtils.LoadStrategy.VISIBLE);
			feature.setLoadStrategy(LoadUtils.LoadStrategy.NO_LOAD);
			return false;
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
				feature.addMethod(uri.toString());
			}
			else {
				// We assume that if there are any GraphSyms, then we're dealing with a list of GraphSyms.
				for(SeqSymmetry feat : results) {
					//grafs.add((GraphSym)feat);
					if (feat instanceof GraphSym) {
						GraphSymUtils.addChildGraph((GraphSym) feat, uri.toString(), ((GraphSym) feat).getGraphName(), uri.toString(), span);
						feature.addMethod(uri.toString());
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
		
		feature.addMethod(uri.toString());
		return true;
	}
	
	public static class DelegateParent{
		String name;
		Boolean direction;
		GenericFeature feature;
		
		public DelegateParent(String name, Boolean direction, GenericFeature feature){
			this.name = name;
			this.direction = direction;
			this.feature = feature;
		}
		
		SeqSymmetry getSeqSymmetry(BioSeq seq){
			if(direction == null){
				return seq.getAnnotation(name);
			}else{
				return getChilds(seq, name, direction.booleanValue());
			}
		}
		
		private SeqSymmetry getChilds(BioSeq seq, String name, boolean isForward) {
			SeqSymmetry parentSym = seq.getAnnotation(name);
			TypeContainerAnnot tca = new TypeContainerAnnot(name);
			
			SeqSymmetry sym;
			SeqSpan span;
			for (int i = 0; i < parentSym.getChildCount(); i++) {
				sym = parentSym.getChild(i);
				span = sym.getSpan(seq);

				if (span == null || span.getLength() == 0) {
					continue;
				}

				if (span.isForward() == isForward) {
					tca.addChild(sym);
				}
			}
			
			return tca;
		}
		
		void clear(){
			name = null;
			feature = null;
		}
	}
}
