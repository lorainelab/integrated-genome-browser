package com.affymetrix.igb.viewmode;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.IndexedSemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class TbiSemanticZoomGlyphFactory extends IndexedSemanticZoomGlyphFactory {
	public static final String TBI_ZOOM_DISPLAYER_EXTENSION = "tbi";

	public TbiSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super(defaultGlyphFactory, graphGlyphFactory);
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym,
			ITrackStyleExtended style, Direction direction,
			SeqMapViewExtendedI smv) {
		return new TbiSemanticZoomGlyph(sym, style, direction, smv);
	}

	@Override
	public String getName() {
		return "tbi_semantic_zoom";
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return super.canAutoLoad(uri) && uri.indexOf("__") > -1;
	}

	@Override
	public String getIndexedFileName(String method, Direction direction) {
		return method + "." + TBI_ZOOM_DISPLAYER_EXTENSION;
	}

	// glyph class
	public class TbiSemanticZoomGlyph extends IndexedSemanticZoomGlyphFactory.IndexedSemanticZoomGlyph {
		private static final double ZOOM_X_SCALE = 0.002;

		public TbiSemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style,
				Direction direction, SeqMapViewExtendedI smv) {
			super(sym, style, direction, smv);
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
				String tbiUrl = getIndexedFileName(method, direction);
				URI tbiUri = new URI(tbiUrl);
				summarySymL = new TbiZoomSymLoader(tbiUri, method, GenometryModel.getGenometryModel().getSelectedSeqGroup());
			}
			catch (Exception x) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "TbiSemanticZoom failed reading tbi file", x);
			}
		}
	}
	// end glyph class
}
