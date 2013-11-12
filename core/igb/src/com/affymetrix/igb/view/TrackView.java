package com.affymetrix.igb.view;

import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.shared.MapTierTypeHolder;
import com.affymetrix.igb.view.factories.AbstractTierGlyph;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;
import com.affymetrix.igb.view.factories.ProbeSetGlyphFactory;

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
		Map<ITrackStyleExtended, TierGlyph> style2track;
		if (style.isGraphTier()) {
			style2track = gstyle2track;
		} else if (tier_direction == TierGlyph.Direction.REVERSE) {
			style2track = style2reverseTierGlyph;
		} else {
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
	synchronized TierGlyph getTrack(SeqMapView smv, ITrackStyleExtended style, TierGlyph.Direction tier_direction) {
		AffyTieredMap seqmap = smv.getSeqMap();
		TierGlyph tierGlyph;
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
			}else {
				style2forwardTierGlyph.put(style, tierGlyph);
			}
			if (seqmap.getTierIndex(tierGlyph) == -1) {
				boolean above_axis = (tier_direction != TierGlyph.Direction.REVERSE);
				seqmap.addTier(tierGlyph, above_axis);
			}
		} else if (seqmap.getTierIndex(tierGlyph) == -1) { // 
			boolean above_axis = (tier_direction != TierGlyph.Direction.REVERSE);
			seqmap.addTier(tierGlyph, above_axis);
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
			RootSeqSymmetry annotSym = seq.getAnnotation(i);
			// skip over any cytoband data.  It is shown in a different way
			if (annotSym instanceof TypeContainerAnnot) {
				TypeContainerAnnot tca = (TypeContainerAnnot) annotSym;
				if (CytobandGlyph.CYTOBAND_TIER_REGEX.matcher(tca.getType()).matches()) {
					continue;
				}
			}
			addAnnotationGlyphs(smv, annotSym, seq);
		}
	}

	void addDependentAndEmptyTrack(SeqMapView smv, BioSeq seq) {
		for(GenericFeature feature : GeneralLoadUtils.getVisibleFeatures()){
			addEmptyTierFor(feature, smv);
		}
	}
	
	private void addAnnotationGlyphs(SeqMapView smv, RootSeqSymmetry annotSym, BioSeq seq) {
		// Map symmetry subclass or method type to a factory, and call factory to make glyphs
		String meth = BioSeq.determineMethod(annotSym);
		if (meth != null) {
			MapTierGlyphFactoryI factory = MapTierTypeHolder.getInstance().getDefaultFactoryFor(annotSym.getCategory());
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
			factory.createGlyphs(annotSym, style, smv, seq);
		}
	}
		
	public void deleteSymsOnSeq(SeqMapView smv, String method, BioSeq seq, GenericFeature feature){
		
		if (seq != null) {
			SeqSymmetry sym = seq.getAnnotation(method);
			if (sym != null) {
				if(sym instanceof GraphSym){
					GlyphI glyph = smv.getSeqMap().getItemFromTier(sym);
					if(glyph != null){
						if(glyph instanceof GraphGlyph){
							smv.split((GraphGlyph)glyph);
						}
						//map.removeItem(glyph);
					}
				} else if (CytobandGlyph.CYTOBAND_TIER_REGEX.matcher("").reset(method).matches()) {
					GlyphI glyph = smv.getAxisTier().getItem(sym);
					if(glyph != null){
						smv.getSeqMap().removeItem(glyph);
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
	
	public void addEmptyTierFor(GenericFeature feature, SeqMapView gviewer) {

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
				
				addTierFor(style, gviewer);
			}
		} else {
			style = getStyle(feature.getURI().toString(), feature);
			style.setFeature(feature);
			addTierFor(style, gviewer);
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

	public void addTierFor(ITrackStyleExtended style, SeqMapView gviewer) {
		if(!style.isGraphTier()){
			TierGlyph.Direction direction = style.getSeparate() ? TierGlyph.Direction.FORWARD : TierGlyph.Direction.BOTH;
			//rootSym = (category == FileTypeCategory.ScoredContainer) ? new ScoredContainerSym() : new TypeContainerAnnot(style.getMethodName());
			TierGlyph tgfor = gviewer.getTrack(style, direction);
			if(tgfor.getChildCount() == 0){
				((AbstractTierGlyph)tgfor).initUnloaded();
			}
			tgfor.pack(gviewer.getSeqMap().getView());
			if (style.getSeparate()) {
				TierGlyph tgrev = gviewer.getTrack(style, TierGlyph.Direction.REVERSE);
				if(tgrev.getChildCount() == 0){
					((AbstractTierGlyph)tgrev).initUnloaded();
				}
				tgrev.pack(gviewer.getSeqMap().getView());
			}
		}else {
			//rootSym = new GraphSym(new int[]{}, new float[]{}, style.getMethodName(), seq);
			TierGlyph tg = gviewer.getTrack(style, TierGlyph.Direction.NONE);
			if(tg.getChildCount() == 0 && !style.getFloatTier() && !style.getJoin()){
				((AbstractTierGlyph)tg).initUnloaded();
			}
			tg.pack(gviewer.getSeqMap().getView());
		}
	}
	
	private void addDummyChild(TierGlyph tierGlyph){
		if(tierGlyph.getChildCount() == 0 
				&& !tierGlyph.getAnnotStyle().getFloatTier() 
				&& !tierGlyph.getAnnotStyle().getJoin()){
			GlyphI glyph = new FillRectGlyph();
			glyph.setCoords(0, 0, 0, tierGlyph.getChildHeight());
			tierGlyph.addChild(glyph);
		}
	}
}
