
package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.operator.DepthOperator;
import com.affymetrix.genometryImpl.operator.LogTransform;
import com.affymetrix.genometryImpl.operator.NotOperator;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.tiers.TrackConstants;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All implementation of map view mode are stored here.
 * @author hiralv
 */
public class MapViewModeHolder {
	
	java.util.LinkedHashMap<String, MapViewGlyphFactoryI> view2Factory = new java.util.LinkedHashMap<String, MapViewGlyphFactoryI>();
	java.util.LinkedHashMap<String, Operator> transform2Operator = new java.util.LinkedHashMap<String, Operator>();
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
		
		
		// Adding operators
		addOperator(new NotOperator());
		addOperator(new LogTransform(2.0));
		
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
	
	public Object[] getAllViewModesFor(String file_format) {
		java.util.List<Object> mode = new java.util.ArrayList<Object>(view2Factory.size());
		
		if (file_format != null) {
			mode.add(TrackConstants.default_view_mode);
			for (java.util.Map.Entry<String, MapViewGlyphFactoryI> entry : view2Factory.entrySet()) {
				MapViewGlyphFactoryI emv = entry.getValue();
				if (emv.isFileSupported(file_format)) {
					mode.add(entry.getKey());
				}
				
			}
		}
		
		return mode.toArray(new Object[0]);
		
	}
	
	public Operator getOperator(String transform){
		if(transform == null){
			return null;
		}
		return transform2Operator.get(transform);
	}
		
	public final void addOperator(Operator operator){
		if(operator == null){
			Logger.getLogger(MapViewModeHolder.class.getName()).log(Level.WARNING, "Trying to add null operator");
			return;
		}
		String transform = operator.getName();
		if(transform2Operator.get(transform) != null){
			Logger.getLogger(MapViewModeHolder.class.getName()).log(Level.WARNING, "Trying to add duplicate operator for {0}", transform);
			return;
		}
		transform2Operator.put(transform, operator);
	}
	
	public final void removeOperator(Operator operator){
		transform2Operator.remove(operator.getName());
	}
	
	public Object[] getAllTransformFor(String file_format) {
		java.util.List<Object> mode = new java.util.ArrayList<Object>(transform2Operator.size());
		FileTypeHandler handler = FileTypeHolder.getInstance().getFileTypeHandler(file_format);
		if(handler == null){
			return mode.toArray(new Object[0]);
		}
		
		FileTypeCategory file_category = handler.getFileTypeCategory();
		if (file_category != null) {
			mode.add(TrackConstants.default_operator);
			for (java.util.Map.Entry<String, Operator> entry : transform2Operator.entrySet()) {
				Operator emv = entry.getValue();
				if (emv.getOperandCountMax(file_category) == 1 &&
						emv.getOperandCountMin(file_category) == 1) {
					mode.add(entry.getKey());
				}
			}
		}
		
		return mode.toArray(new Object[0]);
	}
}
