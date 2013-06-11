package com.affymetrix.igb.tiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

public final class IGBStateProvider extends DefaultStateProvider {

	private static Preferences tiers_root_node = PreferenceUtils.getTopNode().node("tiers");
	private static boolean showIGBTracks = tiers_root_node.getBoolean(TrackConstants.PREF_SHOW_IGB_TRACK_MARK, TrackConstants.default_show_igb_track_mark);
	private static boolean showLockIcon = tiers_root_node.getBoolean(TrackConstants.PREF_SHOW_LOCKED_TRACK_ICON, TrackConstants.default_show_locked_track_icon);
	private static boolean draw_collapse_icon = tiers_root_node.getBoolean(TrackConstants.PREF_DRAW_COLLAPSE_ICON, TrackConstants.default_draw_collapse_icon);
	private static final Map<String, TrackStyle> static_map = new LinkedHashMap<String, TrackStyle>();
	private static TrackStyle default_instance = null;

	@Override
	public GraphState getGraphState(String id, String human_name, String extension, java.util.Map<String, String> props) {
		if (human_name == null) {
			String unzippedName = GeneralUtils.getUnzippedName(id);
			human_name = unzippedName.substring(unzippedName.lastIndexOf("/") + 1);
		}
		return super.getGraphState(id, human_name, extension, props);
	}

	@Override
	public void removeAnnotStyle(String name) {
		removeInstance(name);
	}

	@Override
	public ITrackStyleExtended getAnnotStyle(String name) {
		return getInstance(name);
	}

	@Override
	public ITrackStyleExtended getAnnotStyle(String name, String human_name, String file_type, java.util.Map<String, String> props) {
		return getInstance(name, human_name, file_type, props);
	}

	/**
	 * Returns all (persistent and temporary) instances of AnnotStyle.
	 */
	public static List<TrackStyle> getAllLoadedInstances() {
		return new ArrayList<TrackStyle>(static_map.values());
	}

	public static void removeInstance(String unique_name) {
		TrackStyle style = static_map.get(unique_name.toLowerCase());
		if (style != null) {
			style.setShow(true);
			static_map.remove(unique_name.toLowerCase());
		}
	}

	public static TrackStyle getInstance(String unique_name, String track_name, String file_type, Map<String, String> props) {
		TrackStyle style = static_map.get(unique_name.toLowerCase());
		if (style == null) {
			if (TrackStyle.DEBUG) {
				System.out.println("    (((((((   in AnnotStyle.getInstance() creating AnnotStyle for name: " + unique_name);
			}
			TrackStyle template = getDefaultInstance();
			style = new TrackStyle(unique_name, track_name, file_type, template, props);
			FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(file_type);
			if (fth != null && (fth.getFileTypeCategory() == FileTypeCategory.Graph || fth.getFileTypeCategory() == FileTypeCategory.Mismatch)) {
				style.setExpandable(false);
				style.setGraphTier(true);
			}
			static_map.put(unique_name.toLowerCase(), style);
		}
		return style;
	}

	public static TrackStyle getInstance(String unique_name) {
		return getInstance(unique_name, null, null, null);
	}

	/**
	 * If there is no AnnotStyle with the given name, just returns the given
	 * name; else modifies the name such that there are no instances that are
	 * currently using it.
	 */
	public static String getUniqueName(String name) {
		String result = name.toLowerCase();
		while (static_map.get(result) != null) {
			result = name.toLowerCase() + "." + System.currentTimeMillis();
		}
		return result;
	}

	public static TrackStyle getDefaultInstance() {
		if (default_instance == null) {
			TrackStyle instance = new TrackStyle(TrackConstants.NAME_OF_DEFAULT_INSTANCE, TrackConstants.NAME_OF_DEFAULT_INSTANCE, null, null, null);
			instance.setTrackName("");
			instance.setShow(true);
			default_instance = instance;
			static_map.put(default_instance.getUniqueName(), default_instance);
		}
		return default_instance;
	}
	
	public static void setShowIGBTrackMark(boolean b) {
		showIGBTracks = b;
		if (tiers_root_node != null) {
			tiers_root_node.putBoolean(TrackConstants.PREF_SHOW_IGB_TRACK_MARK, b);
		}
	}

	public static boolean getShowIGBTrackMarkState() {
		return showIGBTracks;
	}
	
	public static void setShowLockIcon(boolean b){
		showLockIcon = b;
		if (tiers_root_node != null) {
			tiers_root_node.putBoolean(TrackConstants.PREF_SHOW_LOCKED_TRACK_ICON, b);
		}
	}
	
	public static boolean getShowLockIcon() {
		return showLockIcon;
	}

	public static void setDrawCollapseControl(boolean b) {
		draw_collapse_icon = b;
		if (tiers_root_node != null) {
			tiers_root_node.putBoolean(TrackConstants.PREF_DRAW_COLLAPSE_ICON, b);
		}
	}
	
	public static boolean getDrawCollapseState() {
		return draw_collapse_icon;
	}

}
