package com.affymetrix.igb.view;

import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
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
import com.affymetrix.igb.shared.MapTierGlyphFactoryI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.shared.MapTierTypeHolder;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;
import com.affymetrix.igb.view.factories.ProbeSetGlyphFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		else if (tier_direction == TierGlyph.Direction.FORWARD || tier_direction == TierGlyph.Direction.BOTH || tier_direction == TierGlyph.Direction.NONE) {
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
	TierGlyph getTrack(SeqMapView smv, ITrackStyleExtended style, TierGlyph.Direction tier_direction) {
		AffyTieredMap seqmap = smv.getSeqMap();
		TierGlyph tierGlyph = null;
		tierGlyph = getTier(style, tier_direction);
		if (tierGlyph == null) {
			tierGlyph = new DefaultTierGlyph(style);
			tierGlyph.setDirection(tier_direction);
			
			// do not set packer here, will be set in ViewModeGlyph
			if (style.isGraphTier()) {
				gstyle2track.put(style, tierGlyph);
			}
			else if (tier_direction == TierGlyph.Direction.REVERSE) {
				style2reverseTierGlyph.put(style, tierGlyph);
			}
			else if (tier_direction == TierGlyph.Direction.FORWARD || tier_direction == TierGlyph.Direction.BOTH || tier_direction == TierGlyph.Direction.NONE) {
				style2forwardTierGlyph.put(style, tierGlyph);
			}
			if (seqmap.getTierIndex(tierGlyph) == -1) {
				boolean above_axis = (tier_direction != TierGlyph.Direction.REVERSE);
				seqmap.addTier(tierGlyph, above_axis);
			}
		}
		
		if (!style.isGraphTier() && (tier_direction == TierGlyph.Direction.FORWARD || 
				tier_direction == TierGlyph.Direction.BOTH || tier_direction == TierGlyph.Direction.NONE)) {
			if (style.getSeparable()) {
				if (style.getSeparate()) {
					tierGlyph.setDirection(TierGlyph.Direction.FORWARD);
				} else {
					tierGlyph.setDirection(TierGlyph.Direction.BOTH);
				}
			} else {
				tierGlyph.setDirection(TierGlyph.Direction.NONE);
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
//					String meth = BioSeq.determineMethod(annotSym);
//					ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
//					if (style.getSeparate()) {
//						smv.getTrack(null, style, TierGlyph.Direction.FORWARD, DummyGlyphFactory.getInstance());
//						smv.getTrack(null, style, TierGlyph.Direction.REVERSE, DummyGlyphFactory.getInstance());
//					}
//					else {
//						smv.getTrack(null, style, TierGlyph.Direction.BOTH, DummyGlyphFactory.getInstance());
//					}
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
	
	private void addAnnotationGlyphs(SeqMapView smv, SymWithProps annotSym) {
		// Map symmetry subclass or method type to a factory, and call factory to make glyphs
		String meth = BioSeq.determineMethod(annotSym);

		if (meth != null && annotSym instanceof RootSeqSymmetry) {
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
			if(((RootSeqSymmetry)annotSym).getCategory() != style.getFileTypeCategory()){
				Logger.getLogger(TrackView.class.getName()).log(Level.SEVERE, 
						"File type category for {0} is {1} while style has category {2}", 
						new Object[]{annotSym, ((RootSeqSymmetry)annotSym).getCategory(), style.getFileTypeCategory()});
			}
			MapTierGlyphFactoryI factory = MapTierTypeHolder.getInstance().getDefaultFactoryFor(((RootSeqSymmetry)annotSym).getCategory());
			factory.createGlyphs(annotSym, style, smv);
		}
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
		RootSeqSymmetry rootSym = null;
		if(!style.isGraphTier()){
			Direction direction = style.getSeparate() ? Direction.FORWARD : Direction.BOTH;
			//rootSym = (category == FileTypeCategory.ScoredContainer) ? new ScoredContainerSym() : new TypeContainerAnnot(style.getMethodName());
			TierGlyph tgfor = gviewer.getTrack(style, direction);
			if(tgfor.getChildCount() == 0){
				tgfor.initUnloaded();
			}
			if (style.getSeparate()) {
				TierGlyph tgrev = gviewer.getTrack(style, Direction.REVERSE);
				if(tgrev.getChildCount() == 0){
					tgrev.initUnloaded();
				}
			}
		}else {
			//rootSym = new GraphSym(new int[]{}, new float[]{}, style.getMethodName(), seq);
			TierGlyph tg = gviewer.getTrack(style, Direction.NONE);
			if(tg.getChildCount() == 0 && !style.getFloatTier()){
				tg.initUnloaded();
			}
		}
	}
}
