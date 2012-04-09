package com.affymetrix.igb.viewmode;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.IndexedSemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class BaiSemanticZoomGlyphFactory extends IndexedSemanticZoomGlyphFactory {
	public static final String BAI_ZOOM_DISPLAYER_EXTENSION = "bai";

	public BaiSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super(defaultGlyphFactory, graphGlyphFactory);
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym,
			ITrackStyleExtended style, Direction direction,
			SeqMapViewExtendedI smv) {
		return new BaiSemanticZoomGlyph(sym, style, direction, smv);
	}

	@Override
	public String getName() {
		return "bai_semantic_zoom";
	}

	@Override
	public String getIndexedFileName(String method, Direction direction) {
		return method + "." + BAI_ZOOM_DISPLAYER_EXTENSION;
	}

	// glyph class
	public class BaiSemanticZoomGlyph extends IndexedSemanticZoomGlyphFactory.IndexedSemanticZoomGlyph {
		private static final double ZOOM_X_SCALE = 0.002;
		private ViewModeGlyph saveSummaryGlyph;

		public BaiSemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style,
				Direction direction, SeqMapViewExtendedI smv) {
			super(sym, style, direction, smv);
		}

		@Override
		public boolean isPreLoaded() {
			return true;
		}

		@Override
		public boolean isDetail(ViewI view) {
			return view.getTransform().getScaleX() >= ZOOM_X_SCALE;
		}

		@Override
		protected void init(SeqSymmetry sym, ITrackStyleExtended trackStyle,
				Direction direction, SeqMapViewExtendedI gviewer) {
			super.init(sym, trackStyle, direction, gviewer);
			try {
				String method = (sym == null) ? trackStyle.getMethodName() : BioSeq.determineMethod(sym);
				detailSymL = FileTypeHolder.getInstance().getFileTypeHandlerForURI(method).createSymLoader(new URI(method), trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
				String baiUrl = getIndexedFileName(method, direction);
				URI baiUri = new URI(baiUrl);
				summarySymL = new BaiZoomSymLoader(baiUri, method, GenometryModel.getGenometryModel().getSelectedSeqGroup());
			}
			catch (Exception x) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "BaiSemanticZoom failed reading bai file", x);
			}
		}

		@Override
		protected ViewModeGlyph getSummaryGlyph(SimpleSeqSpan span) throws Exception {
			if (saveSummaryGlyph == null || !span.getBioSeq().equals(saveSpan.getBioSeq())) {
				saveSummaryGlyph = super.getSummaryGlyph(span);
			}
			return saveSummaryGlyph;
		}
	}
	// end glyph class
}
