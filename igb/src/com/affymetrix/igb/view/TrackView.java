package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
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
import com.affymetrix.igb.viewmode.MapViewModeHolder;
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


	/**List of Dependent data */
	private static final Map<BioSeq, List<DependentData>> dependent_list = new HashMap<BioSeq, List<DependentData>>();

	void clear() {
		style2forwardTierGlyph.clear();
		style2reverseTierGlyph.clear();
		gstyle2track.clear();
		dependent_list.clear();
	}

	/**
	 * Returns a forward and reverse tier for the given method, creating them if they don't
	 * already exist.
	 * Generally called by the Glyph Factory.
	 * Note that this can create empty tiers.  But if the tiers are not filled with
	 * something, they will later be removed automatically.
	 * @param smv  The SeqMapView (could be AltSplice) 
	 * @param sym  The SeqSymmetry (data model) for the track
	 * @param style  a non-null instance of IAnnotStyle; tier label and other properties
	 * are determined by the IAnnotStyle.
	 * @param tier_direction the direction of the track (FORWARD, REVERSE, or BOTH)
	 * @return an array of two (not necessarily distinct) tiers, one forward and one reverse.
	 * The array may instead contain two copies of one mixed-direction tier;
	 * in this case place glyphs for both forward and reverse items into it.
	 */
	TierGlyph getTrack(SeqMapView smv, SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction) {
		MapViewGlyphFactoryI factory = MapViewModeHolder.getInstance().getAutoloadFactory(style.getMethodName());
		return getTrack(smv, sym, style, tier_direction, factory);
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
		smv.processTrack(tierGlyph);
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
						getTrack(smv, null, style, TierGlyph.Direction.FORWARD, DummyGlyphFactory.getInstance());
						getTrack(smv, null, style, TierGlyph.Direction.REVERSE, DummyGlyphFactory.getInstance());
					}
					else {
						getTrack(smv, null, style, TierGlyph.Direction.BOTH, DummyGlyphFactory.getInstance());
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
			addEmptyTierFor(feature, smv, false);
		}
	}

	public void changeViewMode(SeqMapView gviewer, RootSeqSymmetry rootSym, ITrackStyleExtended style, ITrackStyleExtended comboStyle, String viewMode) {
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
			TierGlyph comboTier = getTrack(gviewer, rootSym, comboStyle, TierGlyph.Direction.NONE, null);
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
		// kludge to get GraphAdjuster tab to update Style box (graph type)
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		syms.add(rootSym);
		GenometryModel.getGenometryModel().setSelectedSymmetries(syms, gviewer);
	}

	public void addAnnotationGlyphs(SeqMapView smv, ITrackStyleExtended style){ 
		String meth = style.getMethodName();
		SymWithProps annotSym = smv.getAnnotatedSeq().getAnnotation(meth);
		
		//Remove previous view mode glyph
		TierGlyph mainTier = getTrack(smv, annotSym, style, style.getSeparate() ? TierGlyph.Direction.FORWARD : TierGlyph.Direction.BOTH);
		smv.getSeqMap().removeItem(mainTier.getViewModeGlyph());
		
		if (style.getSeparate()) {
			TierGlyph secondTier = getTrack(smv, annotSym, style, style.getSeparate() ? TierGlyph.Direction.REVERSE : TierGlyph.Direction.BOTH);
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
			TierGlyph mainTier = getTrack(smv, annotSym, style, style.getSeparate() ? TierGlyph.Direction.FORWARD : TierGlyph.Direction.BOTH);
			mainTier.setInfo(annotSym);
			if (style.getSeparate()) {
				TierGlyph secondTier = getTrack(smv, annotSym, style, style.getSeparate() ? TierGlyph.Direction.REVERSE : TierGlyph.Direction.BOTH);
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
			addTierFor(style, gviewer, feature.getRequestSym(), setViewMode);
			style.setFeature(feature);
		}

	}

	private static ITrackStyleExtended getStyle(String method, GenericFeature feature) {
		if (GraphSymUtils.isAGraphExtension(feature.getExtension())) {
			GraphState state = DefaultStateProvider.getGlobalStateProvider().getGraphState(
					method, feature.featureName, feature.getExtension());
			
			if(state.getTierStyle().getFloatGraph())
				return null;
			
			return state.getTierStyle();
		}else{
			return DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(
				method, feature.featureName, feature.getExtension(), feature.featureProps);
		}
	}
	
	private void addTierFor(ITrackStyleExtended style, SeqMapView gviewer, SeqSymmetry requestSym, boolean setViewMode) {
		if(setViewMode){
			FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(style.getFileType());
			FileTypeCategory category = (fth == null) ? FileTypeCategory.Annotation : fth.getFileTypeCategory();
			String viewmode = MapViewModeHolder.getInstance().getDefaultFactoryFor(category).getName();
			style.setViewMode(viewmode);
		}
		if(!style.isGraphTier()){
			Direction direction = style.getSeparate() ? Direction.FORWARD : Direction.BOTH;
			TierGlyph tgfor = getTrack(gviewer, null, style, direction);
			tgfor.reset();
			if (style.getSeparate()) {
				TierGlyph tgrev = getTrack(gviewer, null, style, Direction.REVERSE);
				tgrev.reset();
			}
		}else {
			TierGlyph tg = getTrack(gviewer, null, style, Direction.NONE);
			tg.reset();
		}
		return;
	}
			}
