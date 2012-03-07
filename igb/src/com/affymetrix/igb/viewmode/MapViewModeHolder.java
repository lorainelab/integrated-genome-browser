
package com.affymetrix.igb.viewmode;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.operator.DepthOperator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.view.MismatchOperator;
import com.affymetrix.igb.view.MismatchPileupOperator;

/**
 * All implementation of map view mode are stored here.
 * @author hiralv
 */
public class MapViewModeHolder {
	
	java.util.LinkedHashMap<String, MapViewGlyphFactoryI> view2Factory = new java.util.LinkedHashMap<String, MapViewGlyphFactoryI>();
	java.util.LinkedHashMap<FileTypeCategory, MapViewGlyphFactoryI> defaultView = new java.util.LinkedHashMap<FileTypeCategory, MapViewGlyphFactoryI>();
	private static final MapViewModeHolder instance = new MapViewModeHolder();
	
	public static MapViewModeHolder getInstance(){
		return instance;
	}

	private MapViewModeHolder(){
		SeqMapViewExtendedI seqMapView = IGB.getSingleton().getMapView();
		
		// Add annot factories
		AnnotationGlyphFactory annotationGlyphFactory = new AnnotationGlyphFactory(FileTypeCategory.Annotation);
		annotationGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(annotationGlyphFactory);
		AnnotationGlyphFactory alignmentGlyphFactory = new AnnotationGlyphFactory(FileTypeCategory.Alignment);
		alignmentGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(alignmentGlyphFactory);

		// add sequence factory
		SequenceGlyphFactory sequenceGlyphFactory = new SequenceGlyphFactory();
		sequenceGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(sequenceGlyphFactory);

		// Add graph factories
		GraphGlyphFactory barGraphGlyphFactory = new GraphGlyphFactory(BarGraphGlyph.class);
		barGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(barGraphGlyphFactory);
		GraphGlyphFactory dotGraphGlyphFactory = new GraphGlyphFactory(DotGraphGlyph.class);
		dotGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(dotGraphGlyphFactory);
		GraphGlyphFactory fillBarGraphGlyphFactory = new GraphGlyphFactory(FillBarGraphGlyph.class);
		fillBarGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(fillBarGraphGlyphFactory);
		GraphGlyphFactory heatMapGraphGlyphFactory = new GraphGlyphFactory(HeatMapGraphGlyph.class);
		heatMapGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(heatMapGraphGlyphFactory);
		GraphGlyphFactory lineGraphGlyphFactory = new GraphGlyphFactory(LineGraphGlyph.class);
		lineGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(lineGraphGlyphFactory);
		GraphGlyphFactory minMaxAvgGraphGlyphFactory = new GraphGlyphFactory(MinMaxAvgGraphGlyph.class);
		addViewFactory(minMaxAvgGraphGlyphFactory);
		minMaxAvgGraphGlyphFactory.setSeqMapView(seqMapView);
		GraphGlyphFactory stairStepGraphGlyphFactory = new GraphGlyphFactory(StairStepGraphGlyph.class);
		stairStepGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(stairStepGraphGlyphFactory);
		
		// ProbeSet factory
		ProbeSetGlyphFactory probeSet = new ProbeSetGlyphFactory();
		probeSet.setSeqMapView(seqMapView);
		addViewFactory(probeSet);
		
		// Add ScoredContainer factories
		ScoredContainerGlyphFactory scoredBar = new ScoredContainerGlyphFactory(barGraphGlyphFactory);
		scoredBar.setSeqMapView(seqMapView);
		addViewFactory(scoredBar);
		ScoredContainerGlyphFactory scoredDot = new ScoredContainerGlyphFactory(dotGraphGlyphFactory);
		scoredDot.setSeqMapView(seqMapView);
		addViewFactory(scoredDot);
		ScoredContainerGlyphFactory scoredFillBar = new ScoredContainerGlyphFactory(fillBarGraphGlyphFactory);
		scoredFillBar.setSeqMapView(seqMapView);
		addViewFactory(scoredFillBar);
		ScoredContainerGlyphFactory scoredHeatMap = new ScoredContainerGlyphFactory(heatMapGraphGlyphFactory);
		scoredHeatMap.setSeqMapView(seqMapView);
		addViewFactory(scoredHeatMap);
		ScoredContainerGlyphFactory scoredLine = new ScoredContainerGlyphFactory(lineGraphGlyphFactory);
		scoredLine.setSeqMapView(seqMapView);
		addViewFactory(scoredLine);
		ScoredContainerGlyphFactory scoredMinMaxAvg = new ScoredContainerGlyphFactory(minMaxAvgGraphGlyphFactory);
		scoredMinMaxAvg.setSeqMapView(seqMapView);
		addViewFactory(scoredMinMaxAvg);
		ScoredContainerGlyphFactory scoredStairStep = new ScoredContainerGlyphFactory(stairStepGraphGlyphFactory);
		scoredStairStep.setSeqMapView(seqMapView);
		addViewFactory(scoredStairStep);
		
		// Add mismatch factories
		MismatchGlyphFactory mismatchGlyphFactory = new MismatchGlyphFactory();
		mismatchGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(mismatchGlyphFactory);

		MapViewGlyphFactoryI mismatchFactory = new OperatorGlyphFactory(new MismatchOperator(), mismatchGlyphFactory);
		addViewFactory(mismatchFactory);
		MapViewGlyphFactoryI mismatchPileupFactory = new OperatorGlyphFactory(new MismatchPileupOperator(), mismatchGlyphFactory);
		addViewFactory(mismatchPileupFactory);
		
		// Add Default factories
		addDefaultFactory(FileTypeCategory.Annotation, annotationGlyphFactory);
		addDefaultFactory(FileTypeCategory.Alignment, alignmentGlyphFactory);
		addDefaultFactory(FileTypeCategory.Sequence, sequenceGlyphFactory);
		addDefaultFactory(FileTypeCategory.Graph, stairStepGraphGlyphFactory);
		addDefaultFactory(FileTypeCategory.Mismatch, mismatchGlyphFactory);
		addDefaultFactory(FileTypeCategory.ProbeSet, probeSet);
		addDefaultFactory(FileTypeCategory.ScoredContainer, scoredStairStep);
//		addViewFactory(new OperatorGlyphFactory(new LogTransform(Math.E), new GenericGraphGlyphFactory()));
//		ExpandedAnnotGlyphFactory expandedAnnotGlyphFactory = new ExpandedAnnotGlyphFactory();
//		expandedAnnotGlyphFactory.init(new HashMap<String, Object>());
//		addViewFactory(expandedAnnotGlyphFactory);
		MapViewGlyphFactoryI alignmentDepthFactory = new OperatorGlyphFactory(new DepthOperator(FileTypeCategory.Alignment), stairStepGraphGlyphFactory);
		addViewFactory(new DefaultSemanticZoomGlyphFactory(alignmentGlyphFactory, alignmentDepthFactory));
		MapViewGlyphFactoryI annotationDepthFactory = new OperatorGlyphFactory(new DepthOperator(FileTypeCategory.Annotation), stairStepGraphGlyphFactory);
		addViewFactory(new DefaultSemanticZoomGlyphFactory(annotationGlyphFactory, annotationDepthFactory));
		addViewFactory(new BigWigSemanticZoomGlyphFactory(annotationGlyphFactory));
		addViewFactory(new BigWigSemanticZoomGlyphFactory(alignmentGlyphFactory));
	}

