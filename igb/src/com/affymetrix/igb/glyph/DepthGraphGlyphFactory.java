package com.affymetrix.igb.glyph;

import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;

/**
 *
 * @author hiralv
 */
public class DepthGraphGlyphFactory implements MapViewGlyphFactoryI {
	
	public String getName(){
		return "depth";
	}
	
	public void init(Map<String, Object> options) { }

	public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI smv) {

		String meth = BioSeq.determineMethod(sym);
		
		if (meth == null) {
			return;
		}
	
		ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
		
		TierGlyph[] tiers = smv.getTiers(style, true);
		if (style.getSeparate()) {
			addDepthGraph(meth, sym, tiers[0], tiers[1], smv);			
		} else {
			// use only one tier
			addDepthGraph(meth, sym, tiers[0], tiers[0], smv);
		}

	}
	
	private void addDepthGraph(String meth, SeqSymmetry sym, TierGlyph ftier, TierGlyph rtier, SeqMapViewExtendedI smv){
		
		SeqSpan pspan = smv.getViewSeqSpan(sym);
		
		if (pspan == null || pspan.getLength() == 0) {
			return;
		}
		
		BioSeq seq = smv.getAnnotatedSeq();
		java.util.List<SeqSymmetry> syms = new java.util.ArrayList<SeqSymmetry>();
		syms.add(sym);
		GraphSym gsym = null;
		
		if (ftier == rtier) {
			gsym = SeqSymSummarizer.getSymmetrySummary(syms, seq, false, meth);
			addToParent(pspan, gsym, ftier, smv);
		} else {
			gsym = SeqSymSummarizer.getSymmetrySummary(syms, seq, false, meth, true);
			addToParent(pspan, gsym, ftier, smv);
			
			gsym = SeqSymSummarizer.getSymmetrySummary(syms, seq, false, meth, false);
			addToParent(pspan, gsym, rtier, smv);
		}

	}
	
	private void addToParent(SeqSpan pspan, GraphSym gsym, TierGlyph tier, SeqMapViewExtendedI smv){
		if(gsym != null){
			GraphState state = new GraphState(tier.getAnnotStyle());
			GraphGlyph graph_glyph = new GraphGlyph(gsym, state);
			graph_glyph.setGraphStyle(GraphType.STAIRSTEP_GRAPH);
			graph_glyph.setCoords(pspan.getMin(), 0, pspan.getLength(), tier.getCoordBox().getHeight());
			smv.setDataModelFromOriginalSym(graph_glyph, gsym);
			addToTier(tier, graph_glyph, gsym);
		}
	}

	public void addToTier(TierGlyph tier, GlyphI glyph, SeqSymmetry sym) {
		tier.addChild(glyph);
		tier.setInfo(sym);
	}
	
	public boolean isFileSupported(String format) {
		return true;
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public final SeqMapViewExtendedI getSeqMapView(){
		return null;
	}
}
