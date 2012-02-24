package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GraphSymUtils;

import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;

import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.util.TrackUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.viewmode.MapViewModeHolder;

/**
 *
 * @author hiralv
 */
public class EmptyTierGlyphFactory {
	
	public static void addEmtpyTierfor(GenericFeature feature, SeqMapView gviewer, boolean setViewMode) {

		// No seqeunce selected or if it is cytoband or it is residue file. Then return
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
				if(method.endsWith(ProbeSetDisplayGlyphFactory.NETAFFX_PROBESETS) ||
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
			
			if(state.getFloatGraph())
				return null;
			
			return state.getComboStyle() != null? state.getComboStyle(): state.getTierStyle();
		}else{
			return DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(
				method, feature.featureName, feature.getExtension(), feature.featureProps);
		}
	}
	
	private static void addTierFor(ITrackStyleExtended style, SeqMapView gviewer, SeqSymmetry requestSym, boolean setViewMode) {
		int slots = gviewer.getAverageSlots();
		TierGlyph[] tiers = new TierGlyph[2];
				
		double height = style.getHeight();
		if (TrackUtils.getInstance().useViewMode(style.getMethodName())) {
			if(setViewMode){
				FileTypeCategory category = FileTypeHolder.getInstance().getFileTypeHandlerForURI(style.getMethodName()).getFileTypeCategory();
				String viewmode = MapViewModeHolder.getInstance().getDefaultFactoryFor(category).getName();
				style.setViewMode(viewmode);
			}
			if(!style.isGraphTier()){
				gviewer.getTrack(null, style, style.getSeparate() ? Direction.FORWARD : Direction.BOTH);
				if (style.getSeparate()) {
					gviewer.getTrack(null, style, Direction.REVERSE);
				}
			}else {
				gviewer.getTrack(null, style, Direction.NONE);
			}
			return;
		}
		else {
			if(!style.isGraphTier()){
				tiers = gviewer.getTiers(style, true);
				height = style.getLabelField() == null || style.getLabelField().isEmpty() ? height : height * 2;
			}else {
				tiers[0] = gviewer.getGraphTrack(style, TierGlyph.Direction.NONE);
			}
		}

		if (style.getSeparate() && !style.isGraphTier()) {
			addEmptyChild(tiers[0], height, slots, requestSym, gviewer.getAnnotatedSeq());
			addEmptyChild(tiers[1], height, slots, requestSym, gviewer.getAnnotatedSeq());
		} else {
			addEmptyChild(tiers[0], height, slots, requestSym, gviewer.getAnnotatedSeq());
		}

	}
			
	private static void addEmptyChild(TierGlyph tier, double height, int slots,
			SeqSymmetry requestSym, BioSeq seq) {
		if (tier.getChildCount() <= 0) {
			Glyph glyph;

			for (int i = 0; i < slots; i++) {
				// Add empty child.
				glyph = new Glyph() {
				};
				glyph.setCoords(0, 0, 0, height);
				tier.addChild(glyph);
			}

			// Add middle glyphs.
			SeqSymmetry inverse = SeqUtils.inverse(requestSym, seq);
			int child_count = inverse.getChildCount();
			//If any request was made.
			if (child_count > 0) {
				for (int i = 0; i < child_count; i++) {
					SeqSymmetry child = inverse.getChild(i);
					for (int j = 0; j < child.getSpanCount(); j++) {
						SeqSpan ospan = child.getSpan(j);
						if (ospan.getLength() > 1) {
							if (tier != null) {
								glyph = new FillRectGlyph();
								glyph.setCoords(ospan.getMin(), 0, ospan.getLength() - 1, 0);
								tier.addMiddleGlyph(glyph);
							}
						}
					}
				}
			} else {
				glyph = new FillRectGlyph();
				glyph.setCoords(seq.getMin(), 0, seq.getLength() - 1, 0);
				tier.addMiddleGlyph(glyph);
			}
		}
	}

}
