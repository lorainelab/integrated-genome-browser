package com.affymetrix.igb.glyph;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.MisMatchGraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;

/**
 *
 * @author hiralv
 */
public abstract class AbstractMismatchGraphGlyphFactory implements MapViewGlyphFactoryI {

	private static final String[] supportedFormat = {"bam", "sam"};
	
	public void init(Map<String, Object> options) { }
	
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
		
		//Force to use single track
		style.setSeparate(false);
		
		MisMatchGraphSym mgsym = getMismatchGraph(syms, aseq, false, meth, startEnd[0], startEnd[1]);
		
		TierGlyph[] tiers = smv.getTiers(style, true);
		
		addToParent(pspan, mgsym, tiers[0], smv);
	
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

	protected abstract MisMatchGraphSym getMismatchGraph(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int start, int end);

	protected abstract GraphGlyph getGraphGlyph(MisMatchGraphSym gsym, GraphState state);

	private void addToParent(SeqSpan pspan, MisMatchGraphSym gsym, TierGlyph tier, SeqMapViewExtendedI smv){
		if(gsym != null){
			GraphState state = new GraphState(tier.getAnnotStyle());
			state.setGraphStyle(GraphType.FILL_BAR_GRAPH);
			GraphGlyph graph_glyph = getGraphGlyph(gsym, state);
			graph_glyph.setCoords(pspan.getMin(), 0, pspan.getLength(), tier.getCoordBox().getHeight());
			smv.setDataModelFromOriginalSym(graph_glyph, gsym);
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
