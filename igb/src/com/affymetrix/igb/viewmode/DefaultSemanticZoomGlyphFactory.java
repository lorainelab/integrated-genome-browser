package com.affymetrix.igb.viewmode;

import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.SemanticZoomRule;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class DefaultSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	private final MapViewGlyphFactoryI detailGlyphFactory;
	private final MapViewGlyphFactoryI depthFactory;

	private class DefaultSemanticZoomRule implements SemanticZoomRule {
		private static final double ZOOM_X_SCALE = 0.002;
		private final ViewModeGlyph depthGlyph; 
		private final ViewModeGlyph detailGlyph; 
		private final Map<String, ViewModeGlyph> allViewModeGlyphs;
		private DefaultSemanticZoomRule(SeqSymmetry sym, ITrackStyleExtended style,
			Direction direction, SeqMapViewExtendedI smv) {
			super();
			allViewModeGlyphs = new HashMap<String, ViewModeGlyph>();
			depthGlyph = depthFactory.getViewModeGlyph(sym, style, direction, smv);
			allViewModeGlyphs.put(depthFactory.getName(), depthGlyph);
			detailGlyph = detailGlyphFactory.getViewModeGlyph(sym, style, direction, smv);
			allViewModeGlyphs.put(detailGlyphFactory.getName(), detailGlyph);
		}

		@Override
		public ViewModeGlyph getGlyph(ViewI view) {
			return view.getTransform().getScaleX() < ZOOM_X_SCALE ? depthGlyph : detailGlyph;
		}

		@Override
		public Map<String, ViewModeGlyph> getAllViewModeGlyphs() {
			return allViewModeGlyphs;
		}

		@Override
		public ViewModeGlyph getDefaultGlyph() {
			return depthGlyph;
		}
	}

	public DefaultSemanticZoomGlyphFactory(MapViewGlyphFactoryI detailGlyphFactory, MapViewGlyphFactoryI depthFactory) {
		this.detailGlyphFactory = detailGlyphFactory;
		this.depthFactory = depthFactory;
	}

	@Override
	public String getName() {
		return "semantic_zoom_" + detailGlyphFactory.getName();
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		return detailGlyphFactory.isCategorySupported(category);
	}

	@Override
	protected SemanticZoomRule getRule(SeqSymmetry sym, ITrackStyleExtended style,
		Direction direction, SeqMapViewExtendedI smv) {
		return new DefaultSemanticZoomRule(sym, style, direction, smv);
	}
}
