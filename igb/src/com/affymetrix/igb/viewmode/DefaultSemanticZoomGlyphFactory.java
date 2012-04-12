package com.affymetrix.igb.viewmode;

import java.util.HashMap;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class DefaultSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	private final MapViewGlyphFactoryI detailGlyphFactory;
	private final MapViewGlyphFactoryI depthFactory;

	private class DefaultSemanticZoomGlyph extends SemanticZoomGlyph {
		private static final double ZOOM_X_SCALE = 0.002;
		private ViewModeGlyph depthGlyph; 
		private ViewModeGlyph detailGlyph; 
		private DefaultSemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style,
			Direction direction, SeqMapViewExtendedI smv) {
			super(sym, style, direction, smv);
		}

		@Override
		public ViewModeGlyph getGlyph(ViewI view) {
			return view.getTransform().getScaleX() < ZOOM_X_SCALE ? depthGlyph : detailGlyph;
		}

		@Override
		public void init(SeqSymmetry sym, ITrackStyleExtended style, Direction direction, SeqMapViewExtendedI smv) {
			viewModeGlyphs = new HashMap<String, ViewModeGlyph>();
			depthGlyph = depthFactory.getViewModeGlyph(sym, style, direction, smv);
			viewModeGlyphs.put(depthFactory.getName(), depthGlyph);
			detailGlyph = detailGlyphFactory.getViewModeGlyph(sym, style, direction, smv);
			viewModeGlyphs.put(detailGlyphFactory.getName(), detailGlyph);
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
	public boolean supportsTwoTrack() {
		return true;
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
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym,
			ITrackStyleExtended style, Direction direction,
			SeqMapViewExtendedI smv) {
		return new DefaultSemanticZoomGlyph(sym, style, direction, smv);
	}
}
