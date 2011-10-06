package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;

/**
 *
 * @author hiralv
 */
public class EmptyTierGlyphFactory {
	
	public static void addEmtpyTierfor(GenericFeature feature, SeqMapView gviewer) {

		// No seqeunce selected. Return.
		if(gviewer.getAnnotatedSeq() == null){
			return;
		}
		
		ITrackStyleExtended style;

		// If feature has at least one track then don't add default.
		// Also if track has been loaded on one sequence then load it
		// for other sequence.
		if (!feature.getMethods().isEmpty()) {
			for (String method : feature.getMethods()) {
				style = getStyle(method, feature);
				addTierFor(style, gviewer);
			}
		} else {
			style = getStyle(feature.getURI().toString(), feature);
			addTierFor(style, gviewer);
			style.setFeature(feature);
		}

	}

	private static ITrackStyleExtended getStyle(String method, GenericFeature feature) {
		if (GraphSymUtils.isAGraphExtension(feature.getExtension())) {
			GraphState state = DefaultStateProvider.getGlobalStateProvider().getGraphState(
					method, feature.featureName, feature.getExtension());
			
			return state.getComboStyle() != null? state.getComboStyle(): state.getTierStyle();
		}else{
			return DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(
				method, feature.featureName, feature.getExtension(), feature.featureProps);
		}
	}
	
	private static void addTierFor(ITrackStyleExtended style, SeqMapView gviewer) {
		int slots = getAverageSlots(gviewer.getSeqMap());
		TierGlyph[] tiers = new TierGlyph[2];
				
		double height = style.getHeight();
		if(!style.isGraphTier()){
			tiers = gviewer.getTiers(false, style, true);
			height = style.getLabelField() == null || style.getLabelField().isEmpty() ? height : height * 2;
		}else {
			tiers[0] = gviewer.getGraphTrack(style, TierGlyph.Direction.NONE);
		}

		if (style.getSeparate() && !style.isGraphTier()) {
			addEmptyChild(tiers[0], height, slots);
			addEmptyChild(tiers[1], height, slots);
		} else {
			addEmptyChild(tiers[0], height, slots);
		}

	}
		
	private static int getAverageSlots(AffyTieredMap seqmap) {
		int slot = 1;
		int noOfTiers = 1;
		for(TierGlyph tier : seqmap.getTiers()){
			if(!tier.isVisible())
				continue;
			
			slot += tier.getActualSlots();
			noOfTiers += 1;
		}
		
		return slot/noOfTiers;
	}
	
	private static void addEmptyChild(TierGlyph tier, double height, int slots){
		if (tier.getChildCount() <= 0) {			
			for(int i=0; i<slots; i++){
				Glyph glyph = new Glyph() {};
				glyph.setCoords(0, 0, 0, height);
				tier.addChild(glyph);
			}
		}
	}

}
