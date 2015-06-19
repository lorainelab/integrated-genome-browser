package com.affymetrix.igb.tiers;

import com.affymetrix.genometry.general.DataSetUtils;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.common.PreferenceUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IGBStateProvider extends DefaultStateProvider {

    private static final Logger logger = LoggerFactory.getLogger(IGBStateProvider.class);
    private static boolean draw_collapse_icon = PreferenceUtils.getTopNode().getBoolean(TrackConstants.PREF_DRAW_COLLAPSE_ICON, TrackConstants.default_draw_collapse_icon);
    private static boolean showIGBTracks = PreferenceUtils.getTopNode().getBoolean(TrackConstants.PREF_SHOW_IGB_TRACK_MARK, TrackConstants.default_show_igb_track_mark);
    private static boolean showFilterMark = PreferenceUtils.getTopNode().getBoolean(TrackConstants.PREF_SHOW_FILTER_MARK, TrackConstants.default_show_filter_mark);
    private static boolean showLockIcon = PreferenceUtils.getTopNode().getBoolean(TrackConstants.PREF_SHOW_LOCKED_TRACK_ICON, TrackConstants.default_show_locked_track_icon);

    private static boolean showFullFilePathInTrack = PreferenceUtils.getTopNode().getBoolean(TrackConstants.PREF_SHOW_FULL_FILE_PATH_IN_TRACK, TrackConstants.default_show_full_file_path_in_track);//TK
    private static final Map<String, TrackStyle> static_map = new LinkedHashMap<>();
    private static TrackStyle defaultTrackStyle = null;

    /**
     * Returns all (persistent and temporary) instances of AnnotStyle.
     */
    public static List<TrackStyle> getAllLoadedInstances() {
        return new ArrayList<>(static_map.values());
    }

    public static void removeInstance(String unique_name) {
        TrackStyle style = static_map.get(unique_name.toLowerCase());
        if (style != null) {
            style.setShow(true);
            static_map.remove(unique_name.toLowerCase());
        }
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

    public static TrackStyle getDefaultTrackStyle() {
        if (defaultTrackStyle == null) {
            TrackStyle instance = new TrackStyle(TrackConstants.NAME_OF_DEFAULT_INSTANCE, TrackConstants.NAME_OF_DEFAULT_INSTANCE, null, null, null);
            instance.setTrackName("");
            instance.setShow(true);
            defaultTrackStyle = instance;
            static_map.put(defaultTrackStyle.getUniqueName(), defaultTrackStyle);
        }
        return defaultTrackStyle;
    }

    public static void setShowIGBTrackMark(boolean b) {
        showIGBTracks = b;
    }

    public static boolean getShowIGBTrackMarkState() {
        return showIGBTracks;
    }

    public static void setShowFilterMark(boolean b) {
        showFilterMark = b;
    }

    public static boolean getShowFilterMarkState() {
        return showFilterMark;
    }

    //TK
    public static void setShowFullFilePathInTrackMark(boolean b) {
        showFullFilePathInTrack = b;
    }

    public static boolean getShowFullFilePathInTrackMark() {
        return showFullFilePathInTrack;
    }

    public static void setShowLockIcon(boolean b) {
        showLockIcon = b;
    }

    public static boolean getShowLockIcon() {
        return showLockIcon;
    }

    public static void setDrawCollapseControl(boolean b) {
        draw_collapse_icon = b;
    }

    public static boolean getDrawCollapseState() {
        return draw_collapse_icon;
    }

    @Override
    public GraphState getGraphState(String id, String human_name, String extension, java.util.Map<String, String> props) {
        if (human_name == null) {
            String unzippedName = GeneralUtils.getUnzippedName(id);
            human_name = unzippedName.substring(unzippedName.lastIndexOf('/') + 1);
        }
        return super.getGraphState(id, human_name, extension, props);
    }

    @Override
    public void removeAnnotStyle(String name) {
        removeInstance(name);
    }

    @Override
    public ITrackStyleExtended getAnnotStyle(String unique_name) {
        return getAnnotStyle(unique_name, null, null, null);
    }

    @Override
    public ITrackStyleExtended getAnnotStyle(String unique_name, String track_name, String file_type, java.util.Map<String, String> props) {
        TrackStyle style = static_map.get(unique_name.toLowerCase());
        if (style == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("    (((((((   in AnnotStyle.getInstance() creating AnnotStyle for name: " + unique_name);
            }
            TrackStyle template = getDefaultTrackStyle();

            if (!getShowFullFilePathInTrackMark()) {
                if (track_name != null) {
                    track_name = DataSetUtils.extractNameFromPath(track_name);
                }
            } else {
                track_name = unique_name;
            }

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

}
