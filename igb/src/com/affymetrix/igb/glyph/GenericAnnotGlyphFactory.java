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

import com.affymetrix.genoviz.bioviews.GlyphI;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.parsers.TrackLineParser;

import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;

import java.awt.Color;
import java.util.*;



/**
 *
 * @version $Id$
 */
public final class GenericAnnotGlyphFactory implements MapViewGlyphFactoryI {

	private static final boolean DEBUG = false;

	/** Set to true if the we can assume the container SeqSymmetry being passed
	 *  to addLeafsToTier has all its leaf nodes at the same depth from the top.
	 */
	private static Class default_eparent_class = (new EfficientLineContGlyph()).getClass();
	private static Class default_echild_class = (new EfficientFillRectGlyph()).getClass();
	private static Class default_elabelled_parent_class = (new EfficientLabelledLineGlyph()).getClass();
	private static int DEFAULT_THICK_HEIGHT = 25;
	private static int DEFAULT_THIN_HEIGHT = 15;
	private SeqMapView gviewer;
	private int glyph_depth = 2;  // default is depth = 2 (only show leaf nodes and parents of leaf nodes)
	private Class parent_glyph_class;
	private Class child_glyph_class;
	private Class parent_labelled_glyph_class;

	public GenericAnnotGlyphFactory() {
		parent_glyph_class = default_eparent_class;
		child_glyph_class = default_echild_class;
		parent_labelled_glyph_class = default_elabelled_parent_class;
	}

	public void init(Map options) {
		if (DEBUG) {
			System.out.println("     @@@@@@@@@@@@@     in GenericAnnotGlyphFactory.init(), props: " + options);
		}

		String parent_glyph_name = (String) options.get("parent_glyph");
		if (parent_glyph_name != null) {
			try {
				parent_glyph_class = Class.forName(parent_glyph_name);
			} catch (Exception ex) {
				System.err.println();
				System.err.println("WARNING: Class for parent glyph not found: " + parent_glyph_name);
				System.err.println();
				parent_glyph_class = default_eparent_class;
			}
		}
		String child_glyph_name = (String) options.get("child_glyph");
		if (child_glyph_name != null) {
			try {
				child_glyph_class = Class.forName(child_glyph_name);
			} catch (Exception ex) {
				System.err.println();
				System.err.println("WARNING: Class for child glyph not found: " + child_glyph_name);
				System.err.println();
				child_glyph_class = default_echild_class;
			}
		}
	}

	public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
		gviewer = smv;

		String meth = BioSeq.determineMethod(sym);

