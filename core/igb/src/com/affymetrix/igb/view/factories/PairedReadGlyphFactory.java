package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TrackConstants.DIRECTION_TYPE;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class PairedReadGlyphFactory extends AnnotationGlyphFactory {
	
	@Override
	protected void addLeafsToTier(SeqMapViewExtendedI gviewer, SeqSymmetry sym,
			TierGlyph ftier, TierGlyph rtier,
			int desired_leaf_depth) {
		int depth = SeqUtils.getDepthFor(sym);
		if (depth > 3 || sym instanceof TypeContainerAnnot) {
			int childCount = sym.getChildCount();
			for (int i = 0; i < childCount; i++) {
				addLeafsToTier(gviewer, sym.getChild(i), ftier, rtier, 3);
			}
		} else {  // depth == desired_leaf_depth
			addToTier(gviewer, sym, ftier, rtier, (depth >= 2));
		}
	}
		
	@Override
	protected void addTopChild(GlyphI glyph, SeqMapViewExtendedI gviewer, 
			boolean parent_and_child, SeqSymmetry pinsym, TierGlyph the_tier, 
			ITrackStyleExtended the_style, boolean labelInSouth, SeqSpan pspan, 
			SeqSymmetry psym, BioSeq annotseq, BioSeq coordseq, int child_height, 
			DIRECTION_TYPE direction_type) throws IllegalAccessException, InstantiationException {
		
		Color color = getSymColor(pinsym, the_style, pspan.isForward(), direction_type, the_style.getColorProvider());
		GlyphI pglyph = determineGlyph(parent_glyph_class, parent_labelled_glyph_class, the_tier, the_style, pinsym, labelInSouth, pspan, psym, gviewer, child_height, direction_type, color, annotseq);
		
		List<SeqSymmetry> children = new ArrayList<SeqSymmetry>();
		int childCount = pinsym.getChildCount();
		for (int i = 0; i < childCount; i++) {
			SeqSymmetry insym = pinsym.getChild(i);			
			SeqSymmetry sym = psym.getChild(i);
			pspan = gviewer.getViewSeqSpan(sym);
			children.add(sym);
			
			super.addTopChild(pglyph, gviewer, true, insym, the_tier, the_style, labelInSouth, pspan, sym, annotseq, coordseq, child_height, direction_type);
		}
		
		PairRelationSeqSymmetry pairRelation = PairRelationSeqSymmetry.findRelation(children, annotseq);
		if(pairRelation != null){
			FillRectGlyph middle = new FillRectGlyph();
			SeqSpan span = gviewer.getViewSeqSpan(pairRelation);
			middle.setCoords(span.getMin(), 0, span.getLength(), child_height);
			middle.setHitable(false);
			middle.setSelectable(false);
			pglyph.addChild(middle);
			if(pairRelation.getRelation() == PairRelationSeqSymmetry.INTERSECTION){
				middle.setColor(Color.BLACK);
			} else if (pairRelation.getRelation() == PairRelationSeqSymmetry.UNION){
				middle.setColor(Color.WHITE);
			} else {
				middle.setColor(the_style.getForeground());
			}
		}
		
		glyph.addChild(pglyph);
	}
	
	private static class PairRelationSeqSymmetry extends SimpleSeqSymmetry {
		static final int INTERSECTION = 0;
		static final int NONE = 1;
		static final int UNION = 2;
		
		private final int relation;
		
		PairRelationSeqSymmetry(int relation, SeqSpan span){
			super();
			this.relation = relation;
			spans = new ArrayList<SeqSpan>(1);
			spans.add(span);
		}
		
		int getRelation(){
			return relation;
		}
		
		static PairRelationSeqSymmetry findRelation(List<SeqSymmetry> children, BioSeq aseq){
			int relation = NONE;
			SeqSpan span = null;
			PairRelationSeqSymmetry pairRelation = null;
			
			if(children.isEmpty()){
				
			} else if (children.size() == 1) {
				span = children.get(0).getSpan(aseq);
			} else if (children.size() >= 2) {
				SeqSpan child1 = children.get(0).getSpan(aseq);
				SeqSpan child2 = children.get(1).getSpan(aseq);
				
				if(child1 != null && child2 != null){
					if(child2.getMin() < child1.getMax()){
						relation = INTERSECTION;
						span = new SimpleSeqSpan(child2.getMin(), child1.getMax(), aseq);
					}else{
						relation = UNION;
						span = new SimpleSeqSpan(child1.getMax(), child2.getMin(), aseq);
					}
				}
			
			}
			
			if(span != null){
				pairRelation = new PairRelationSeqSymmetry(relation, span);
			}
			return pairRelation;
		}
	}
}
