package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MisMatchGraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.ExtendedMapViewGlyphFactoryI;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MismatchPileupGlyph;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;

/**
 *
 * @author hiralv
 */
public class MismatchGraphGlyphFactory implements ExtendedMapViewGlyphFactoryI {

	private static final String[] supportedFormat = {"bam", "sam"};
	private final boolean createPileUp;
	private final String name;
	
	public String getName(){
		return name;
	}

	public void init(java.util.Map options) { }
	
	public MismatchGraphGlyphFactory(boolean createPileUp){
		this.createPileUp = createPileUp;
		if(createPileUp){
			name = "mismatch pileup";
		}else{
			name = "mismatch";
		}
	}
	
	public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI smv) {
		
		if (sym == null || sym.getChildCount() == 0) {
			return;
		}
		
		String meth = BioSeq.determineMethod(sym);
		SeqSpan pspan = smv.getViewSeqSpan(sym);
		
		if (meth == null || pspan == null || pspan.getLength() == 0) {
			return;
		}
		
		ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
		
		if(!isFileSupported(style.getFileType())){
			return;
		}
		
		BioSeq aseq = smv.getAnnotatedSeq();
		java.util.List<SeqSymmetry> syms = new java.util.ArrayList<SeqSymmetry>();
		syms.add(sym);
		
		int[] startEnd = getStartEnd(sym, aseq);
		SeqSpan loadSpan = new SimpleSeqSpan(startEnd[0], startEnd[1], aseq);

		//Load Residues
		if(!aseq.isAvailable(loadSpan)){
			if(!GeneralLoadView.getLoadView().loadResidues(loadSpan, true)){
				return;
			}
		}
		
		MisMatchGraphSym mgsym = SeqSymSummarizer.getMismatchGraph(syms, aseq, false, meth, startEnd[0], startEnd[1]);
		
		TierGlyph[] tiers = smv.getTiers(false, style, true);
		
		addToParent(pspan, mgsym, tiers[0]);
	
	}
	
	public boolean isFileSupported(String fileFormat){
		if(fileFormat == null)
			return false;
		
		for(String format : supportedFormat){
			if(format.equals(fileFormat)){
				return true;
			}
		}
		return false;
	}
		
	private void addToParent(SeqSpan pspan, MisMatchGraphSym gsym, TierGlyph tier){
		if(gsym != null){
			GraphState state = new GraphState(tier.getAnnotStyle());
			state.setGraphStyle(GraphType.FILL_BAR_GRAPH);
			GraphGlyph graph_glyph = null;
			if(createPileUp){
				graph_glyph = new MismatchPileupGlyph(gsym, state);
			}else{
				graph_glyph = new GraphGlyph(gsym, state);
			}
			graph_glyph.drawHandle(false);
			graph_glyph.setSelectable(false);
			graph_glyph.setCoords(pspan.getMin(), 0, pspan.getLength(), tier.getCoordBox().getHeight());
			tier.addChild(graph_glyph);
			tier.setInfo(gsym);
		}
	}
	
	static int[] getStartEnd(SeqSymmetry tsym, BioSeq aseq){
		int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;

		for(int i=0; i<tsym.getChildCount(); i++){
			SeqSymmetry childSym = tsym.getChild(i);
			SeqSpan span = childSym.getSpan(aseq);
			if(span.getMax() > end){
				end = span.getMax();
			}

			if(span.getMin() < start){
				start = span.getMin();
			}
		}

		return new int[]{start, end};
	}
}
