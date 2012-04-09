package com.affymetrix.igb.shared;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.view.load.GeneralLoadUtils;

public abstract class IndexedSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	protected final MapViewGlyphFactoryI defaultGlyphFactory;
	protected final MapViewGlyphFactoryI graphGlyphFactory;

	public IndexedSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super();
		this.defaultGlyphFactory = defaultGlyphFactory;
		this.graphGlyphFactory = graphGlyphFactory;
	}

	public abstract String getIndexedFileName(String method, Direction direction);

	protected boolean hasIndex(String uri) {
		if (uri == null) {
			return false;
		}
		return GeneralUtils.urlExists(getIndexedFileName(uri, Direction.BOTH));
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		return defaultGlyphFactory.isCategorySupported(category);
	}

	@Override
	public boolean isURISupported(String uri) {
		return hasIndex(uri);
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return hasIndex(uri);
	}

	protected AbstractGraphGlyph getEmptyGraphGlyph(SimpleSeqSpan span, ITrackStyleExtended trackStyle, SeqMapViewExtendedI gviewer) {
		GraphSym graf = new GraphSym(new int[]{span.getMin()}, new int[]{span.getLength()}, new float[]{0}, trackStyle.getMethodName(), span.getBioSeq());
		return (AbstractGraphGlyph)graphGlyphFactory.getViewModeGlyph(graf, trackStyle, Direction.BOTH, gviewer);
	}

	// glyph class
	public abstract class IndexedSemanticZoomGlyph extends SemanticZoomGlyphFactory.SemanticZoomGlyph {
		protected ViewModeGlyph defaultGlyph; 
		protected final SeqMapViewExtendedI smv;
		protected SymLoader detailSymL;
		protected SymLoader summarySymL;
		protected SimpleSeqSpan saveSpan;

		public IndexedSemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style,
			Direction direction, SeqMapViewExtendedI smv) {
			super(sym, style, direction, smv);
			this.smv = smv;
			saveSpan = null;
		}

		public abstract boolean isDetail(ViewI view);

		@Override
		protected void init(SeqSymmetry sym, ITrackStyleExtended trackStyle,
			Direction direction, SeqMapViewExtendedI gviewer) {
			viewModeGlyphs = new HashMap<String, ViewModeGlyph>();
			BioSeq seq = gviewer.getAnnotatedSeq();
			defaultGlyph = getEmptyGraphGlyph(new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq), trackStyle, gviewer);
		}

		protected ViewModeGlyph getDetailGlyph(SimpleSeqSpan span) throws Exception {
			GenericFeature feature = style.getFeature();
			SeqSymmetry optimized_sym = feature.optimizeRequest(span);	
			if (optimized_sym != null) {
				boolean result = GeneralLoadUtils.loadFeaturesForSym(feature, optimized_sym);
				if (!result) {
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "getDetailGlyph() result is false");
					return null;
				}
			}
			SymWithProps rootSym = span.getBioSeq().getAnnotation(style.getMethodName());
			if (rootSym == null) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "getDetailGlyph() rootSym is null");
				return null;
			}
			return defaultGlyphFactory.getViewModeGlyph(rootSym, style, Direction.BOTH, smv);
		}

		protected ViewModeGlyph getSummaryGlyph(SimpleSeqSpan span) throws Exception {
			ViewModeGlyph resultGlyph = null;
			List<? extends SeqSymmetry> symList = summarySymL.getRegion(span);
			if (symList.size() > 0) {
				GraphSym gsym = (GraphSym)symList.get(0);
				resultGlyph = (AbstractGraphGlyph)graphGlyphFactory.getViewModeGlyph(gsym, style, Direction.BOTH, smv);
			}
			if (resultGlyph != null) {
				((AbstractGraphGlyph)resultGlyph).drawHandle(false);
				resultGlyph.setCoords(resultGlyph.getCoordBox().x, resultGlyph.getCoordBox().y, resultGlyph.getCoordBox().width, style.getMaxDepth() * style.getHeight());
			}
			return resultGlyph;
		}

		@Override
		public ViewModeGlyph getGlyph(ViewI view) {
			BioSeq seq = smv.getAnnotatedSeq();
	        int startBase = (int)Math.round(view.getCoordBox().getX());
			int length = (int)Math.round(view.getCoordBox().getWidth());
	        int endBase = startBase + length;
	        SimpleSeqSpan span = new SimpleSeqSpan(startBase, endBase, seq);
	        if (span.equals(saveSpan) && lastUsedGlyph != null) {
	        	return lastUsedGlyph;
	        }
			try {
				ViewModeGlyph resultGlyph = null;
				if (isDetail(view)) {
					resultGlyph = getDetailGlyph(span);
				}
				else {
					resultGlyph = getSummaryGlyph(span);
				}
				if (resultGlyph == null) {
					resultGlyph = getEmptyGraphGlyph(span, style, smv);
				}
				resultGlyph.setSelectable(false);
				double y = resultGlyph.getCoordBox().y;
				if (y == 0) {
					y = getCoordBox().y;
				}
				double height = resultGlyph.getCoordBox().height;
				if (height == 0) {
					height = getCoordBox().height;
				}
				getCoordBox().setRect(startBase, y, length, height);
				resultGlyph.setCoordBox(getCoordBox());
				resultGlyph.setVisibility(true);
				resultGlyph.setParent(getParent());
				resultGlyph.setScene(getScene());
				double saveY = resultGlyph.getCoordBox().y;
				resultGlyph.pack(view);
				if (resultGlyph.getCoordBox().y != saveY) {
					resultGlyph.moveAbsolute(resultGlyph.getCoordBox().x, saveY);
				}
				saveSpan = span;
				lastUsedGlyph = resultGlyph;
				viewModeGlyphs.put("lastUsed", lastUsedGlyph);
				return resultGlyph;
			}
			catch (Exception x) {
				Logger logger = Logger.getLogger(this.getClass().getName());
				logger.log(Level.SEVERE, "Error in Indexed Semantic zoom", x);
				return null;
			}
		}

		@Override
		public ViewModeGlyph getDefaultGlyph() {
			return defaultGlyph;
		}

		@Override
		public void processParentCoordBox(Rectangle2D.Double parentCoordBox) {
			super.processParentCoordBox(parentCoordBox);
			if (defaultGlyph != null) {
				defaultGlyph.setCoordBox(parentCoordBox);
			}
		}
	}
}
