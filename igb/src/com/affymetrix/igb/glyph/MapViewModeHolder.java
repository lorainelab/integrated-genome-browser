
package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.tiers.TrackConstants;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		addViewFactory(new MismatchGraphGlyphFactory());
		addViewFactory(new MismatchPileupGraphGlyphFactory());
		ExpandedAnnotGlyphFactory expandedAnnotGlyphFactory = new ExpandedAnnotGlyphFactory();
		expandedAnnotGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(expandedAnnotGlyphFactory);
		CollapsedAnnotGlyphFactory collapsedAnnotGlyphFactory = new CollapsedAnnotGlyphFactory();
		collapsedAnnotGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(collapsedAnnotGlyphFactory);
		BarGraphGlyphFactory barGraphGlyphFactory = new BarGraphGlyphFactory();
		barGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(barGraphGlyphFactory);
		DotGraphGlyphFactory dotGraphGlyphFactory = new DotGraphGlyphFactory();
		dotGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(dotGraphGlyphFactory);
		FillBarGraphGlyphFactory fillBarGraphGlyphFactory = new FillBarGraphGlyphFactory();
		fillBarGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(fillBarGraphGlyphFactory);
		HeatMapGraphGlyphFactory heatMapGraphGlyphFactory = new HeatMapGraphGlyphFactory();
		heatMapGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(heatMapGraphGlyphFactory);
		LineGraphGlyphFactory lineGraphGlyphFactory = new LineGraphGlyphFactory();
		lineGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(lineGraphGlyphFactory);
		MinMaxAvgGraphGlyphFactory minMaxAvgGraphGlyphFactory = new MinMaxAvgGraphGlyphFactory();
		addViewFactory(minMaxAvgGraphGlyphFactory);
		minMaxAvgGraphGlyphFactory.setSeqMapView(seqMapView);
		StairStepGraphGlyphFactory stairStepGraphGlyphFactory = new StairStepGraphGlyphFactory();
		stairStepGraphGlyphFactory.setSeqMapView(seqMapView);
		addViewFactory(stairStepGraphGlyphFactory);
		
		// Add Default factories
		addDefaultFactory(FileTypeCategory.Annotation, expandedAnnotGlyphFactory);
		addDefaultFactory(FileTypeCategory.Graph, stairStepGraphGlyphFactory);
		
//		addViewFactory(new OperatorGlyphFactory(new LogTransform(Math.E), new GenericGraphGlyphFactory()));
//		ExpandedAnnotGlyphFactory expandedAnnotGlyphFactory = new ExpandedAnnotGlyphFactory();
//		expandedAnnotGlyphFactory.init(new HashMap<String, Object>());
//		addViewFactory(expandedAnnotGlyphFactory);

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
		
	public Object[] getAllViewModesFor(FileTypeCategory category) {
		java.util.List<Object> mode = new java.util.ArrayList<Object>(view2Factory.size());

		mode.add(TrackConstants.default_view_mode);
		for (java.util.Map.Entry<String, MapViewGlyphFactoryI> entry : view2Factory.entrySet()) {
			MapViewGlyphFactoryI emv = entry.getValue();
			if (emv.isFileSupported(category)) {
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
