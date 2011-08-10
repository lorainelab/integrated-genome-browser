
package com.affymetrix.igb.glyph;

import com.affymetrix.igb.shared.ExtendedMapViewGlyphFactoryI;
import com.affymetrix.igb.tiers.TrackConstants;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All implementation of map view mode are stored here.
 * @author hiralv
 */
public class MapViewModeHolder {
	
	java.util.LinkedHashMap<String, ExtendedMapViewGlyphFactoryI> view2Factory = new java.util.LinkedHashMap<String, ExtendedMapViewGlyphFactoryI>();
	private static final MapViewModeHolder instance = new MapViewModeHolder();
	
	public static MapViewModeHolder getInstance(){
		return instance;
	}
	
	private MapViewModeHolder(){
		addViewFactory(new DepthGraphGlyphFactory());
		addViewFactory(new CoverageSummarizerFactory());
		addViewFactory(new MismatchGraphGlyphFactory(false));
		addViewFactory(new MismatchGraphGlyphFactory(true));
	}
	
	public MapViewGlyphFactoryI getViewFactory(String view){
		if(view == null){
			return null;
		}
		return view2Factory.get(view);
	}
	
	public void addViewFactory(ExtendedMapViewGlyphFactoryI factory){
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
	
	public void removeViewFactory(ExtendedMapViewGlyphFactoryI factory){
		view2Factory.remove(factory.getName());
	}
	
	public Object[] getAllViewModes(){
		return view2Factory.keySet().toArray();
	}
}
