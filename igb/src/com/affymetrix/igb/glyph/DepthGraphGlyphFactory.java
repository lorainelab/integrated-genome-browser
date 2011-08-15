package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

import com.affymetrix.igb.shared.ExtendedMapViewGlyphFactoryI;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;

/**
 *
 * @author hiralv
 */
public class DepthGraphGlyphFactory implements ExtendedMapViewGlyphFactoryI {
	
	public String getName(){
		return "depth";
	}
	
	public void init(java.util.Map options) { }

	public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI smv) {

		String meth = BioSeq.determineMethod(sym);
		SeqSpan pspan = smv.getViewSeqSpan(sym);
		if (meth == null || pspan == null || pspan.getLength() == 0) {
			return;
		}
	
		ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
		
		TierGlyph[] tiers = smv.getTiers(false, style, true);
		if (style.getSeparate()) {
			addDepthGraph(meth, sym, pspan, smv.getAnnotatedSeq(), tiers[0], tiers[1]);			
		} else {
			// use only one tier
			addDepthGraph(meth, sym, pspan, smv.getAnnotatedSeq(), tiers[0], tiers[0]);
		}

	}
	

	private void addDepthGraph(String meth, SeqSymmetry sym, 
			SeqSpan pspan, BioSeq seq, TierGlyph ftier, TierGlyph rtier){
		
		java.util.List<SeqSymmetry> syms = new java.util.ArrayList<SeqSymmetry>();
		syms.add(sym);
		GraphSym gsym = null;
		
		if (ftier == rtier) {
			gsym = SeqSymSummarizer.getSymmetrySummary(syms, seq, false, meth);
			addToParent(meth, pspan, gsym, ftier);
		} else {
			gsym = SeqSymSummarizer.getSymmetrySummary(syms, seq, false, meth, true);
			addToParent(meth, pspan, gsym, ftier);
			
			gsym = SeqSymSummarizer.getSymmetrySummary(syms, seq, false, meth, false);
			addToParent(meth, pspan, gsym, rtier);
		}

	}
	
	private void addToParent(String meth, SeqSpan pspan, GraphSym gsym, TierGlyph tier){
		if(gsym != null){
			GraphState state = new GraphState(meth, tier.getAnnotStyle());
			GraphGlyph graph_glyph = new GraphGlyph(gsym, state);
			graph_glyph.drawHandle(false);
			graph_glyph.setSelectable(false);
			graph_glyph.setGraphStyle(GraphType.STAIRSTEP_GRAPH);
			graph_glyph.setCoords(pspan.getMin(), 0, pspan.getLength(), tier.getPreferredHeight());
			tier.addChild(graph_glyph);
			tier.setInfo(gsym);
		}
	}

	public boolean isFileSupported(String format) {
		return true;
	}
}
