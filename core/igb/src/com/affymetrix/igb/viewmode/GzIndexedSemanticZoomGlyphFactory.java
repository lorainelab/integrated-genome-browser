package com.affymetrix.igb.viewmode;

import java.awt.geom.Rectangle2D;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.shared.IndexedSemanticZoomGlyphFactory;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public abstract class GzIndexedSemanticZoomGlyphFactory extends IndexedSemanticZoomGlyphFactory {
//	protected final Operator TRANSFORM_OPERATOR = new com.affymetrix.genometryImpl.operator.LogTransform(Math.E);
	protected final Operator TRANSFORM_OPERATOR = new com.affymetrix.genometryImpl.operator.PowerTransformer(0.5);
	private MapViewGlyphFactoryI heatMapGraphGlyphFactory;
	private MapViewGlyphFactoryI graphGlyphFactory;

	public GzIndexedSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultDetailGlyphFactory, MapViewGlyphFactoryI heatMapGraphGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super(defaultDetailGlyphFactory, null);
		this.heatMapGraphGlyphFactory = heatMapGraphGlyphFactory;
		this.graphGlyphFactory = graphGlyphFactory;
	}


	@Override
	protected SemanticZoomGlyph getSemanticZoomGlyph(MapViewGlyphFactoryI defaultDetailGlyphFactory, MapViewGlyphFactoryI defaultSummaryGlyphFactory, SeqSymmetry sym, ITrackStyleExtended style){
		boolean useHeatMap = PreferenceUtils.getBooleanParam(PreferenceUtils.COVERAGE_SUMMARY_HEATMAP, PreferenceUtils.default_coverage_summary_heatmap);
		return new GzIndexedSemanticZoomGlyph(defaultDetailGlyphFactory, useHeatMap ? heatMapGraphGlyphFactory : graphGlyphFactory, sym);
	}
	
	@Override
	public String getIndexedFileName(String method, Direction direction) {
		return method + "." + getExtension();
	}

	protected abstract String getExtension();
	protected abstract SymLoader createSummarySymLoader(URI uri, String featureName, AnnotatedSeqGroup group);

	// glyph class
	public class GzIndexedSemanticZoomGlyph extends IndexedSemanticZoomGlyphFactory.IndexedSemanticZoomGlyph{
		private ViewModeGlyph saveSummaryGlyph;
		private Rectangle2D.Double saveSummaryCoordbox;

		public GzIndexedSemanticZoomGlyph(MapViewGlyphFactoryI detailGlyphFactory, MapViewGlyphFactoryI summaryGlyphFactory, SeqSymmetry sym) {
			super(detailGlyphFactory, summaryGlyphFactory, sym);
		}

		@Override
		public boolean isPreLoaded() {
			return true;
		}

		@Override
		protected void init(SeqSymmetry sym, ITrackStyleExtended trackStyle,
				Direction direction, SeqMapViewExtendedI gviewer) {
			super.init(sym, trackStyle, direction, gviewer);
			try {
				String method = (sym == null) ? trackStyle.getMethodName() : BioSeq.determineMethod(sym);
				detailSymL = FileTypeHolder.getInstance().getFileTypeHandlerForURI(method).createSymLoader(new URI(method), trackStyle.getMethodName(), GenometryModel.getGenometryModel().getSelectedSeqGroup());
				URI indexUri = new URI(getIndexedFileName(method, direction));
				summarySymL = createSummarySymLoader(indexUri, method, GenometryModel.getGenometryModel().getSelectedSeqGroup());
			}
			catch (Exception x) {
				Logger.getLogger(this.getClass().getPackage().getName()).log(
						Level.SEVERE, "TbiSemanticZoom failed reading tbi file", x);
			}
		}

		@Override
		protected Operator getSummaryTransformOperator() {
			return TRANSFORM_OPERATOR;
		}

		@Override
		protected ViewModeGlyph getSummaryGlyph(SeqMapViewExtendedI smv) throws Exception {
			if (saveSummaryGlyph == null /* || !span.getBioSeq().equals(saveSpan.getBioSeq()) */) {
				saveSummaryGlyph = super.getSummaryGlyph(smv);
				saveSummaryGlyph.setPreferredHeight(
						saveSummaryGlyph.getStyleDepth() * saveSummaryGlyph.getChildHeight()
						, smv.getSeqMap().getView());
				saveSummaryCoordbox = new Rectangle2D.Double(saveSummaryGlyph.getCoordBox().x, saveSummaryGlyph.getCoordBox().y, saveSummaryGlyph.getCoordBox().width, saveSummaryGlyph.getCoordBox().height);
			}
			else if (lastUsedGlyph != saveSummaryGlyph && saveSummaryCoordbox != null) {
				Rectangle2D.Double coordBox = new Rectangle2D.Double(saveSummaryCoordbox.x, saveSummaryCoordbox.y, saveSummaryCoordbox.width, saveSummaryCoordbox.height);
				saveSummaryGlyph.setCoordBox(coordBox);
				Rectangle2D.Double coordbox = this.getCoordBox();
				lastUsedGlyph = saveSummaryGlyph;
				this.processParentCoordBox(coordbox);
				lastUsedGlyph.setCoordBox(coordbox);
				lastUsedGlyph.setTierGlyph(getTierGlyph());
			}
			return saveSummaryGlyph;
		}

		@Override
		protected void rangeChanged(SeqMapViewExtendedI smv){
			if (saveSummaryGlyph != null && ((GraphSym)saveSummaryGlyph.getInfo()).getGraphSeq() != smv.getAnnotatedSeq()) {
				saveSummaryGlyph = null;
			}
			super.rangeChanged(smv);
		}

		public void setSummaryViewMode(String viewmode, SeqMapViewExtendedI smv) {
			super.setSummaryViewMode(viewmode, smv);
			if (isLastSummary()) {
				saveSummaryGlyph = lastUsedGlyph;
				saveSummaryCoordbox = new Rectangle2D.Double(saveSummaryGlyph.getCoordBox().x, saveSummaryGlyph.getCoordBox().y, saveSummaryGlyph.getCoordBox().width, saveSummaryGlyph.getCoordBox().height);
			}
		}
	}
	// end glyph class
}
