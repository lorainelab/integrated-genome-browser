package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.Scene;
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
	protected void addTopChild(GlyphI the_tier, SeqMapViewExtendedI gviewer, 
			boolean parent_and_child, SeqSymmetry pinsym, ITrackStyleExtended the_style, 
			boolean labelInSouth, SeqSpan pspan, SeqSymmetry psym, BioSeq annotseq, 
			BioSeq coordseq, int child_height, DIRECTION_TYPE direction_type) 
			throws IllegalAccessException, InstantiationException {
		GlyphI pglyph = determineGlyph(parent_glyph_class, parent_labelled_glyph_class, the_style, pinsym, labelInSouth, pspan, psym, gviewer, child_height, direction_type);
		
		List<SeqSymmetry> children = new ArrayList<SeqSymmetry>();
		int childCount = pinsym.getChildCount();
		for (int i = 0; i < childCount; i++) {
			SeqSymmetry insym = pinsym.getChild(i);			
			SeqSymmetry sym = psym.getChild(i);
			pspan = gviewer.getViewSeqSpan(sym);
			children.add(sym);
			
			super.addTopChild(pglyph, gviewer, true, insym, the_style, labelInSouth, pspan, sym, annotseq, coordseq, child_height, direction_type);
		}
		SeqSymmetry intersection = SeqUtils.intersection(children, annotseq);
		if(intersection != null && intersection.getSpan(annotseq) != null){
			FillRectGlyph middle = new FillRectGlyph();
			SeqSpan span = gviewer.getViewSeqSpan(intersection);
			if(span != null){
				middle.setCoords(span.getStart(), 0, span.getLength(), child_height);
			}
			middle.setColor(the_style.getForeground().darker());
			middle.setHitable(false);
			middle.setSelectable(false);
			pglyph.addChild(middle);
		}else{
			SeqSymmetry intron = SeqUtils.getIntronSym(psym, annotseq);
			if (intron != null) {
				FillRectGlyph middle = new FillRectGlyph();
				SeqSpan span = gviewer.getViewSeqSpan(intron);
				if (span != null) {
					middle.setCoords(span.getStart(), 0, span.getLength(), child_height);
				}
				middle.setColor(the_style.getForeground().brighter());
				middle.setHitable(false);
				middle.setSelectable(false);
				pglyph.addChild(middle);
				Scene.toBack(middle);
			}
		}
		the_tier.addChild(pglyph);
	}
	
}
