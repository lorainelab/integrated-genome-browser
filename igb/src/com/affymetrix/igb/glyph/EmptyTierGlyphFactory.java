package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GraphSymUtils;

import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.viewmode.MapViewModeHolder;
import com.affymetrix.igb.viewmode.TierGlyphViewMode;

/**
 *
 * @author hiralv
 */
public class EmptyTierGlyphFactory {
	
	public static void addEmtpyTierfor(GenericFeature feature, SeqMapView gviewer, boolean setViewMode) {

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
			
			if(state.getTierStyle().getFloatGraph())
				return null;
			
			return state.getComboStyle() != null? state.getComboStyle(): state.getTierStyle();
		}else{
			return DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(
				method, feature.featureName, feature.getExtension(), feature.featureProps);
		}
	}
	
	private static void addTierFor(ITrackStyleExtended style, SeqMapView gviewer, SeqSymmetry requestSym, boolean setViewMode) {
		if(setViewMode){
			FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(style.getFileType());
			FileTypeCategory category = (fth == null) ? FileTypeCategory.Annotation : fth.getFileTypeCategory();
			String viewmode = MapViewModeHolder.getInstance().getDefaultFactoryFor(category).getName();
			style.setViewMode(viewmode);
		}
		if(!style.isGraphTier()){
			Direction direction = style.getSeparate() ? Direction.FORWARD : Direction.BOTH;
			TierGlyphViewMode tgfor = (TierGlyphViewMode)gviewer.getTrack(null, style, direction);
			tgfor.reset();
			if (style.getSeparate()) {
				TierGlyphViewMode tgrev = (TierGlyphViewMode)gviewer.getTrack(null, style, Direction.REVERSE);
				tgrev.reset();
			}
		}else {
			TierGlyphViewMode tg = (TierGlyphViewMode)gviewer.getTrack(null, style, Direction.NONE);
			tg.reset();
		}
		return;
	}
			
}