	public MapViewGlyphFactoryI getViewFactory(String view){
		if(view == null){
			return null;
		}
		return view2Factory.get(view);
	}
	
	public final void addViewFactory(MapViewGlyphFactoryI factory){
		if(factory == null){
			Logger.getLogger(MapViewModeHolder.class.getName()).log(Level.WARNING, "Trying to add null factory");
			return;
		}
		String view = factory.getName();
		if(view2Factory.get(view) != null){
			Logger.getLogger(MapViewModeHolder.class.getName()).log(Level.WARNING, "Trying to add duplicate factory for {0}", view);
			return;
		}
		view2Factory.put(view, factory);
	}
	
	public final void removeViewFactory(MapViewGlyphFactoryI factory){
		view2Factory.remove(factory.getName());
	}
	
	public final void addDefaultFactory(FileTypeCategory category, MapViewGlyphFactoryI factory){
		if(factory == null){
			Logger.getLogger(MapViewModeHolder.class.getName()).log(Level.WARNING, "Trying to add null factory for default view");
			return;
		}
		
		if(defaultView.get(category) != null){
			Logger.getLogger(MapViewModeHolder.class.getName()).log(Level.WARNING, "Trying to add duplicate factory for {0}", category);
			return;
		}
		defaultView.put(category, factory);
	}
		
	public Object[] getAllViewModesFor(FileTypeCategory category, String uri) {
		java.util.List<Object> mode = new java.util.ArrayList<Object>(view2Factory.size());

		for (java.util.Map.Entry<String, MapViewGlyphFactoryI> entry : view2Factory.entrySet()) {
			MapViewGlyphFactoryI emv = entry.getValue();
			if (emv.isCategorySupported(category) && emv.isURISupported(uri)) {
				mode.add(entry.getKey());
			}

		}

		return mode.toArray(new Object[0]);

	}
	
	public MapViewGlyphFactoryI getDefaultFactoryFor(FileTypeCategory category) {
		if(defaultView.get(category) != null)
			return defaultView.get(category);
		
		return defaultView.get(FileTypeCategory.Annotation);
	}
}