		if (meth != null) {
			IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
			glyph_depth = style.getGlyphDepth();

			TierGlyph[] tiers = smv.getTiers(meth, false, style);
			if (style.getSeparate()) {
				addLeafsToTier(sym, tiers[0], tiers[1], glyph_depth);
			} else {
				// use only one tier
				addLeafsToTier(sym, tiers[0], tiers[0], glyph_depth);
			}
		} else {  // keep recursing down into child syms if parent sym has no "method" property
			int childCount = sym.getChildCount();
			for (int i = 0; i < childCount; i++) {
				SeqSymmetry childSym = sym.getChild(i);
				createGlyph(childSym, gviewer);
			}
		}
	}

	private static int getDepth(SeqSymmetry sym) {
		int depth = 1;
		SeqSymmetry current = sym;
		while (current.getChildCount() != 0) {
			current = current.getChild(0);
			depth++;
		}
		return depth;
	}


	private void addLeafsToTier(SeqSymmetry sym,
			TierGlyph ftier, TierGlyph rtier,
			int desired_leaf_depth) {
		int depth = getDepth(sym);
		if (depth > desired_leaf_depth || sym instanceof TypeContainerAnnot) {
			for (int i = 0; i < sym.getChildCount(); i++) {
				SeqSymmetry child = sym.getChild(i);
				addLeafsToTier(child, ftier, rtier, desired_leaf_depth);
			}
		} else {  // depth == desired_leaf_depth
			addToTier(sym, ftier, rtier, (depth >= 2));
		}
	}

	private static Color getSymColor(SeqSymmetry insym, IAnnotStyleExtended style) {
		boolean use_score_colors = style.getColorByScore();
		boolean use_item_rgb = "on".equalsIgnoreCase((String) style.getTransientPropertyMap().get(TrackLineParser.ITEM_RGB));

		if (!(use_score_colors || use_item_rgb)) {
			return style.getColor();
		}

		SeqSymmetry sym = insym;
		if (insym instanceof DerivedSeqSymmetry) {
			sym = (SymWithProps) getMostOriginalSymmetry(insym);
		}

		if (use_item_rgb && sym instanceof SymWithProps) {
			Color cc = (Color) ((SymWithProps) sym).getProperty(TrackLineParser.ITEM_RGB);
			if (cc != null) {
				return cc;
			}
		}
		if (use_score_colors && sym instanceof Scored) {
			float score = ((Scored) sym).getScore();
			if (score != Float.NEGATIVE_INFINITY && score > 0.0f) {
				return style.getScoreColor(score);
			}
		}

		return style.getColor();
	}

	/**
	 *  @param parent_and_child  Whether to draw this sym as a parent and
	 *    also draw its children, or to just draw the sym itself
	 *   (using the child glyph style).  If this is set to true, then
	 *    the symmetry must have a depth of at least 2.
	 */
	private GlyphI addToTier(SeqSymmetry insym,
			TierGlyph forward_tier,
			TierGlyph reverse_tier,
			boolean parent_and_child) {

		GlyphI g = null;

		try {
			if (parent_and_child && insym.getChildCount() > 0) {
				g = doTwoLevelGlyph(insym, forward_tier, reverse_tier);
			} else {
				// depth !>= 2, so depth <= 1, so _no_ parent, use child glyph instead...
				g = doSingleLevelGlyph(insym, forward_tier, reverse_tier);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return g;
	}


	private static Object getTheProperty(SeqSymmetry sym, String prop) {
		if (prop == null || (prop.trim().length() == 0)) {
			return null;
		}
		SeqSymmetry original = getMostOriginalSymmetry(sym);

		if (original instanceof SymWithProps) {
			return ((SymWithProps) original).getProperty(prop);
		}
		return null;
	}

	private static SeqSymmetry getMostOriginalSymmetry(SeqSymmetry sym) {
		if (sym instanceof DerivedSeqSymmetry) {
			return getMostOriginalSymmetry(((DerivedSeqSymmetry) sym).getOriginalSymmetry());
		} else {
			return sym;
		}
	}

	private GlyphI doSingleLevelGlyph(SeqSymmetry insym, TierGlyph forward_tier, TierGlyph reverse_tier)
			throws InstantiationException, IllegalAccessException {

		AffyTieredMap map = gviewer.getSeqMap();
		BioSeq annotseq = gviewer.getAnnotatedSeq();
		BioSeq coordseq = gviewer.getViewSeq();
		SeqSymmetry sym = insym;
		boolean same_seq = (annotseq == coordseq);

		if (!same_seq) {
			sym = gviewer.transformForViewSeq(insym, annotseq);
		}

		SeqSpan pspan = gviewer.getViewSeqSpan(sym);
		if (pspan == null || pspan.getLength() == 0) {
			return null;
		}  // if no span corresponding to seq, then return;

		TierGlyph the_tier = pspan.isForward() ? forward_tier : reverse_tier;

		// I hate having to do this cast to IAnnotStyleExtended.  But how can I avoid it?
		IAnnotStyleExtended the_style = (IAnnotStyleExtended) the_tier.getAnnotStyle();
		GlyphI pglyph = determineGlyph(child_glyph_class, parent_labelled_glyph_class, the_style, insym, the_tier, pspan, map, sym);

		the_tier.addChild(pglyph);
		return pglyph;
	}


	/**
	 *   Creation of genoviz Glyphs for rendering
	 *      a two-level symmetry (parent with children) in the SeqMapView
	 *      includes transformations used by slice view and other alternative coordinate systems

	 */
	private GlyphI doTwoLevelGlyph(SeqSymmetry insym, TierGlyph forward_tier, TierGlyph reverse_tier)
			throws InstantiationException, IllegalAccessException {

		AffyTieredMap map = gviewer.getSeqMap();
		BioSeq annotseq = gviewer.getAnnotatedSeq();
		BioSeq coordseq = gviewer.getViewSeq();
		SeqSymmetry sym = insym;
		boolean same_seq = (annotseq == coordseq);

		if (!same_seq) {
			sym = gviewer.transformForViewSeq(insym, annotseq);
		}

		SeqSpan pspan = gviewer.getViewSeqSpan(sym);

		if (pspan == null || pspan.getLength() == 0) {
			return null;
		}  // if no span corresponding to seq, then return;

		TierGlyph the_tier = pspan.isForward() ? forward_tier : reverse_tier;

		// I hate having to do this cast to AnnotStyle.  But how can I avoid it?
		IAnnotStyleExtended the_style = (IAnnotStyleExtended) the_tier.getAnnotStyle();
		GlyphI pglyph = determineGlyph(parent_glyph_class, parent_labelled_glyph_class, the_style, insym, the_tier, pspan, map, sym);

		// call out to handle rendering to indicate if any of the children of the
		//    orginal annotation are completely outside the view
		addChildren(insym, same_seq, sym, the_style, annotseq, pglyph, map, coordseq);

		the_tier.addChild(pglyph);
		return pglyph;
	}


	private static GlyphI determineGlyph(Class glyphClass, Class labelledGlyphClass, IAnnotStyleExtended the_style, SeqSymmetry insym, TierGlyph the_tier, SeqSpan pspan, AffyTieredMap map, SeqSymmetry sym) throws IllegalAccessException, InstantiationException {
		GlyphI pglyph = null;
		// Note: Setting parent height (pheight) larger than the child height (cheight)
		// allows the user to select both the parent and the child as separate entities
		// in order to look at the properties associated with them.  Otherwise, the method
		// EfficientGlyph.pickTraversal() will only allow one to be chosen.
		double pheight = DEFAULT_THICK_HEIGHT + 0.0001;
		String label_field = the_style.getLabelField();
		boolean use_label = label_field != null && (label_field.trim().length() > 0);
		if (use_label) {
			EfficientLabelledGlyph lglyph = (EfficientLabelledGlyph) labelledGlyphClass.newInstance();
			Object property = getTheProperty(insym, label_field);
			String label = (property == null) ? "" : property.toString();
			if (the_tier.getDirection() == TierGlyph.Direction.REVERSE) {
				lglyph.setLabelLocation(GlyphI.SOUTH);
			} else {
				lglyph.setLabelLocation(GlyphI.NORTH);
			}
			lglyph.setLabel(label);
			pheight = 2 * pheight;
			pglyph = lglyph;
		} else {
			pglyph = (GlyphI) glyphClass.newInstance();
		}
		pglyph.setCoords(pspan.getMin(), 0, pspan.getLength(), pheight);
		pglyph.setColor(getSymColor(insym, the_style));
		map.setDataModelFromOriginalSym(pglyph, sym);
		return pglyph;
	}


	private void addChildren(SeqSymmetry insym, boolean same_seq, SeqSymmetry sym, IAnnotStyleExtended the_style, BioSeq annotseq, GlyphI pglyph, AffyTieredMap map, BioSeq coordseq) throws InstantiationException, IllegalAccessException {
		SeqSpan cdsSpan = null;
		SeqSymmetry cds_sym = null;
		if ((insym instanceof SupportsCdsSpan) && ((SupportsCdsSpan) insym).hasCdsSpan()) {
			cdsSpan = ((SupportsCdsSpan) insym).getCdsSpan();
			MutableSeqSymmetry tempsym = new SimpleMutableSeqSymmetry();
			tempsym.addSpan(new SimpleMutableSeqSpan(cdsSpan));
			if (!same_seq) {
				SeqUtils.transformSymmetry(tempsym, gviewer.getTransformPath());
				cdsSpan = gviewer.getViewSeqSpan(tempsym);
			}
			cds_sym = tempsym;
		}
		// call out to handle rendering to indicate if any of the children of the
		//    orginal annotation are completely outside the view

		int childCount = sym.getChildCount();
		List<SeqSymmetry> outside_children = new ArrayList<SeqSymmetry>();
		for (int i = 0; i < childCount; i++) {
			SeqSymmetry child = null;
			SeqSpan cspan = null;
			child = sym.getChild(i);
			cspan = gviewer.getViewSeqSpan(child);
			if (cspan == null) {
				// if no span for view, then child is either to left or right of view
				outside_children.add(child); // collecting children outside of view to handle later
			} else {
				GlyphI cglyph;
				if (cspan.getLength() == 0) {
					cglyph = new DeletionGlyph();
				} else {
					cglyph = (GlyphI) child_glyph_class.newInstance();
				}
				double cheight = DEFAULT_THICK_HEIGHT;
				Color child_color = getSymColor(child, the_style);
				if (cdsSpan != null) {
					cheight = DEFAULT_THIN_HEIGHT;
					if (SeqUtils.contains(cdsSpan, cspan)) {
						cheight = DEFAULT_THICK_HEIGHT;
					} else if (SeqUtils.overlap(cdsSpan, cspan)) {
						SeqSymmetry cds_sym_2 = SeqUtils.intersection(cds_sym, child, annotseq);
						SeqSymmetry cds_sym_3 = cds_sym_2;
						if (!same_seq) {
							cds_sym_3 = gviewer.transformForViewSeq(cds_sym_2, annotseq);
						}
						SeqSpan cds_span = gviewer.getViewSeqSpan(cds_sym_3);
						if (cds_span != null) {
							GlyphI cds_glyph;
							if (cspan.getLength() == 0) {
								cds_glyph = new DeletionGlyph();
							} else {
								cds_glyph = (GlyphI) child_glyph_class.newInstance();
							}
							cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), DEFAULT_THICK_HEIGHT);
							cds_glyph.setColor(child_color); // CDS same color as exon
							pglyph.addChild(cds_glyph);
							map.setDataModelFromOriginalSym(cds_glyph, cds_sym_3);
						}
					}
				}
				cglyph.setCoords(cspan.getMin(), 0, cspan.getLength(), cheight);
				cglyph.setColor(child_color);
				pglyph.addChild(cglyph);
				map.setDataModelFromOriginalSym(cglyph, child);
			}
		}
		// call out to handle rendering to indicate if any of the children of the
		//    orginal annotation are completely outside the view
		DeletionGlyph.handleEdgeRendering(outside_children, pglyph, annotseq, coordseq, 0.0, DEFAULT_THIN_HEIGHT);
	}
	
}
