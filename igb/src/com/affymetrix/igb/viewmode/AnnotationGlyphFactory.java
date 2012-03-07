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
package com.affymetrix.igb.viewmode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.TrackLineParser;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.genoviz.glyph.EfficientLabelledGlyph;
import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.genoviz.glyph.EfficientLineContGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.InsertionSeqGlyph;
import com.affymetrix.genoviz.glyph.PointedGlyph;

import com.affymetrix.igb.tiers.TrackConstants.DIRECTION_TYPE;
import com.affymetrix.igb.glyph.GlyphProcessorUtil;
import com.affymetrix.igb.shared.AlignedResidueGlyph;
import com.affymetrix.igb.shared.DeletionGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.StyleGlyphI;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.tiers.TrackConstants;

/**
 *
 * @version $Id: GenericAnnotGlyphFactory.java 10247 2012-02-10 16:36:20Z lfrohman $
 */
public class AnnotationGlyphFactory implements MapViewGlyphFactoryI {

	private final FileTypeCategory category;
	private static final boolean DEBUG = false;
	/** Set to true if the we can assume the container SeqSymmetry being passed
	 *  to addLeafsToTier has all its leaf nodes at the same depth from the top.
	 */
	private static Class<?> default_eparent_class = (new EfficientLineContGlyph()).getClass();
	private static Class<?> default_echild_class = (new FillRectGlyph()).getClass();
	private static Class<?> default_elabelled_parent_class = (new EfficientLabelledLineGlyph()).getClass();
//	private static final int DEFAULT_THICK_HEIGHT = 25;
//	private static final int DEFAULT_THIN_HEIGHT = 15;
	private SeqMapViewExtendedI gviewer;
	private Class<?> parent_glyph_class;
	private Class<?> child_glyph_class;
	private final Class<?> parent_labelled_glyph_class;

	public AnnotationGlyphFactory(FileTypeCategory category) {
		this.category = category;
		parent_glyph_class = default_eparent_class;
		child_glyph_class = default_echild_class;
		parent_labelled_glyph_class = default_elabelled_parent_class;
	}
	
