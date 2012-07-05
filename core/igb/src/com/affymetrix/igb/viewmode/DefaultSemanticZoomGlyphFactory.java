package com.affymetrix.igb.viewmode;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.MapViewModeHolder;
import com.affymetrix.igb.shared.SemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class DefaultSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {

	private class DefaultSemanticZoomGlyph extends SemanticZoomGlyph {
		private ViewModeGlyph summaryGlyph; 
		
		private DefaultSemanticZoomGlyph(MapViewGlyphFactoryI detailGlyphFactory, MapViewGlyphFactoryI summaryGlyphFactory, SeqSymmetry sym, SeqMapViewExtendedI smv) {
			super(detailGlyphFactory, summaryGlyphFactory, sym, smv);
		}

		@Override
		protected ViewModeGlyph createGlyphs(RootSeqSymmetry rootSym, MapViewGlyphFactoryI factory, SeqMapViewExtendedI smv) {
			ViewModeGlyph result = factory.getViewModeGlyph(rootSym, style, direction, smv);
			summaryGlyph = summaryGlyphFactory.getViewModeGlyph((SeqSymmetry)detailGlyph.getInfo(), style, direction, smv);
			prepareViewModeGlyph(summaryGlyph, smv.getSeqMap().getView());
			return result;
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
			if (!summaryGlyphFactory.getName().equals(style.getViewMode())) {
				summaryGlyphFactory = MapViewModeHolder.getInstance().getViewFactory(style.getViewMode());
				summaryGlyph = summaryGlyphFactory.getViewModeGlyph((SeqSymmetry)summaryGlyph.getInfo(), style, direction, smv);
			}
			return summaryGlyph;
		}

		@Override
		public void init(SeqSymmetry sym, ITrackStyleExtended style, Direction direction, SeqMapViewExtendedI smv) {
			viewModeGlyphs = new HashMap<String, ViewModeGlyph>();
			summaryGlyph = summaryGlyphFactory.getViewModeGlyph(sym, style, direction, smv);
			viewModeGlyphs.put(summaryGlyphFactory.getName(), summaryGlyph);
			detailGlyph = detailGlyphFactory.getViewModeGlyph(sym, style, direction, smv);
			viewModeGlyphs.put(detailGlyphFactory.getName(), detailGlyph);
		}

		@Override
		public ViewModeGlyph getDefaultGlyph() {
			return summaryGlyph;
		}
	}

	public DefaultSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultDetailGlyphFactory, MapViewGlyphFactoryI defaultSummaryGlyphFactory) {
		super(defaultDetailGlyphFactory, defaultSummaryGlyphFactory);
	}

	@Override
	public boolean supportsTwoTrack() {
		return true;
	}

	@Override
	public String getName() {
		return "semantic_zoom_" + defaultDetailGlyphFactory.getName();
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		return defaultDetailGlyphFactory.isCategorySupported(category);
	}

	@Override
	protected SemanticZoomGlyph getSemanticZoomGlyph(MapViewGlyphFactoryI defaultDetailGlyphFactory, MapViewGlyphFactoryI defaultSummaryGlyphFactory, SeqSymmetry sym, SeqMapViewExtendedI smv) {
		return new DefaultSemanticZoomGlyph(defaultDetailGlyphFactory, defaultSummaryGlyphFactory, sym, smv);
	}
}
