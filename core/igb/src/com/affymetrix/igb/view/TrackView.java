package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.viewmode.ComboGlyphFactory.ComboGlyph;
import com.affymetrix.igb.viewmode.DummyGlyphFactory;
import com.affymetrix.igb.shared.MapViewModeHolder;
import com.affymetrix.igb.viewmode.ProbeSetGlyphFactory;
import com.affymetrix.igb.viewmode.TransformHolder;

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

	/** Hash of ITrackStyle to forward TierGlyph */
	private static final Map<ITrackStyleExtended, TierGlyph> style2forwardTierGlyph = new HashMap<ITrackStyleExtended, TierGlyph>();
	/** Hash of ITrackStyle to reverse TierGlyph */
	private static final Map<ITrackStyleExtended, TierGlyph> style2reverseTierGlyph = new HashMap<ITrackStyleExtended, TierGlyph>();
	/** Hash of ITrackStyle to TierGlyph. */
	private static final Map<ITrackStyleExtended, TierGlyph> gstyle2track = new HashMap<ITrackStyleExtended, TierGlyph>();


	void clear() {
		style2forwardTierGlyph.clear();
		style2reverseTierGlyph.clear();
		gstyle2track.clear();
	}

	public TierGlyph getTier(ITrackStyleExtended style, TierGlyph.Direction tier_direction) {
		if (style == null || tier_direction == null) {
			return null;
		}
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
		return style2track.get(style);
	}

	/**
	 * get an new TierGlyphViewMode, unless there is already a TierGlyph for the style/direction
	 * @param smv the SeqMapView
	 * @param style the style
	 * @param tier_direction the direction
	 * @return the existing TierGlyph, or a new TierGlyphViewMode, for the style/direction
	 */
	TierGlyph getTrack(SeqMapView smv, SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction, MapViewGlyphFactoryI factory) {
		AffyTieredMap seqmap = smv.getSeqMap();
		TierGlyph tierGlyph = null;
		tierGlyph = getTier(style, tier_direction);
		if (tierGlyph == null) {
			tierGlyph = new TierGlyph(sym, style, tier_direction, smv, factory.getViewModeGlyph(sym, style, tier_direction, smv));
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

//		smv.processTrack(tierGlyph);
		return tierGlyph;
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
						smv.getTrack(null, style, TierGlyph.Direction.FORWARD, DummyGlyphFactory.getInstance());
						smv.getTrack(null, style, TierGlyph.Direction.REVERSE, DummyGlyphFactory.getInstance());
					}
					else {
						smv.getTrack(null, style, TierGlyph.Direction.BOTH, DummyGlyphFactory.getInstance());
					}
					continue;
				}
			}
			if (annotSym instanceof SymWithProps) {
				addAnnotationGlyphs(smv, (SymWithProps)annotSym);
				// TODO: reimplement middleground shading in a generic fashion
				doMiddlegroundShading(smv, (SymWithProps)annotSym, seq);
			}
		}
		
	}

	void addDependentAndEmptyTrack(SeqMapView smv, BioSeq seq) {
		for(GenericFeature feature : GeneralLoadUtils.getVisibleFeatures()){
			addEmptyTierFor(feature, smv, false);
		}
	}

	public void changeViewMode(SeqMapView gviewer, ITrackStyleExtended style, String viewMode, RootSeqSymmetry rootSym, ITrackStyleExtended comboStyle) {
		String oldViewMode = style.getViewMode();
		if (oldViewMode.equals(viewMode)) {
			return;
		}
		if (comboStyle == null) {
			style.setViewMode(viewMode);
			gviewer.addAnnotationTrackFor(style);
		}
		else {
			// must be a GraphGlyph in a ComboGlyph
			TierGlyph comboTier = gviewer.getTrack(rootSym, comboStyle, TierGlyph.Direction.NONE, null);
			ComboGlyph comboGlyph = (ComboGlyph)comboTier.getViewModeGlyph();
			AbstractGraphGlyph oldGlyph = (AbstractGraphGlyph)comboGlyph.getChildWithStyle(style);
			AbstractGraphGlyph newGlyph = (AbstractGraphGlyph)MapViewModeHolder.getInstance().getViewFactory(viewMode).getViewModeGlyph((SeqSymmetry)oldGlyph.getInfo(), style, TierGlyph.Direction.NONE, gviewer);
			style.setViewMode(viewMode);
			comboGlyph.removeChild(oldGlyph);
			newGlyph.setScene(oldGlyph.getScene());
			newGlyph.setCoordBox(oldGlyph.getCoordBox());
			newGlyph.setTierGlyph(oldGlyph.getTierGlyph());
			newGlyph.setVisibility(oldGlyph.isVisible());
			comboGlyph.addChild(newGlyph);
			//gviewer.getSeqMap().packTiers(true, false, false);
			gviewer.getSeqMap().updateWidget();
		}
		
		if(rootSym != null){
			// kludge to get GraphAdjuster tab to update Style box (graph type)
			List<RootSeqSymmetry> all_syms = new ArrayList<RootSeqSymmetry>();
			List<SeqSymmetry> graph_syms = new ArrayList<SeqSymmetry>();
			graph_syms.add(rootSym);
			GenometryModel.getGenometryModel().setSelectedSymmetries(all_syms, graph_syms, gviewer);
		}
	}

	public void addAnnotationGlyphs(SeqMapView smv, ITrackStyleExtended style){ 
		String meth = style.getMethodName();
		SymWithProps annotSym = smv.getAnnotatedSeq().getAnnotation(meth);
		
		//Remove previous view mode glyph
		TierGlyph mainTier = smv.getTrack(annotSym, style, style.getSeparate() ? TierGlyph.Direction.FORWARD : TierGlyph.Direction.BOTH);
		smv.getSeqMap().removeItem(mainTier.getViewModeGlyph());
		
		if (style.getSeparate()) {
			TierGlyph secondTier = smv.getTrack(annotSym, style, style.getSeparate() ? TierGlyph.Direction.REVERSE : TierGlyph.Direction.BOTH);
			smv.getSeqMap().removeItem(secondTier.getViewModeGlyph());
		}
		
		addAnnotationGlyphs(smv, annotSym);
	}
	
	private void addAnnotationGlyphs(SeqMapView smv, SymWithProps annotSym) {
		// Map symmetry subclass or method type to a factory, and call factory to make glyphs
		String meth = BioSeq.determineMethod(annotSym);

		if (meth != null) {
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);

			Operator operator = determineOperator(annotSym);
			if(operator != null){
				if(!operator.supportsTwoTrack()){
					style.setSeparate(false);
				}
			}
			TierGlyph mainTier = smv.getTrack(annotSym, style, style.getSeparate() ? TierGlyph.Direction.FORWARD : TierGlyph.Direction.BOTH);
			mainTier.setInfo(annotSym);
			if (style.getSeparate()) {
				TierGlyph secondTier = smv.getTrack(annotSym, style, style.getSeparate() ? TierGlyph.Direction.REVERSE : TierGlyph.Direction.BOTH);
				secondTier.setInfo(annotSym);
			}
			return;
		}
	}

	private Operator determineOperator(SymWithProps sym){
		String meth = BioSeq.determineMethod(sym);

		if (meth != null) {
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
			return TransformHolder.getInstance().getOperator(style.getOperator());
		}
		
		return null;
	}
		
	private void doMiddlegroundShading(SeqMapView gviewer, SymWithProps annotSym, BioSeq seq) {
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
			if (seq != gviewer.getViewSeq()) {
				inverse = gviewer.transformForViewSeq(inverse, seq);
			}
			int child_count = inverse.getChildCount();
			for (int i = 0; i < child_count; i++) {
				SeqSymmetry child = inverse.getChild(i);
				for (int j = 0; j < child.getSpanCount(); j++) {
					SeqSpan ospan = child.getSpan(j);
					if(ospan.getBioSeq() != gviewer.getViewSeq())
						continue;
					
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

	public void delete(AffyTieredMap map, String method, ITrackStyleExtended style){
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		GenericFeature feature = style.getFeature();
		
		// If genome is selected then delete all syms on the all seqs.
		if(IGBConstants.GENOME_SEQ_ID.equals(seq.getID())){
			GeneralLoadView.getLoadView().removeFeature(feature, true);
			return;
		}

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
	
	public void addEmptyTierFor(GenericFeature feature, SeqMapView gviewer, boolean setViewMode) {

		// No sequence selected or if it is cytoband or it is residue file. Then return
		if(gviewer.getAnnotatedSeq() == null || feature.featureName.equals(CytobandParser.CYTOBAND) ||
				feature.featureName.toLowerCase().contains(CytobandParser.CYTOBAND) ||
				(feature.symL != null && (feature.symL.isResidueLoader() || feature.symL.getExtension().equalsIgnoreCase("cyt")))){
			return;
		}
		
		ITrackStyleExtended style;

		// If feature has at least one track then don't add default.
		// Also if track has been loaded on one sequence then load it
		// for other sequence.
		if (!feature.getMethods().isEmpty()) {
			for (String method : feature.getMethods()) {
				if(method.endsWith(ProbeSetGlyphFactory.NETAFFX_PROBESETS) ||
						method.equals(CytobandParser.CYTOBAND_TIER_NAME)){
					continue;
				}
				style = getStyle(method, feature);
				
				if(style == null)
					continue;
				
				addTierFor(style, gviewer, feature.getRequestSym(), setViewMode);
			}
		} else {
			style = getStyle(feature.getURI().toString(), feature);
			style.setFeature(feature);
			addTierFor(style, gviewer, feature.getRequestSym(), setViewMode);
		}

	}

	public ITrackStyleExtended getStyle(String method, GenericFeature feature) {
		if (GraphSymUtils.isAGraphExtension(feature.getExtension())) {
			GraphState state = DefaultStateProvider.getGlobalStateProvider().getGraphState(
					method, feature.featureName, feature.getExtension(), feature.featureProps);
			
			if(state.getTierStyle().getFloatTier())
				return null;
			
			return state.getTierStyle();
		}else{
			return DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(
				method, feature.featureName, feature.getExtension(), feature.featureProps);
		}
	}

	private void addTierFor(ITrackStyleExtended style, SeqMapView gviewer, SeqSymmetry requestSym, boolean setViewMode) {
		if(setViewMode){
			MapViewGlyphFactoryI factory = MapViewModeHolder.getInstance().getAutoloadFactory(style);
			String viewmode = factory.getName();
			style.setViewMode(viewmode);
		}
		RootSeqSymmetry rootSym = null;
		if(!style.isGraphTier()){
			Direction direction = style.getSeparate() ? Direction.FORWARD : Direction.BOTH;
			//rootSym = (category == FileTypeCategory.ScoredContainer) ? new ScoredContainerSym() : new TypeContainerAnnot(style.getMethodName());
			TierGlyph tgfor = gviewer.getTrack(rootSym, style, direction);
			tgfor.setUnloadedOK(true);
			tgfor.initUnloaded();
			if (style.getSeparate()) {
				TierGlyph tgrev = gviewer.getTrack(rootSym, style, Direction.REVERSE);
				tgrev.setUnloadedOK(true);
				tgrev.initUnloaded();
			}
		}else {
			//rootSym = new GraphSym(new int[]{}, new float[]{}, style.getMethodName(), seq);
			TierGlyph tg = gviewer.getTrack(rootSym, style, Direction.NONE);
			tg.setUnloadedOK(true);
			tg.initUnloaded();
		}
		return;
	}
}
