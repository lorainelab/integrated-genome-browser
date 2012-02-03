/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.glyph;

import java.awt.Color;
import java.util.*;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;

public final class CoverageSummarizerFactory implements MapViewGlyphFactoryI {

	private static final int default_glyph_height = 50;
	private int glyph_height = default_glyph_height;

	/*
	 *  style options:
	 *    COVERAGE: standard viualization, shows plot of for each pixel in view, base-pair coverage
	 *        (coords covered by spans / total coords in pixel)
	 *    SIMPLE: just shows what regions are covered by spans as solid blocks
	 */
	private int style = CoverageSummarizerGlyph.DEFAULT_STYLE;

	public CoverageSummarizerFactory() {
	}

	public String getName(){
		return "coverage";
	}
	
	public void init(Map<String, Object> options) {
		if (options.get("height") != null) {
			try {
				glyph_height = Integer.parseInt((String) options.get("height"));
			} catch (Exception ex) {
				ex.printStackTrace();
				glyph_height = default_glyph_height;
			}
		}

		String style_name = (String) options.get("style");
		if (style_name != null) {
			if (style_name.equalsIgnoreCase("coverage")) {
				style = CoverageSummarizerGlyph.COVERAGE;
			}
			if (style_name.equalsIgnoreCase("simple")) {
				style = CoverageSummarizerGlyph.SIMPLE;
			}
		}
	}

	public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI gviewer) {
		String meth = BioSeq.determineMethod(sym);
		ITrackStyleExtended annot_style = TrackStyle.getInstance(meth, false);

		if (meth != null) {
			TierGlyph[] tiers = gviewer.getTiers(false, // next_to_axis = false
					annot_style, true);
			//TierGlyph ftier = tiers[0]; // ignore the reverse tier

			createGlyph(sym, tiers[0], tiers[1], annot_style, style, glyph_height, gviewer);
		}
	}
	
	public static void createGlyph(SeqSymmetry sym, TierGlyph ftier, TierGlyph rtier, 
			ITrackStyleExtended annot_style, int style, int glyph_height, SeqMapViewExtendedI gviewer) {

		Color background_color;
		Color glyph_color;
		if (annot_style == null) {
			glyph_color = TrackStyle.getDefaultInstance().getForeground();
			background_color = TrackStyle.getDefaultInstance().getBackground();
		} else {
			glyph_color = annot_style.getForeground();
			background_color = annot_style.getBackground();
		}

		BioSeq annotseq = gviewer.getAnnotatedSeq();
		BioSeq coordseq = gviewer.getViewSeq();
		SeqSymmetry tsym = sym;
		// transform symmetry to coordseq if annotseq != coordseq, like in the slice viewer
		if (annotseq != coordseq) {
			tsym = gviewer.transformForViewSeq(sym, gviewer.getAnnotatedSeq());
		}

		int child_count = tsym.getChildCount();
		// initializing list internal array length to child count to reduce list expansions...
		List<SeqSpan> leaf_spans = null;
		
		if(annot_style != null && annot_style.getSeparate()){
			leaf_spans = new ArrayList<SeqSpan>(child_count/2);
			SeqUtils.collectLeafSpans(tsym, coordseq, true, leaf_spans);
			addCoverageGlyph(background_color, leaf_spans, glyph_color, style, coordseq, glyph_height, ftier, gviewer, tsym);
			
			leaf_spans = new ArrayList<SeqSpan>(child_count/2);
			SeqUtils.collectLeafSpans(tsym, coordseq, false, leaf_spans);
			addCoverageGlyph(background_color, leaf_spans, glyph_color, style, coordseq, glyph_height, rtier, gviewer, tsym);
			
		}else{
			leaf_spans = new ArrayList<SeqSpan>(child_count);
			SeqUtils.collectLeafSpans(tsym, coordseq, leaf_spans);
			addCoverageGlyph(background_color, leaf_spans, glyph_color, style, coordseq, glyph_height, ftier, gviewer, tsym);
		}
		
	}
	
	private static void addCoverageGlyph(Color background_color, List<SeqSpan> leaf_spans, Color glyph_color, 
			int style, BioSeq coordseq, int glyph_height, TierGlyph ftier, SeqMapViewExtendedI gviewer, SeqSymmetry tsym) {
		CoverageSummarizerGlyph cov = new CoverageSummarizerGlyph();
		cov.setHitable(false);
		cov.setBackgroundColor(background_color);
		cov.setCoveredIntervals(leaf_spans);
		cov.setColor(glyph_color);
		cov.setStyle(style);
		cov.setCoords(0, 0, coordseq.getLength(), glyph_height);
		ftier.addChild(cov);

		gviewer.setDataModelFromOriginalSym(cov, tsym);
	}

	public boolean isSeqSymmetrySupported(SeqSymmetry sym) {
		return true;
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style) {
		return null;
	}
}
