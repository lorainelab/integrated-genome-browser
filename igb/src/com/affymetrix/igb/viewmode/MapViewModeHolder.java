
package com.affymetrix.igb.viewmode;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;

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

	public MapViewGlyphFactoryI getDefaultFactory() {
		return getDefaultFactoryFor(FileTypeCategory.Annotation);
	}

	public MapViewGlyphFactoryI getDefaultFactoryFor(FileTypeCategory category) {
		if(defaultView.get(category) != null)
			return defaultView.get(category);

		return defaultView.get(FileTypeCategory.Annotation);
	}
}
