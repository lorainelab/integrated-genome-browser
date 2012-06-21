package com.affymetrix.igb.shared;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.*;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.view.load.GeneralLoadUtils;

public abstract class IndexedSemanticZoomGlyphFactory extends SemanticZoomGlyphFactory {
	protected final MapViewGlyphFactoryI defaultGlyphFactory;
	protected final MapViewGlyphFactoryI graphGlyphFactory;
//	protected final Operator transformOperator = new com.affymetrix.genometryImpl.operator.LogTransform(Math.E);
	protected final Operator transformOperator = new com.affymetrix.genometryImpl.operator.PowerTransformer(0.5);

	public IndexedSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super();
		this.defaultGlyphFactory = defaultGlyphFactory;
		this.graphGlyphFactory = graphGlyphFactory;
	}

	public abstract String getIndexedFileName(String method, Direction direction);
	protected abstract FileTypeCategory getFileTypeCategory();

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

	protected AbstractGraphGlyph getEmptyGraphGlyph(ITrackStyleExtended trackStyle, SeqMapViewExtendedI gviewer) {
		GraphSym graf = new GraphSym(new int[]{gviewer.getVisibleSpan().getMin()}, new int[]{gviewer.getVisibleSpan().getLength()}, 
				new float[]{0}, trackStyle.getMethodName(), gviewer.getVisibleSpan().getBioSeq());
		return (AbstractGraphGlyph)graphGlyphFactory.getViewModeGlyph(graf, trackStyle, Direction.BOTH, gviewer);
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym,
			ITrackStyleExtended style, Direction direction,
			SeqMapViewExtendedI smv) {
		SemanticZoomGlyph szg = (SemanticZoomGlyph) super.getViewModeGlyph(sym, style, direction, smv);
		szg.setLastUsedGlyph(szg.getGlyph(smv));
		return szg;
	}
	// glyph class
	public abstract class IndexedSemanticZoomGlyph extends SemanticZoomGlyphFactory.SemanticZoomGlyph implements SeqSelectionListener {
		protected ViewModeGlyph defaultGlyph, saveDetailGlyph;
//		protected final SeqMapViewExtendedI smv;
		protected SymLoader detailSymL;
		protected SymLoader summarySymL;
//		protected SimpleSeqSpan saveSpan;
		CThreadWorker<Void, Void> worker ;
			
		public IndexedSemanticZoomGlyph(SeqSymmetry sym) {
			super(sym);
//			this.smv = smv;
//			saveSpan = null;
		}

		public abstract boolean isDetail(ViewI view);

		protected boolean isAutoLoadMode(){
			if(this.getAnnotStyle() == null)
				return false;
			
			if(this.getAnnotStyle().getFeature() == null)
				return false;
			
			if(this.getAnnotStyle().getFeature().getLoadStrategy() != LoadStrategy.AUTOLOAD)
				return false;
			
			return true;
		}
		
		@Override
		protected void init(SeqSymmetry sym, ITrackStyleExtended trackStyle,
			Direction direction, SeqMapViewExtendedI gviewer) {
			viewModeGlyphs = new HashMap<String, ViewModeGlyph>();
			defaultGlyph = getEmptyGraphGlyph(trackStyle, gviewer);
			saveDetailGlyph = defaultGlyphFactory.getViewModeGlyph(sym, trackStyle, Direction.BOTH, gviewer);
		}

		protected RootSeqSymmetry getRootSym() {
			return (RootSeqSymmetry)GenometryModel.getGenometryModel().getSelectedSeq().getAnnotation(style.getMethodName());
		}

		protected ViewModeGlyph getDetailGlyph(final SeqMapViewExtendedI smv) throws Exception {
			final SeqSpan span = smv.getVisibleSpan();
			if(worker != null && !worker.isCancelled() && !worker.isDone()){
				worker.cancelThread(true);
			}
			CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>("Loading details for " + style.getTrackName() + " region " + span.toString(), Thread.MIN_PRIORITY) {

				@Override
				protected Void runInBackground() {
					try {
						GenericFeature feature = style.getFeature();
						SeqSymmetry optimized_sym = feature.optimizeRequest(span);
						if (optimized_sym != null) {
							List<SeqSymmetry> syms = GeneralLoadUtils.loadFeaturesForSym(feature, optimized_sym);
							if (syms != null && !syms.isEmpty()) {
								TypeContainerAnnot detailSym = new TypeContainerAnnot(style.getMethodName());
								for (SeqSymmetry sym : syms) {
									detailSym.addChild(sym);
								}
								saveDetailGlyph.copyChildren(defaultGlyphFactory.getViewModeGlyph(detailSym, style, Direction.BOTH, smv));
								pack(smv.getSeqMap().getView());
							}
						}
					} catch (Exception ex) {
						Logger.getLogger(IndexedSemanticZoomGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
					}
					return null;
				}

				@Override
				protected void finished() {
					if (lastUsedGlyph == saveDetailGlyph) {
						smv.getSeqMap().updateWidget();
					}
				}
			};
			CThreadHolder.getInstance().execute(saveDetailGlyph, worker);
			this.worker = worker;
			return saveDetailGlyph;
		}

		protected ViewModeGlyph getSummaryGlyph(SeqMapViewExtendedI smv) throws Exception {
			ViewModeGlyph resultGlyph = null;
			List<? extends SeqSymmetry> symList = summarySymL.getRegion(smv.getVisibleSpan());
			if (symList.size() > 0) {
				GraphSym gsym = (GraphSym)symList.get(0);
				List<SeqSymmetry> operList = new ArrayList<SeqSymmetry>();
				operList.add(gsym);
				BioSeq aseq = GenometryModel.getGenometryModel().getSelectedSeq();
				GraphSym opersym = (GraphSym)transformOperator.operate(aseq, operList);
				Color halfwayColor = new Color((style.getBackground().getRed() + style.getForeground().getRed()) / 2, (style.getBackground().getGreen() + style.getForeground().getGreen()) / 2, (style.getBackground().getBlue() + style.getForeground().getBlue()) / 2);
				opersym.getGraphState().setHeatMap(HeatMap.makeLinearHeatmap(gsym.getID(), style.getBackground(), halfwayColor));
				resultGlyph = graphGlyphFactory.getViewModeGlyph(opersym, style, Direction.BOTH, smv);
			}
			if (resultGlyph != null) {
				((AbstractGraphGlyph)resultGlyph).drawHandle(false);
				resultGlyph.setCoords(resultGlyph.getCoordBox().x, resultGlyph.getCoordBox().y, resultGlyph.getCoordBox().width, style.getMaxDepth() * style.getHeight());
			}
			return resultGlyph;
		}

		@Override
		public ViewModeGlyph getGlyph(SeqMapViewExtendedI smv) {
//			BioSeq seq = smv.getAnnotatedSeq();
			ViewI view = smv.getSeqMap().getView();
	        int startBase = (int)Math.round(view.getCoordBox().getX());
			int length = (int)Math.round(view.getCoordBox().getWidth());
//	        int endBase = startBase + length;
//	        SimpleSeqSpan span = new SimpleSeqSpan(startBase, endBase, seq);
//	        if (span.equals(saveSpan) && lastUsedGlyph != null) {
//	        	return lastUsedGlyph;
//	        }
			try {
				ViewModeGlyph resultGlyph = null;
				if (isAutoLoadMode() && isDetail(view)) {
					resultGlyph = getDetailGlyph(smv);
				}
				else {
					resultGlyph = getSummaryGlyph(smv);
				}
				if (resultGlyph == null) {
					resultGlyph = getEmptyGraphGlyph(style, smv);
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
				if (resultGlyph.getScene() != null) {
					resultGlyph.pack(view);
				}
				if (resultGlyph.getCoordBox().y != saveY) {
					resultGlyph.moveAbsolute(resultGlyph.getCoordBox().x, saveY);
				}
//				saveSpan = span;
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
		public void processParentCoordBox(java.awt.geom.Rectangle2D.Double parentCoordBox) {
			super.processParentCoordBox(parentCoordBox);
			if (defaultGlyph != null) {
				defaultGlyph.setCoordBox(parentCoordBox);
			}
		}

		@Override
		public Object getInfo() {
			RootSeqSymmetry rootSym = getRootSym();
			if (rootSym == null) {
				rootSym = new RootSeqSymmetry() { // so that it is not null
					@Override public FileTypeCategory getCategory() { return getFileTypeCategory(); }
				};
			};
			return rootSym;
		}
		
		@Override
		public void seqSelectionChanged(SeqSelectionEvent evt) {
			saveDetailGlyph = null;
		}
	}
	// end glyph class
}
