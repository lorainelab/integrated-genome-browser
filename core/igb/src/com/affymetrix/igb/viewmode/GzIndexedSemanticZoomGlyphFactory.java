package com.affymetrix.igb.viewmode;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.action.AutoLoadThresholdAction;
import com.affymetrix.igb.shared.IndexedSemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.view.SeqMapView;

public abstract class GzIndexedSemanticZoomGlyphFactory extends IndexedSemanticZoomGlyphFactory {

	public GzIndexedSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super(defaultGlyphFactory, graphGlyphFactory);
	}

	@Override
	protected SemanticZoomGlyph getSemanticZoomGlyph(SeqSymmetry sym){
		return new GzIndexedSemanticZoomGlyph(sym);
	}
	
	@Override
	public String getName() {
		return getExtension() + "_semantic_zoom";
	}

	@Override
	public String getIndexedFileName(String method, Direction direction) {
		return method + "." + getExtension();
	}

	protected abstract String getExtension();
	protected abstract SymLoader createSummarySymLoader(URI uri, String featureName, AnnotatedSeqGroup group);

	// glyph class
	public class GzIndexedSemanticZoomGlyph extends IndexedSemanticZoomGlyphFactory.IndexedSemanticZoomGlyph{
//		private static final double ZOOM_X_SCALE = 0.002;
		private AutoLoadThresholdAction autoLoadThresholdAction;
		private ViewModeGlyph saveSummaryGlyph;

		public GzIndexedSemanticZoomGlyph(SeqSymmetry sym) {
			super(sym);
		}

		@Override
		public boolean isPreLoaded() {
			return true;
		}

		@Override
		public boolean isDetail(ViewI view) {
			return autoLoadThresholdAction.isDetail(getAnnotStyle());
			//return view.getTransform().getScaleX() >= ZOOM_X_SCALE;
		}

		@Override
		protected void init(SeqSymmetry sym, ITrackStyleExtended trackStyle,
				Direction direction, SeqMapViewExtendedI gviewer) {
			super.init(sym, trackStyle, direction, gviewer);
			autoLoadThresholdAction = ((SeqMapView)gviewer).getAutoLoad();
			try {
				String method = (sym == null) ? trackStyle.getMethodName() : BioSeq.determineMethod(sym);
				detailSymL = FileTypeHolder.getInstance().getFileTypeHandlerForURI(method).createSymLoader(new URI(method), trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
				URI indexUri = new URI(getIndexedFileName(method, direction));
				summarySymL = createSummarySymLoader(indexUri, method, GenometryModel.getGenometryModel().getSelectedSeqGroup());
			}
			catch (Exception x) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "TbiSemanticZoom failed reading tbi file", x);
			}
		}

		@Override
		protected ViewModeGlyph getSummaryGlyph(SeqMapViewExtendedI smv) throws Exception {
			if (saveSummaryGlyph == null /* || !span.getBioSeq().equals(saveSpan.getBioSeq()) */) {
				saveSummaryGlyph = super.getSummaryGlyph(smv);
			}
			return saveSummaryGlyph;
		}

		@Override
		public void seqSelectionChanged(SeqSelectionEvent evt) {
			saveSummaryGlyph = null;
		}
	}
	// end glyph class
}
