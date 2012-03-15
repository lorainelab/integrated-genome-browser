package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.shared.CollapsePacker;
import com.affymetrix.igb.shared.ExpandPacker;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.stylesheet.XmlStylesheetGlyphFactory;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.viewmode.DummyGlyphFactory;
import com.affymetrix.igb.viewmode.MapViewModeHolder;
import com.affymetrix.igb.viewmode.TierGlyphViewMode;
import com.affymetrix.igb.viewmode.TransformHolder;
import com.affymetrix.igb.viewmode.UnloadedGlyphFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jnicol
 */
public class TrackView {
	private static final TrackView instance = new TrackView();
	private TrackView() {
		super();
	}
	public static TrackView getInstance() {
		return instance;
	}
	private static final XmlStylesheetGlyphFactory default_glyph_factory = new XmlStylesheetGlyphFactory();

	/** Hash of ITrackStyle to forward TierGlyph */
	private static final Map<ITrackStyleExtended, TierGlyph> style2forwardTierGlyph = new HashMap<ITrackStyleExtended, TierGlyph>();
	/** Hash of ITrackStyle to reverse TierGlyph */
	private static final Map<ITrackStyleExtended, TierGlyph> style2reverseTierGlyph = new HashMap<ITrackStyleExtended, TierGlyph>();
	/** Hash of ITrackStyle to TierGlyph. */
	private static final Map<ITrackStyleExtended, TierGlyph> gstyle2track = new HashMap<ITrackStyleExtended, TierGlyph>();


	/**List of Dependent data */
	private static final Map<BioSeq, List<DependentData>> dependent_list = new HashMap<BioSeq, List<DependentData>>();

	void clear() {
		style2forwardTierGlyph.clear();
		style2reverseTierGlyph.clear();
		gstyle2track.clear();
		dependent_list.clear();
	}

	// XmlStylesheetGlyphFactory takes the method and type
	// into account when determining how to draw a sym.
	public XmlStylesheetGlyphFactory getAnnotationGlyphFactory() {
		return default_glyph_factory;
	}

	/**
	 * get an new TierGlyphViewMode, unless there is already a TierGlyph for the style/direction
	 * @param smv the SeqMapView
	 * @param style the style
	 * @param tier_direction the direction
	 * @return the existing TierGlyph, or a new TierGlyphViewMode, for the style/direction
	 */
	TierGlyph getTrack(SeqMapView smv, SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction, boolean dummy) {
		AffyTieredMap seqmap = smv.getSeqMap();
		TierGlyph tierGlyph = null;
		Map<ITrackStyleExtended, TierGlyph> style2track = null;
		if (style.isGraphTier()) {
			style2track = gstyle2track;
		}
		else if (tier_direction == TierGlyph.Direction.REVERSE) {
			style2track = style2reverseTierGlyph;
		}
		else if (tier_direction == TierGlyph.Direction.BOTH || tier_direction == TierGlyph.Direction.FORWARD) {
			style2track = style2forwardTierGlyph;
		}
		tierGlyph = style2track.get(style);
		if (tierGlyph != null && !(tierGlyph instanceof TierGlyphViewMode)) {
			seqmap.removeTier(tierGlyph);
			tierGlyph = null;
		}
		if (tierGlyph == null) {
			MapViewGlyphFactoryI factory = dummy ? DummyGlyphFactory.getInstance() : UnloadedGlyphFactory.getInstance();
			tierGlyph = new TierGlyphViewMode(sym, style, tier_direction, smv, factory.getViewModeGlyph(sym, style, tier_direction, smv));
			tierGlyph.setLabel(style.getTrackName());
			// do not set packer here, will be set in ViewModeGlyph
			if (style.isGraphTier()) {
				gstyle2track.put(style, tierGlyph);
			}
			else if (tier_direction == TierGlyph.Direction.REVERSE) {
				style2reverseTierGlyph.put(style, tierGlyph);
			}
			else if (tier_direction == TierGlyph.Direction.BOTH || tier_direction == TierGlyph.Direction.FORWARD) {
				style2forwardTierGlyph.put(style, tierGlyph);
			}
			if (seqmap.getTierIndex(tierGlyph) == -1) {
				boolean above_axis = (tier_direction != TierGlyph.Direction.REVERSE);
				seqmap.addTier(tierGlyph, above_axis);
			}
		}
		
		if (!style.isGraphTier() && (tier_direction == TierGlyph.Direction.BOTH || tier_direction == TierGlyph.Direction.FORWARD)) {
			if(style.getSeparate()){
				tierGlyph.setDirection(TierGlyph.Direction.FORWARD);
			}else{
				tierGlyph.setDirection(TierGlyph.Direction.BOTH);
			}
		}
		
		return tierGlyph;
	}

