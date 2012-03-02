package com.affymetrix.igb.viewmode;

import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class DefaultSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	private final MapViewGlyphFactoryI annotationGlyphFactory;
	private final MapViewGlyphFactoryI depthFactory;

	private class DefaultSemanticZoomRule implements SemanticZoomRule {
		private static final double ZOOM_X_SCALE = 0.002;
		private final ViewModeGlyph depthGlyph; 
		private final ViewModeGlyph annotationGlyph; 
		private final Map<String, ViewModeGlyph> allViewModeGlyphs;
		private DefaultSemanticZoomRule(SeqSymmetry sym,
				ITrackStyleExtended style, Direction direction) {
			super();
			allViewModeGlyphs = new HashMap<String, ViewModeGlyph>();
			depthGlyph = depthFactory.getViewModeGlyph(sym, style, direction);
			allViewModeGlyphs.put("Annotation depth", depthGlyph);
			annotationGlyph = annotationGlyphFactory.getViewModeGlyph(sym, style, direction);
			allViewModeGlyphs.put("annotation", annotationGlyph);
		}

		@Override
		public ViewModeGlyph getGlyph(ViewI view) {
			return view.getTransform().getScaleX() < ZOOM_X_SCALE ? depthGlyph : annotationGlyph;
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

	public DefaultSemanticZoomGlyphFactory(MapViewGlyphFactoryI annotationGlyphFactory, MapViewGlyphFactoryI depthFactory) {
		this.annotationGlyphFactory = annotationGlyphFactory;
		this.depthFactory = depthFactory;
	}

	@Override
	public String getName() {
		return "semantic zoom";
	}

	@Override
	public boolean isFileSupported(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation;
	}

	@Override
	protected SemanticZoomRule getRule(SeqSymmetry sym,
			ITrackStyleExtended style, Direction direction) {
		return new DefaultSemanticZoomRule(sym, style, direction);
	}
}