	public void init(Map<String, Object> options) {
		if (DEBUG) {
			System.out.println("     @@@@@@@@@@@@@     in " + getClass().getSimpleName() + ".init(), props: " + options);
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

	public void setSeqMapView(SeqMapViewExtendedI gviewer) {
		this.gviewer = gviewer;
	}

	@Override
	public final SeqMapViewExtendedI getSeqMapView(){
		return gviewer;
	}
	
	public void createGlyph(final SeqSymmetry sym, SeqMapViewExtendedI smv) {
		gviewer = smv;
		String meth = BioSeq.determineMethod(sym);

		if (meth != null) {
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);			
			int glyph_depth = style.getGlyphDepth();

			StyleGlyphI[] tiers = smv.getTiers(style, true);
			tiers[0].setInfo(sym);
			tiers[1].setInfo(sym);
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
	
	protected void addLeafsToTier(SeqSymmetry sym,
			StyleGlyphI ftier, StyleGlyphI rtier,
			int desired_leaf_depth) {
		int depth = getDepth(sym);
		if (depth > desired_leaf_depth || sym instanceof TypeContainerAnnot) {
			int childCount = sym.getChildCount();
			for (int i = 0; i < childCount; i++) {
				addLeafsToTier(sym.getChild(i), ftier, rtier, desired_leaf_depth);
			}
		} else {  // depth == desired_leaf_depth
			addToTier(sym, ftier, rtier, (depth >= 2));
		}
	}

	/**
	 *  @param parent_and_child  Whether to draw this sym as a parent and
	 *    also draw its children, or to just draw the sym itself
	 *   (using the child glyph style).  If this is set to true, then
	 *    the symmetry must have a depth of at least 2.
	 */
	private void addToTier(SeqSymmetry insym,
			StyleGlyphI forward_tier,
			StyleGlyphI reverse_tier,
			boolean parent_and_child) {
		try {
			BioSeq annotseq = gviewer.getAnnotatedSeq();
			BioSeq coordseq = gviewer.getViewSeq();
			SeqSymmetry sym = insym;

			if (annotseq != coordseq) {
				sym = gviewer.transformForViewSeq(insym, annotseq);
			}

			SeqSpan pspan = gviewer.getViewSeqSpan(sym);
			if (pspan == null || pspan.getLength() == 0) {
				return;
			}  // if no span corresponding to seq, then return;

			StyleGlyphI the_tier = !pspan.isForward() ? reverse_tier : forward_tier;
			boolean labelInSouth = !pspan.isForward() && (reverse_tier != forward_tier);
			
			ITrackStyleExtended the_style = the_tier.getAnnotStyle();

			the_tier.addChild(determinePGlyph(
					parent_and_child, insym, the_style, labelInSouth, pspan, sym, annotseq, coordseq));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private GlyphI determinePGlyph(
			boolean parent_and_child, SeqSymmetry insym,
			ITrackStyleExtended the_style, boolean labelInSouth, SeqSpan pspan,
			SeqSymmetry sym, BioSeq annotseq, BioSeq coordseq)
			throws InstantiationException, IllegalAccessException {
		GlyphI pglyph = null;
		if (parent_and_child && insym.getChildCount() > 0) {
			pglyph = determineGlyph(parent_glyph_class, parent_labelled_glyph_class, the_style, insym, labelInSouth, pspan, sym, gviewer);
			// call out to handle rendering to indicate if any of the children of the
			//    original annotation are completely outside the view
			addChildren(insym, sym, pspan, the_style, annotseq, pglyph, coordseq);
			handleInsertionGlyphs(insym, annotseq, pglyph, the_style.getHeight());
		} else {
			// depth !>= 2, so depth <= 1, so _no_ parent, use child glyph instead...
			pglyph = determineGlyph(child_glyph_class, parent_labelled_glyph_class, the_style, insym, labelInSouth, pspan, sym, gviewer);
			GlyphI alignResidueGlyph = handleAlignedResidues(insym, annotseq);
			if(alignResidueGlyph != null){
				alignResidueGlyph.setCoordBox(pglyph.getCoordBox());
				pglyph.addChild(alignResidueGlyph);
			}
		}
		return pglyph;
	}

	private static GlyphI determineGlyph(
			Class<?> glyphClass, Class<?> labelledGlyphClass,
			ITrackStyleExtended the_style, SeqSymmetry insym, boolean labelInSouth,
			SeqSpan pspan, SeqSymmetry sym, SeqMapViewExtendedI gviewer)
			throws IllegalAccessException, InstantiationException {
		GlyphI pglyph = null;
		// Note: Setting parent height (pheight) larger than the child height (cheight)
		// allows the user to select both the parent and the child as separate entities
		// in order to look at the properties associated with them.  Otherwise, the method
		// EfficientGlyph.pickTraversal() will only allow one to be chosen.
		double pheight = the_style.getHeight() + 0.0001;
		String label_field = the_style.getLabelField();
		boolean use_label = label_field != null && !label_field.equals(TrackConstants.NO_LABEL) && (label_field.trim().length() > 0);
		if (use_label) {
			EfficientLabelledGlyph lglyph = (EfficientLabelledGlyph) labelledGlyphClass.newInstance();
			Object property = getTheProperty(insym, label_field);
			String label = (property == null) ? "" : property.toString();
			if (labelInSouth) {
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
		pglyph.setColor(getSymColor(insym, the_style, pspan.isForward(), DIRECTION_TYPE.valueFor(the_style.getDirectionType())));
		gviewer.setDataModelFromOriginalSym(pglyph, sym);
		return pglyph;
	}

	private static Object getTheProperty(SeqSymmetry sym, String prop) {
		if (prop == null || (prop.trim().length() == 0)) {
			return null;
		}
		SeqSymmetry original = getMostOriginalSymmetry(sym);

		if (original instanceof SymWithProps) {
			Object ret = ((SymWithProps) original).getProperty(prop);

			if(ret == null || ret.toString().length() == 0){
				ret = ((SymWithProps) original).getProperty(prop.toLowerCase());
			}

			if(ret == null || ret.toString().length() == 0){
				ret = ((SymWithProps) original).getProperty(prop.toUpperCase());
			}

			return ret;
		}
		return null;
	}

	private static SeqSymmetry getMostOriginalSymmetry(SeqSymmetry sym) {
		if (sym instanceof DerivedSeqSymmetry) {
			return getMostOriginalSymmetry(((DerivedSeqSymmetry) sym).getOriginalSymmetry());
		}
		return sym;
	}

	private void addChildren(
			SeqSymmetry insym, SeqSymmetry sym, SeqSpan pspan, ITrackStyleExtended the_style, BioSeq annotseq,
			GlyphI pglyph, BioSeq coordseq)
			throws InstantiationException, IllegalAccessException {
		SeqSpan cdsSpan = null;
		SeqSymmetry cds_sym = null;
		boolean same_seq = annotseq == coordseq;
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
		DIRECTION_TYPE direction_type = DIRECTION_TYPE.valueFor(the_style.getDirectionType());
		double thin_height = the_style.getHeight() * 0.6;
//		Color start_color = the_style.getStartColor();
//		Color end_color = the_style.getEndColor();
		for (int i = 0; i < childCount; i++) {
			SeqSymmetry child = sym.getChild(i);
			SeqSpan cspan = gviewer.getViewSeqSpan(child);
			if (cspan == null) {
				// if no span for view, then child is either to left or right of view
				outside_children.add(child); // collecting children outside of view to handle later
			} else {
				GlyphI cglyph = getChild(cspan, cspan.getMin() == pspan.getMin(), cspan.getMax() == pspan.getMax(), direction_type);
				Color child_color = getSymColor(child, the_style, cspan.isForward(), direction_type);
				double cheight = handleCDSSpan(cdsSpan, cspan, cds_sym, child, annotseq, same_seq, child_color, pglyph, the_style.getHeight(), thin_height);
				cglyph.setCoords(cspan.getMin(), 0, cspan.getLength(), cheight);
				cglyph.setColor(child_color);
				pglyph.addChild(cglyph);
				gviewer.setDataModelFromOriginalSym(cglyph, child);
				
//				if(direction_type == DIRECTION_TYPE.COLOR || direction_type == DIRECTION_TYPE.BOTH){
//					addCdsColorDirection(cdsSpan, cspan, pglyph, start_color, end_color);
//				}
				
				GlyphI alignResidueGlyph = handleAlignedResidues(child, annotseq);
				if(alignResidueGlyph != null){
					alignResidueGlyph.setCoords(cspan.getMin(), 0, cspan.getLength(), cheight);
					gviewer.setDataModelFromOriginalSym(alignResidueGlyph, child);
					pglyph.addChild(alignResidueGlyph);
				}
				
				if(cglyph instanceof DirectedGlyph){
					((DirectedGlyph)cglyph).setForward(cspan.isForward());
				}
				GlyphProcessorUtil.getInstance().fireProcessGlyph(cglyph);
			}
		}
				
		
//		ArrowHeadGlyph.addDirectionGlyphs(gviewer, sym, pglyph, coordseq, coordseq, 0.0, 
//			thin_height, the_style.getDirectionType() == DIRECTION_TYPE.ARROW.ordinal());
		
		// call out to handle rendering to indicate if any of the children of the
		//    orginal annotation are completely outside the view
		DeletionGlyph.handleEdgeRendering(outside_children, pglyph, annotseq, coordseq, 0.0, thin_height);
	}

	private GlyphI getChild(SeqSpan cspan, boolean isFirst, boolean isLast, DIRECTION_TYPE direction_type) 
			throws InstantiationException, IllegalAccessException{
		
		if (cspan.getLength() == 0) 
			return new DeletionGlyph();
		else if(((isLast && cspan.isForward()) || (isFirst && !cspan.isForward())) && 
				(direction_type == DIRECTION_TYPE.ARROW || direction_type == DIRECTION_TYPE.BOTH))
			return new PointedGlyph();
			
		return (GlyphI) child_glyph_class.newInstance();
	}
		
	private static Color getSymColor(SeqSymmetry insym, ITrackStyleExtended style, boolean isForward, DIRECTION_TYPE direction_type) {
		if(direction_type == DIRECTION_TYPE.COLOR || direction_type == DIRECTION_TYPE.BOTH){
			if(isForward)
				return style.getForwardColor();
			return style.getReverseColor();
		}
		
		boolean use_score_colors = style.getColorByScore();
		boolean use_item_rgb = "on".equalsIgnoreCase((String) style.getTransientPropertyMap().get(TrackLineParser.ITEM_RGB));

		if (!(use_score_colors || use_item_rgb)) {
			return style.getForeground();
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

		return style.getForeground();
	}

	private double handleCDSSpan(
			SeqSpan cdsSpan, SeqSpan cspan, SeqSymmetry cds_sym,
			SeqSymmetry child, BioSeq annotseq, boolean same_seq,
			Color child_color, GlyphI pglyph, double thick_height, double thin_height)
			throws IllegalAccessException, InstantiationException {
		if (cdsSpan == null || SeqUtils.contains(cdsSpan, cspan)) {
			return thick_height;
		}
		if (SeqUtils.overlap(cdsSpan, cspan)) {
			SeqSymmetry cds_sym_2 = SeqUtils.intersection(cds_sym, child, annotseq);
			if (!same_seq) {
				cds_sym_2 = gviewer.transformForViewSeq(cds_sym_2, annotseq);
			}
			SeqSpan cds_span = gviewer.getViewSeqSpan(cds_sym_2);
			if (cds_span != null) {
				GlyphI cds_glyph;
				if (cspan.getLength() == 0) {
					cds_glyph = new DeletionGlyph();
				} else {
					cds_glyph = (GlyphI) child_glyph_class.newInstance();
				}
				cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), thick_height);
				cds_glyph.setColor(child_color); // CDS same color as exon
				pglyph.addChild(cds_glyph);
				gviewer.setDataModelFromOriginalSym(cds_glyph, cds_sym_2);
				GlyphProcessorUtil.getInstance().fireProcessGlyph(cds_glyph);
			}
		}
		return thin_height;
	}

	/**
	 * Determine and set the appropriate residues for this element.
	 * @param sym
	 * @param annotseq
	 * @return GlyphI
	 */
	private GlyphI handleAlignedResidues(SeqSymmetry insym, BioSeq annotseq) {
		SeqSymmetry sym = insym;
		if (insym instanceof DerivedSeqSymmetry) {
			sym = getMostOriginalSymmetry(insym);
		}

		if (!(sym instanceof SymWithResidues)) {
			return null;
		}
		
		SeqSpan span = sym.getSpan(annotseq);
		if (span == null) {
			return null;
		}

		String residueStr = ((SymWithResidues) sym).getResidues();

		if (residueStr == null || residueStr.length() == 0) {
			return null;
		}
		
		AlignedResidueGlyph csg = new AlignedResidueGlyph();
		csg.setResidues(residueStr);
		String bioSeqResidue = annotseq.getResidues(span.getMin(), span.getMin() + residueStr.length());
		if (bioSeqResidue != null) {
			csg.setResidueMask(bioSeqResidue);
		}
		csg.setHitable(false);
		
		return csg;
		
		// SEQ array has unexpected behavior;  commenting out for now.
			/*if (((SymWithProps) sym).getProperty("SEQ") != null) {
		byte[] seqArr = (byte[]) ((SymWithProps) sym).getProperty("SEQ");
		for (int i = 0; i < seqArr.length; i++) {
		System.out.print((char) seqArr[i]);
		}
		System.out.println();
		isg.setResidueMask(seqArr);
		}*/
		
	}
	
	private void handleInsertionGlyphs(SeqSymmetry sym, BioSeq annotseq, GlyphI pglyph, double height)
			throws IllegalAccessException, InstantiationException {
		
		if (!(sym instanceof BAMSym)) {
			return;
		}

		BAMSym inssym = (BAMSym)sym;
		if(inssym.getInsChildCount() == 0)
			return;

		BioSeq coordseq = gviewer.getViewSeq();
		SeqSymmetry psym = inssym;
		if (annotseq != coordseq) {
			psym = gviewer.transformForViewSeq(inssym, annotseq);
		}
		SeqSpan pspan = gviewer.getViewSeqSpan(psym);
		
		Color color = Color.RED;

		for (int i = 0; i < inssym.getInsChildCount(); i++) {

			SeqSymmetry childsym = inssym.getInsChild(i);
			SeqSymmetry dsym = childsym;
			
			if (annotseq != coordseq) {
				dsym = gviewer.transformForViewSeq(childsym, annotseq);
			}
			SeqSpan dspan = gviewer.getViewSeqSpan(dsym);
			SeqSpan ispan = childsym.getSpan(annotseq);

			if(ispan == null || dspan == null){
				continue;
			}

			InsertionSeqGlyph isg = new InsertionSeqGlyph();
			isg.setSelectable(true);
			String residues = inssym.getResidues(ispan.getMin() - 1, ispan.getMin() + 1); 
			isg.setResidues(residues);
			isg.setCoords(Math.max(pspan.getMin(), dspan.getMin() - 1), 0, residues.length(), height);
			isg.setColor(color);

			pglyph.addChild(isg);
			gviewer.setDataModelFromOriginalSym(isg, childsym);
		}
	}

	//Note : Use this code to add cds start and end glyph - HV 07/09/11
	@SuppressWarnings("unused")
	private static void addCdsColorDirection(SeqSpan cdsSpan, SeqSpan cspan, GlyphI pglyph, Color start_color, Color end_color) {
		if (cdsSpan == null || SeqUtils.contains(cdsSpan, cspan)
				|| !SeqUtils.overlap(cdsSpan, cspan) || cdsSpan.getLength() == 0) {
			return;
		}

		if (cdsSpan.isForward()) {
			if (SeqUtils.contains(cspan, cdsSpan)) {
				addColorDirection(pglyph, cdsSpan.getMin(), Math.min(cdsSpan.getLength(), 3), start_color);
				addColorDirection(pglyph, Math.max(cdsSpan.getMin(), cdsSpan.getMax() - 3), Math.min(cdsSpan.getLength(), 3), end_color);
			} else {
				//First
				if (cdsSpan.getEnd() >= cspan.getEnd()) {
					addColorDirection(pglyph, cdsSpan.getStart(), Math.min(cdsSpan.getLength(), 3), start_color);
				} else {
					addColorDirection(pglyph, Math.max(cdsSpan.getStart(), cdsSpan.getEnd() - 3), Math.min(cdsSpan.getLength(), 3), end_color);
				}
			}
		} else {
			if (SeqUtils.contains(cspan, cdsSpan)) {
				addColorDirection(pglyph, cdsSpan.getMin(), Math.min(cdsSpan.getLength(), 3), end_color);
				addColorDirection(pglyph, Math.max(cdsSpan.getMin(), cdsSpan.getMax() - 3), Math.min(cdsSpan.getLength(), 3), start_color);
			} else {
				//First
				if (cdsSpan.getStart() >= cspan.getStart()) {
					addColorDirection(pglyph, cdsSpan.getMin(), Math.min(cdsSpan.getLength(), 3), end_color);
				} else {
					addColorDirection(pglyph, Math.max(cdsSpan.getMin(), cdsSpan.getMax() - 3), Math.min(cdsSpan.getLength(), 3), start_color);
				}
			}
		}
		
	}
	
	//TODO : Use height from style
	private static void addColorDirection(GlyphI pglyph, double start, double length, Color color){
		FillRectGlyph gl = new FillRectGlyph();
		gl.setHitable(false);
		gl.setColor(color);
		gl.setCoords(start, 0, length, 25);
		pglyph.addChild(gl);
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory checkCategory) {
		return (checkCategory == category);
	}

	protected ViewModeGlyph createViewModeGlyph(ITrackStyleExtended style, Direction direction) {
		ViewModeGlyph viewModeGlyph = new AnnotationGlyph(style);
		viewModeGlyph.setDirection(direction);
		return viewModeGlyph;
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, Direction tier_direction) {
		String meth = BioSeq.determineMethod(sym);

		if (meth == null) {
			return createViewModeGlyph(style, tier_direction);
		}
		else {
			int glyph_depth = style.getGlyphDepth();

			Direction useDirection = (tier_direction == Direction.BOTH) ? Direction.BOTH : Direction.FORWARD;
			ViewModeGlyph ftier = createViewModeGlyph(style, useDirection);
			ftier.setInfo(sym);
			ViewModeGlyph rtier = (tier_direction == Direction.BOTH) ? ftier : createViewModeGlyph(style, Direction.REVERSE);
			rtier.setInfo(sym);
			if (style.getSeparate()) {
				addLeafsToTier(sym, ftier, rtier, glyph_depth);
			} else {
				// use only one tier
				addLeafsToTier(sym, ftier, ftier, glyph_depth);
			}
			return (tier_direction == Direction.REVERSE) ? rtier : ftier;
		}
//		else {  // keep recursing down into child syms if parent sym has no "method" property
//			int childCount = sym.getChildCount();
//			for (int i = 0; i < childCount; i++) {
//				SeqSymmetry childSym = sym.getChild(i);
//				createGlyph(childSym, gviewer);
//			}
//		}
	}

	@Override
	public String getName() {
		return category.name().toLowerCase();
	}

	// for GenericGraphGlyphFactory, can be removed when that is removed
	public static ViewModeGlyph getViewModeGlyph(ITrackStyleExtended style) {
		return new AnnotationGlyph(style);
	}

	@Override
	public boolean isURISupported(String uri) {
		return true;
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return false;
	}
}
