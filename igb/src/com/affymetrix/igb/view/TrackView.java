package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.das2.Das2FeatureRequestSym;
import com.affymetrix.genometryImpl.style.IAnnotStyle;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.glyph.CytobandGlyph;
import com.affymetrix.igb.glyph.GenericGraphGlyphFactory;
import com.affymetrix.igb.glyph.MapViewGlyphFactoryI;
import com.affymetrix.igb.glyph.ScoredContainerGlyphFactory;
import com.affymetrix.igb.stylesheet.XmlStylesheetGlyphFactory;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.CollapsePacker;
import com.affymetrix.igb.tiers.ExpandPacker;
import com.affymetrix.igb.tiers.FasterExpandPacker;
import com.affymetrix.igb.tiers.TierGlyph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jnicol
 */
public class TrackView {
	private static final XmlStylesheetGlyphFactory default_glyph_factory = new XmlStylesheetGlyphFactory();

	// We only need a single ScoredContainerGlyphFactory because all graph properties
	// are in the GraphState object.
	private static final ScoredContainerGlyphFactory container_factory = new ScoredContainerGlyphFactory();

	// We only need a single GraphGlyphFactory because all graph properties
	// are in the GraphState object.
	private static final GenericGraphGlyphFactory graph_factory = new GenericGraphGlyphFactory();

	/** Hash of method names (lower case) to forward tiers */
	private static final Map<String, TierGlyph> method2forwardTrack = new HashMap<String, TierGlyph>();
	/** Hash of method names (lower case) to reverse tiers */
	private static final Map<String, TierGlyph> method2reverseTrack = new HashMap<String, TierGlyph>();
	/** Hash of GraphStates to TierGlyphs. */
	private static final Map<IAnnotStyle, TierGlyph> gstyle2track = new HashMap<IAnnotStyle, TierGlyph>();


	/**List of Dependent data */
	private static final List<DependentData> dependent_list = new ArrayList<DependentData>();

	static void clear() {
		method2reverseTrack.clear();
		method2forwardTrack.clear();
		gstyle2track.clear();
		dependent_list.clear();
	}

	// XmlStylesheetGlyphFactory takes the method and type
	// into account when determining how to draw a sym.
	public static XmlStylesheetGlyphFactory getAnnotationGlyphFactory() {
		return default_glyph_factory;
	}

	/**
	 *  This UcscVersion of getTiers() allows you to specify whether the tier will hold
	 *  glyphs that are all of the same height.  If so, a more efficient packer can
	 *  be used.  Note: if style.isGraphTier() is true, then the given value of
	 *  constant_height will be ignored and re-set to false.
	 */
	public static TierGlyph[] getTiers(
			SeqMapView smv, String meth, boolean next_to_axis, IAnnotStyleExtended style, boolean constant_heights) {
		if (style == null) {
			throw new NullPointerException();
		}
		AffyTieredMap map = smv.getSeqMap();

		if (style.isGraphTier()) {
			constant_heights = false;
		}

		TierGlyph fortier = method2forwardTrack.get(meth.toLowerCase());
		if (fortier == null) {
			fortier = new TierGlyph(style);
			fortier.setParentURL(meth);
			setUpTrackPacker(fortier, true, constant_heights);
			method2forwardTrack.put(meth.toLowerCase(), fortier);
		}

		if (style.getSeparate()) {
			fortier.setDirection(TierGlyph.Direction.FORWARD);
		} else {
			fortier.setDirection(TierGlyph.Direction.BOTH);
		}
		fortier.setLabel(style.getHumanName());

		if (map.getTierIndex(fortier) == -1) {
			if (next_to_axis) {
				int axis_index = map.getTierIndex(smv.getAxisTier());
				map.addTier(fortier, axis_index);
			} else {
				map.addTier(fortier, true);
			}
		}

		TierGlyph revtier = method2reverseTrack.get(meth.toLowerCase());
		if (revtier == null) {
			revtier = new TierGlyph(style);
			revtier.setParentURL(meth);
			revtier.setDirection(TierGlyph.Direction.REVERSE);
			setUpTrackPacker(revtier, false, constant_heights);
			method2reverseTrack.put(meth.toLowerCase(), revtier);
		}
		revtier.setLabel(style.getHumanName());

		if (map.getTierIndex(revtier) == -1) {
			if (next_to_axis) {
				int axis_index = map.getTierIndex(smv.getAxisTier());
				map.addTier(revtier, axis_index + 1);
			} else {
				map.addTier(revtier, false);
			}
		}

		if (style.getSeparate()) {
			return new TierGlyph[]{fortier, revtier};
		} else {
			// put everything in a single tier
			return new TierGlyph[]{fortier, fortier};
		}
	}


