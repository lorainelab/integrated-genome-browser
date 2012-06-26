package com.affymetrix.igb.viewmode;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class DefaultSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	private final MapViewGlyphFactoryI detailGlyphFactory;
	private final MapViewGlyphFactoryI depthFactory;

	private class DefaultSemanticZoomGlyph extends SemanticZoomGlyph {
		private ViewModeGlyph depthGlyph; 
		
		private DefaultSemanticZoomGlyph(SeqSymmetry sym) {
			super(sym);
		}

		@Override
		public ViewModeGlyph getGlyph(SeqMapViewExtendedI smv) {
			if(isDetail(smv.getSeqMap().getView())){
				try {
					return getDetailGlyph(smv, detailGlyphFactory);
				} catch (Exception ex) {
					Logger.getLogger(DefaultSemanticZoomGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			return depthGlyph;
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
	protected SemanticZoomGlyph getSemanticZoomGlyph(SeqSymmetry sym) {
		return new DefaultSemanticZoomGlyph(sym);
	}
}