	/**
	 *  This UcscVersion of getTiers() allows you to specify whether the tier will hold
	 *  glyphs that are all of the same height.  If so, a more efficient packer can
	 *  be used.  Note: if style.isGraphTier() is true, then the given value of
	 *  constant_height will be ignored and re-set to false.
	 */
	TierGlyph[] getTiers(
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
	TierGlyph getGraphTrack(AffyTieredMap seqmap, ITrackStyleExtended style, TierGlyph.Direction tier_direction) {
		TierGlyph tier = gstyle2track.get(style);
		if (tier == null) {
			tier = new TierGlyph(style);
			tier.setDirection(tier_direction);
			setUpTrackPacker(tier, true, false);
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

	private void setUpTrackPacker(TierGlyph tg, boolean above_axis, boolean constantHeights) {
		FasterExpandPacker ep = new FasterExpandPacker();
		ep.setConstantHeights(constantHeights);
		if (above_axis) {
			ep.setMoveType(ExpandPacker.UP);
		} else {
			ep.setMoveType(ExpandPacker.DOWN);
		}
		tg.setExpandedPacker(ep);
	}

	void addTracks(SeqMapView smv, BioSeq seq) {
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
					ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
					if (style.getSeparate()) {
						smv.getTrack(null, style, TierGlyph.Direction.FORWARD, true);
						smv.getTrack(null, style, TierGlyph.Direction.REVERSE, true);
					}
					else {
						smv.getTrack(null, style, TierGlyph.Direction.BOTH, true);
					}
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

	void addDependentAndEmptyTrack(SeqMapView smv, BioSeq seq) {
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
			EmptyTierGlyphFactory.addEmtpyTierfor(feature, smv, false);
		}
	}

	public void changeViewMode(SeqMapView gviewer, RootSeqSymmetry rootSym, ITrackStyleExtended style, String viewMode) {
		String oldViewMode = style.getViewMode();
		if (oldViewMode.equals(viewMode)) {
			return;
		}
		style.setViewMode(viewMode);
		gviewer.getTrack(rootSym, style, style.getSeparate() ? TierGlyph.Direction.FORWARD : TierGlyph.Direction.BOTH);
		if (style.getSeparate()) {
			gviewer.getTrack(rootSym, style, style.getSeparate() ? TierGlyph.Direction.REVERSE : TierGlyph.Direction.BOTH);
		}
		gviewer.addAnnotationTrackFor(style);
		// kludge to get GraphAdjuster tab to update Style box (graph type)
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		syms.add(rootSym);
		GenometryModel.getGenometryModel().setSelectedSymmetries(syms, gviewer);
	}

	public void addAnnotationGlyphs(SeqMapView smv, ITrackStyleExtended style){ 
		String meth = style.getMethodName();
		SymWithProps annotSym = smv.getAnnotatedSeq().getAnnotation(meth);
		
		//Remove previous view mode glyph
		TierGlyphViewMode mainTier = (TierGlyphViewMode)smv.getTrack(annotSym, style, style.getSeparate() ? TierGlyph.Direction.FORWARD : TierGlyph.Direction.BOTH);
		smv.getSeqMap().removeItem(mainTier.getViewModeGlyph());
		
		if (style.getSeparate()) {
			TierGlyphViewMode secondTier = (TierGlyphViewMode)smv.getTrack(annotSym, style, style.getSeparate() ? TierGlyph.Direction.REVERSE : TierGlyph.Direction.BOTH);
			smv.getSeqMap().removeItem(secondTier.getViewModeGlyph());
		}
		
		addAnnotationGlyphs(smv, annotSym);
	}
	
	private void addAnnotationGlyphs(SeqMapView smv, SymWithProps annotSym) {
		// Map symmetry subclass or method type to a factory, and call factory to make glyphs
		MapViewGlyphFactoryI factory = determineFactory(annotSym);
		if (!factory.getClass().getName().startsWith("com.affymetrix.igb.glyph") &&
			!factory.getClass().getName().startsWith("com.affymetrix.igb.stylesheet")
			) {
			String meth = BioSeq.determineMethod(annotSym);

			if (meth != null) {
				ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
				Operator operator = determineOperator(annotSym);
				if(operator != null){
					if(!operator.supportsTwoTrack()){
						style.setSeparate(false);
					}
				}
				TierGlyphViewMode mainTier = (TierGlyphViewMode)smv.getTrack(annotSym, style, style.getSeparate() ? TierGlyph.Direction.FORWARD : TierGlyph.Direction.BOTH);
				mainTier.setInfo(annotSym);
				if (style.getSeparate()) {
					TierGlyphViewMode secondTier = (TierGlyphViewMode)smv.getTrack(annotSym, style, style.getSeparate() ? TierGlyph.Direction.REVERSE : TierGlyph.Direction.BOTH);
					secondTier.setInfo(annotSym);
				}
				return;
			}
		}

		factory.createGlyph(annotSym, smv);
	}

	private MapViewGlyphFactoryI determineFactory(SymWithProps sym){
		String meth = BioSeq.determineMethod(sym);
		
		if (meth != null) {
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
			if ("default".equals(style.getViewMode())) {
				style.setViewMode(MapViewModeHolder.getInstance().getDefaultFactoryFor(((RootSeqSymmetry)sym).getCategory()).getName());
			}
			
			// Use alternate view mode if available
			MapViewGlyphFactoryI view_mode = MapViewModeHolder.getInstance().getViewFactory(style.getViewMode());
			if(view_mode != null){
				return view_mode;
			}
		}
		
		return getAnnotationGlyphFactory();
	}
	
	private Operator determineOperator(SymWithProps sym){
		String meth = BioSeq.determineMethod(sym);

		if (meth != null) {
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
			return TransformHolder.getInstance().getOperator(style.getOperator());
		}
		
		return null;
	}
		
	private void doMiddlegroundShading(SymWithProps annotSym, BioSeq seq) {
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

	public SymWithProps addToDependentList(DependentData dd){
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

	public void updateDependentData() {
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		if (seq == null)
			return;
		
		List<DependentData> dd_list = dependent_list.get(seq);
		if(dd_list == null)
			return;
		
		for (DependentData dd : dd_list)
			dd.createTier(seq);
	}

	public void delete(AffyTieredMap map, String method, ITrackStyleExtended style){
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
	
	public void deleteSymsOnSeq(AffyTieredMap map, String method, BioSeq seq, GenericFeature feature){
		
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
	
	public void deleteDependentData(AffyTieredMap map, String method, BioSeq seq) {
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
