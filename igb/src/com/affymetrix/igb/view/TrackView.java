package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.ScoredContainerSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.glyph.CytobandGlyph;
import com.affymetrix.igb.glyph.EmptyTierGlyphFactory;
import com.affymetrix.igb.glyph.GenericGraphGlyphFactory;
import com.affymetrix.igb.glyph.MapViewModeHolder;
import com.affymetrix.igb.glyph.ScoredContainerGlyphFactory;
import com.affymetrix.igb.shared.CollapsePacker;
import com.affymetrix.igb.shared.ExpandPacker;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.stylesheet.XmlStylesheetGlyphFactory;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
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

	/** Hash of ITrackStyle to forward TierGlyph */
	private static final Map<ITrackStyleExtended, TierGlyph> style2forwardTierGlyph = new HashMap<ITrackStyleExtended, TierGlyph>();
	/** Hash of ITrackStyle to reverse TierGlyph */
	private static final Map<ITrackStyleExtended, TierGlyph> style2reverseTierGlyph = new HashMap<ITrackStyleExtended, TierGlyph>();
	/** Hash of ITrackStyle to TierGlyph. */
	private static final Map<ITrackStyleExtended, TierGlyph> gstyle2track = new HashMap<ITrackStyleExtended, TierGlyph>();


	/**List of Dependent data */
	private static final Map<BioSeq, List<DependentData>> dependent_list = new HashMap<BioSeq, List<DependentData>>();

	static void clear() {
		style2forwardTierGlyph.clear();
		style2reverseTierGlyph.clear();
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
			SeqMapView smv, ITrackStyleExtended style, boolean constant_heights) {
		AffyTieredMap map = smv.getSeqMap();
		
		if (style.isGraphTier()) {
			constant_heights = false;
		}

		TierGlyph fortier = style2forwardTierGlyph.get(style);
		if (fortier == null) {
			fortier = new TierGlyph(style);
			setUpTrackPacker(fortier, true, constant_heights);
			style2forwardTierGlyph.put(style, fortier);
		}

		if (style.getSeparate()) {
			fortier.setDirection(TierGlyph.Direction.FORWARD);
		} else {
			fortier.setDirection(TierGlyph.Direction.BOTH);
		}
		fortier.setLabel(style.getTrackName());

		if (map.getTierIndex(fortier) == -1) {
			map.addTier(fortier, true);
		}

		TierGlyph revtier = style2reverseTierGlyph.get(style);
		if (revtier == null) {
			revtier = new TierGlyph(style);
			revtier.setDirection(TierGlyph.Direction.REVERSE);
			setUpTrackPacker(revtier, false, constant_heights);
			style2reverseTierGlyph.put(style, revtier);
		}
		revtier.setLabel(style.getTrackName());

		if (map.getTierIndex(revtier) == -1) {
			map.addTier(revtier, false);
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
	public static TierGlyph getGraphTrack(AffyTieredMap seqmap, ITrackStyleExtended style, TierGlyph.Direction tier_direction) {
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
		tier.setLabel(style.getTrackName());
		tier.setFillColor(style.getBackground());
		tier.setForegroundColor(style.getForeground());

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
					//Create dummy tier
					String meth = BioSeq.determineMethod(annotSym);
					ITrackStyleExtended  style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
					smv.getTiers(style, true);
					continue;
				}
			}
			if (annotSym instanceof SymWithProps) {
				addAnnotationGlyphs(smv, (SymWithProps)annotSym);
				// TODO: reimplement middleground shading in a generic fashion
				doMiddlegroundShading((SymWithProps)annotSym, seq);
			}
		}
		
	}

	static void addDependentAndEmptyTrack(SeqMapView smv, BioSeq seq) {
		List<DependentData> dd_list = dependent_list.get(seq);
		if (dd_list != null) {
			for (DependentData d : dd_list) {
				SymWithProps sym = d.getSym();
				if (sym != null) {
					addAnnotationGlyphs(smv, sym);
					// TODO: reimplement middleground shading in a generic fashion
					doMiddlegroundShading(sym, seq);
				}
			}
		}
		
		for(GenericFeature feature : GeneralLoadUtils.getVisibleFeatures()){
			EmptyTierGlyphFactory.addEmtpyTierfor(feature, smv);
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
			factory = determineFactory(annotSym);
		}

		factory.createGlyph(annotSym, smv);
	}

	private static MapViewGlyphFactoryI determineFactory(SymWithProps sym){
				String meth = BioSeq.determineMethod(sym);

		if (meth != null) {
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
			
			// Use alternate view mode if available
			MapViewGlyphFactoryI view_mode = MapViewModeHolder.getInstance().getViewFactory(style.getViewMode());
			if(view_mode != null){
				return view_mode;
			}
		}
		
		return getAnnotationGlyphFactory();
	}
	
	private static void doMiddlegroundShading(SymWithProps annotSym, BioSeq seq) {
		String meth = BioSeq.determineMethod(annotSym);
		ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
		GenericFeature feature = style.getFeature();
		if ((meth != null)
				&& (annotSym instanceof TypeContainerAnnot)
				&& (annotSym.getChildCount() > 0)
				&& (feature != null)) {
			TierGlyph forwardTrack = style2forwardTierGlyph.get(style);
			TierGlyph reverseTrack = style2reverseTierGlyph.get(style);
			SeqSymmetry inverse = SeqUtils.inverse(feature.getRequestSym(), seq);
			int child_count = inverse.getChildCount();
			for (int i = 0; i < child_count; i++) {
				SeqSymmetry child = inverse.getChild(i);
				for (int j = 0; j < child.getSpanCount(); j++) {
					SeqSpan ospan = child.getSpan(j);
					if (ospan.getLength() > 1) {
						if (forwardTrack != null) {
							GlyphI mglyph = new FillRectGlyph();
							mglyph.setCoords(ospan.getMin(), 0, ospan.getLength() - 1, 0);
							forwardTrack.addMiddleGlyph(mglyph);
						}
						if (reverseTrack != null) {
							GlyphI mglyph = new FillRectGlyph();
							mglyph.setCoords(ospan.getMin(), 0, ospan.getLength() - 1, 0);
							reverseTrack.addMiddleGlyph(mglyph);
						}
					}
				}
			}
		}

		// Middle ground shading for graphsym
		if ((meth != null)
				&& (annotSym instanceof GraphSym)
				&& (feature != null)) {
			TierGlyph track = gstyle2track.get(style);
			SeqSymmetry inverse = SeqUtils.inverse(feature.getRequestSym(), seq);
			int child_count = inverse.getChildCount();
			for (int i = 0; i < child_count; i++) {
				SeqSymmetry child = inverse.getChild(i);
				for (int j = 0; j < child.getSpanCount(); j++) {
					SeqSpan ospan = child.getSpan(j);
					if (ospan.getLength() > 1) {
						if (track != null) {
							GlyphI mglyph = new FillRectGlyph();
							mglyph.setCoords(ospan.getMin(), 0, ospan.getLength() - 1, 0);
							track.addMiddleGlyph(mglyph);
						}
					}
				}
			}
		}
	}

	public static SymWithProps addToDependentList(DependentData dd){
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		if(seq == null)
			return null;

		List<DependentData> dd_list = dependent_list.get(seq);
		if(dd_list == null){
			dd_list = new ArrayList<DependentData>();
			dependent_list.put(seq, dd_list);
		}
		
		dd_list.add(dd);
		return dd.createTier(seq);
	}

	public static void updateDependentData() {
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		if (seq == null)
			return;
		
		List<DependentData> dd_list = dependent_list.get(seq);
		if(dd_list == null)
			return;
		
		for (DependentData dd : dd_list)
			dd.createTier(seq);
	}

	public static void delete(AffyTieredMap map, String method, ITrackStyleExtended style){
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		GenericFeature feature = style.getFeature();
		
		// If genome is selected then delete all syms on the all seqs.
		if(IGBConstants.GENOME_SEQ_ID.equals(seq.getID())){
			GeneralLoadView.getLoadView().removeFeature(feature, true);
			return;
		}

		deleteDependentData(map, method, seq);
		deleteSymsOnSeq(map, method, seq, feature);
	}
	
	public static void deleteSymsOnSeq(AffyTieredMap map, String method, BioSeq seq, GenericFeature feature){
		
		if (seq != null) {
			SeqSymmetry sym = seq.getAnnotation(method);
			if (sym != null) {
				if(sym instanceof GraphSym){
					GlyphI glyph = map.getItem(sym);
					if(glyph != null){
						map.removeItem(glyph);
					}
				}
				seq.unloadAnnotation(sym);
				
				if(feature != null){
					feature.clear(seq);
					if(feature.getLoadStrategy() == LoadStrategy.GENOME || feature.getLoadStrategy() == LoadStrategy.AUTOLOAD){
						feature.setLoadStrategy(LoadStrategy.NO_LOAD);
					}
				}
			}
		}
	}
	
	public static void deleteDependentData(AffyTieredMap map, String method, BioSeq seq) {
		DependentData dd = null;
		List<DependentData> dd_list = dependent_list.get(seq);
		if(dd_list == null)
			return;
		
		List<DependentData> remove_list = new ArrayList<DependentData>();
		for (int i = 0; i < dd_list.size(); i++) {
			dd = dd_list.get(i);
			if ((method == null ? dd.getParentMethod() == null : method.equals(dd.getParentMethod()))
					|| method.equals(dd.getID())) {
				remove_list.add(dd);
//				GlyphI glyph = map.getItem(dd.getSym());
//				if(glyph != null){
//					map.removeItem(glyph);
//				}
//				seq.unloadAnnotation(dd.getSym());
			}
		}
		
		for(DependentData r : remove_list){
			dd_list.remove(r);
		}
		
		remove_list.clear();
	}

}
