
package com.affymetrix.igb.glyph;

import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.tiers.TrackConstants;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All implementation of map view mode are stored here.
 * @author hiralv
 */
public class MapViewModeHolder {
	
	java.util.LinkedHashMap<String, MapViewGlyphFactoryI> view2Factory = new java.util.LinkedHashMap<String, MapViewGlyphFactoryI>();
	private static final MapViewModeHolder instance = new MapViewModeHolder();
	
	public static MapViewModeHolder getInstance(){
		return instance;
	}
	
	private MapViewModeHolder(){
		addViewFactory(new DepthGraphGlyphFactory());
		addViewFactory(new MismatchGraphGlyphFactory());
		addViewFactory(new MismatchPileupGraphGlyphFactory());
		addViewFactory(new ExpandedAnnotGlyphFactory());
		addViewFactory(new CollapsedAnnotGlyphFactory());
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
}
