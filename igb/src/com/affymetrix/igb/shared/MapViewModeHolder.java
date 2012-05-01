
package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.tiers.TrackConstants;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All implementation of map view mode are stored here.
 * @author hiralv
 */
public class MapViewModeHolder {

	java.util.LinkedHashMap<String, MapViewGlyphFactoryI> view2Factory = new java.util.LinkedHashMap<String, MapViewGlyphFactoryI>();
	EnumMap<FileTypeCategory, MapViewGlyphFactoryI> defaultView = new EnumMap<FileTypeCategory, MapViewGlyphFactoryI>(FileTypeCategory.class);
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

	public boolean styleSupportsTwoTrack(ITrackStyleExtended style) {
		String viewMode = style.getViewMode();
		if (viewMode.equals(TrackConstants.default_view_mode)) {
			viewMode = getDefaultFactoryFor(style.getFileTypeCategory()).getName();
		}
		MapViewGlyphFactoryI factory = getViewFactory(viewMode);
		if (factory == null) {
			return false;
		}
		else {
			return factory.supportsTwoTrack();
		}
	}

	public List<MapViewGlyphFactoryI> getAllViewModesFor(FileTypeCategory category, String uri) {
		List<MapViewGlyphFactoryI> modes = new ArrayList<MapViewGlyphFactoryI>(view2Factory.size());

		for (Entry<String, MapViewGlyphFactoryI> entry : view2Factory.entrySet()) {
			MapViewGlyphFactoryI emv = entry.getValue();
			if (emv.isCategorySupported(category) && emv.isURISupported(uri)) {
				modes.add(emv);
			}

		}

		return modes;
	}

	public MapViewGlyphFactoryI getDefaultFactory() {
		return getDefaultFactoryFor(FileTypeCategory.Annotation);
	}

	public MapViewGlyphFactoryI getDefaultFactoryFor(FileTypeCategory category) {
		if(defaultView.get(category) != null) {
			return defaultView.get(category);
		}
		return defaultView.get(FileTypeCategory.Annotation);
	}

	/**
	 * get the MapViewGlyphFactoryI to be used for a track when it is first
	 * selected (before Load Data), normally the UnloadedFactory (gray background).
	 * @param uri - the uri of the data source of the track
	 * @return the factory to use
	 */
	public MapViewGlyphFactoryI getAutoloadFactory(String uri) {
		FileTypeCategory category = null;
		FileTypeHandler handler = FileTypeHolder.getInstance().getFileTypeHandlerForURI(uri);
		if (handler != null) {
			category = handler.getFileTypeCategory();
		}
		if (category != null) {
			for (Entry<String, MapViewGlyphFactoryI> entry : view2Factory.entrySet()) {
				MapViewGlyphFactoryI emv = entry.getValue();
				if (emv.isCategorySupported(category) && emv.canAutoLoad(uri)) {
					return emv;
				}
			}
		}
		return MapViewModeHolder.getInstance().getDefaultFactoryFor(category);
	}
}