	/**
	 *  Returns a track for the given IAnnotStyle, creating the tier if necessary.
	 *  Generally called by a Graph Glyph Factory.
	 */
	public static TierGlyph getGraphTrack(AffyTieredMap seqmap, IAnnotStyle style, TierGlyph.Direction tier_direction) {
		if (style == null) {
			throw new NullPointerException();
		}

		TierGlyph tier = gstyle2track.get(style);
		if (tier == null) {
			tier = new TierGlyph(style);
			tier.setDirection(tier_direction);
			TrackView.setUpTrackPacker(tier, true, false);
			gstyle2track.put(style, tier);
		}

		PackerI pack = tier.getPacker();
		if (pack instanceof CollapsePacker) {
			CollapsePacker cp = (CollapsePacker) pack;
			cp.setParentSpacer(0); // fill tier to the top and bottom edges
			cp.setAlignment(CollapsePacker.ALIGN_CENTER);
		}

		tier.setDirection(tier_direction);
		tier.setLabel(style.getHumanName());
		tier.setFillColor(style.getBackground());
		tier.setForegroundColor(style.getColor());

		if (seqmap.getTierIndex(tier) == -1) {
			boolean above_axis = (tier_direction != TierGlyph.Direction.REVERSE);
			seqmap.addTier(tier, above_axis);
		}
		return tier;
	}

	private static void setUpTrackPacker(TierGlyph tg, boolean above_axis, boolean constantHeights) {
		FasterExpandPacker ep = new FasterExpandPacker();
		ep.setConstantHeights(constantHeights);
		if (above_axis) {
			ep.setMoveType(ExpandPacker.UP);
		} else {
			ep.setMoveType(ExpandPacker.DOWN);
		}
		tg.setExpandedPacker(ep);
		tg.setMaxExpandDepth(tg.getAnnotStyle().getMaxDepth());
	}

	static void addTracks(SeqMapView smv, BioSeq seq) {
		// WARNING: use seq.getAnnotationCount() in loop, because some annotations end up lazily instantiating
		//   other annotations and adding them to the annotation list
		// For example, accessing methods for the first time on a LazyChpSym can cause it to dynamically add
		//      probeset annotation tracks
		for (int i = 0; i < seq.getAnnotationCount(); i++) {
			SeqSymmetry annotSym = seq.getAnnotation(i);
			// skip over any cytoband data.  It is shown in a different way
			if (annotSym instanceof TypeContainerAnnot) {
				TypeContainerAnnot tca = (TypeContainerAnnot) annotSym;
				if (CytobandGlyph.CYTOBAND_TIER_REGEX.matcher(tca.getType()).matches()) {
					continue;
				}
			}
			if (annotSym instanceof SymWithProps) {
				addAnnotationGlyphs(smv, (SymWithProps)annotSym);
				doMiddlegroundShading((SymWithProps)annotSym);
			}
		}
	}

	private static void addAnnotationGlyphs(SeqMapView smv, SymWithProps annotSym) {
		// Map symmetry subclass or method type to a factory, and call factory to make glyphs
		MapViewGlyphFactoryI factory = null;
		if (annotSym instanceof ScoredContainerSym) {
			factory = container_factory;
		} else if (annotSym instanceof GraphSym) {
			factory = graph_factory;
		} else {
			factory = getAnnotationGlyphFactory();
		}

		factory.createGlyph(annotSym, smv);
	}

	private static void doMiddlegroundShading(SymWithProps annotSym) {
		String meth = BioSeq.determineMethod(annotSym);
		// do "middleground" shading for tracks loaded via DAS/2
		if ((meth != null) &&
						(annotSym instanceof TypeContainerAnnot) &&
						(annotSym.getChildCount() > 0) &&
						(annotSym.getChild(0) instanceof Das2FeatureRequestSym)) {
			int child_count = annotSym.getChildCount();
			TierGlyph forwardTrack = method2forwardTrack.get(meth.toLowerCase());
			TierGlyph reverseTrack = method2reverseTrack.get(meth.toLowerCase());
			for (int i = 0; i < child_count; i++) {
				SeqSymmetry csym = annotSym.getChild(i);
				if (csym instanceof Das2FeatureRequestSym) {
					Das2FeatureRequestSym dsym = (Das2FeatureRequestSym) csym;
					SeqSpan ospan = dsym.getOverlapSpan();
					if (forwardTrack != null) {
						GlyphI mglyph = new FillRectGlyph();
						mglyph.setCoords(ospan.getMin(), 0, ospan.getMax() - ospan.getMin(), 0);
						forwardTrack.addMiddleGlyph(mglyph);
					}
					if (reverseTrack != null) {
						GlyphI mglyph = new FillRectGlyph();
						mglyph.setCoords(ospan.getMin(), 0, ospan.getMax() - ospan.getMin(), 0);
						reverseTrack.addMiddleGlyph(mglyph);
					}
				}
			}
		}

	}

	public static SymWithProps addToDependentList(DependentData dd){
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		if(seq == null)
			return null;

		dependent_list.add(dd);
		return dd.createTier(seq);
	}

	public static void updateDependentData() {
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		if (seq != null) {
			for (DependentData dd : dependent_list) {
				seq.removeAnnotation(dd.getSym());
				dd.createTier(seq);
			}
		}
	}

	public static void deleteTrack(TierGlyph tg) {
		String URL = tg.getParentURL();
		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		if (group != null) {
			for (BioSeq seq : group.getSeqList()) {
				SeqSymmetry sym = seq.getAnnotation(URL);
				if (sym != null) {
					seq.removeAnnotation(sym);
				}
			}
		}

		for (DependentData dd : dependent_list) {
			if (URL == null ? dd.getParentUrl() == null : URL.equals(dd.getParentUrl()))
				dependent_list.remove(dd);
		}
	}
}
