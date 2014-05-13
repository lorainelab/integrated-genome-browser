
package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All implementation of map view mode are stored here.
 * @author hiralv
 */
public class MapTierTypeHolder {

	java.util.LinkedHashMap<String, MapTierGlyphFactoryI> view2Factory = new java.util.LinkedHashMap<String, MapTierGlyphFactoryI>();
	EnumMap<FileTypeCategory, MapTierGlyphFactoryI> defaultView = new EnumMap<FileTypeCategory, MapTierGlyphFactoryI>(FileTypeCategory.class);
	private static final MapTierTypeHolder instance = new MapTierTypeHolder();

	public static MapTierTypeHolder getInstance(){
		return instance;
	}

	private MapTierTypeHolder(){
	}

	public MapTierGlyphFactoryI getViewFactory(String view){
		if(view == null){
			return null;
		}
		return view2Factory.get(view);
	}

	public final void addViewFactory(MapTierGlyphFactoryI factory){
		if(factory == null){
			Logger.getLogger(MapTierTypeHolder.class.getName()).log(Level.WARNING, "Trying to add null factory");
			return;
		}
		String view = factory.getName();
		if(view2Factory.get(view) != null){
			Logger.getLogger(MapTierTypeHolder.class.getName()).log(Level.WARNING, "Trying to add duplicate factory for {0}", view);
			return;
		}
		view2Factory.put(view, factory);
	}

	public final void removeViewFactory(MapTierGlyphFactoryI factory){
		view2Factory.remove(factory.getName());
	}

	public final void addDefaultFactory(FileTypeCategory category, MapTierGlyphFactoryI factory){
		if(factory == null){
			Logger.getLogger(MapTierTypeHolder.class.getName()).log(Level.WARNING, "Trying to add null factory for default view");
			return;
		}

		if(defaultView.get(category) != null){
			Logger.getLogger(MapTierTypeHolder.class.getName()).log(Level.WARNING, "Trying to add duplicate factory for {0}", category);
			return;
		}
		defaultView.put(category, factory);
	}

	public boolean supportsTwoTrack(FileTypeCategory category) {
		MapTierGlyphFactoryI factory = getDefaultFactoryFor(category);
		if (factory == null) {
			return false;
		}
		else {
			return factory.supportsTwoTrack();
		}
	}

	public MapTierGlyphFactoryI getDefaultFactoryFor(FileTypeCategory category) {
		if(defaultView.get(category) != null) {
			return defaultView.get(category);
		}
		return defaultView.get(FileTypeCategory.Annotation);
	}
}
