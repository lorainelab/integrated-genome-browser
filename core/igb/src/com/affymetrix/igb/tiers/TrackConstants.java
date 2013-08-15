package com.affymetrix.igb.tiers;

import java.awt.Color;
import java.util.regex.Pattern;

/**
 *
 * @author hiralv
 */
public interface TrackConstants {

	// A pattern that matches two or more slash "/" characters.
	// A preference node name can't contain two slashes, nor end with a slash.
	static final Pattern multiple_slashes = Pattern.compile("/{2,}");
	static final String NAME_OF_DEFAULT_INSTANCE = "* Default *";
	static final String NAME_OF_COORDINATE_INSTANCE = "Coordinates";
	static final String NO_LABEL = "* none *";
	// The String constants named PREF_* are for use in the persistent preferences
	// They are not displayed to users, and should never change
	static final String PREF_CONNECTED = "Connected";
	static final String PREF_GLYPH_DEPTH = "Glyph Depth";
	static final String PREF_COLLAPSED = "Collapsed";
	static final String PREF_MAX_DEPTH = "Max Depth";
	static final String PREF_FOREGROUND = "Foreground";
	static final String PREF_BACKGROUND = "Background";
	static final String PREF_START_COLOR = "Start Color";
	static final String PREF_END_COLOR = "End Color";
	static final String PREF_TRACK_NAME = "Track Name";
	static final String PREF_LABEL_FIELD = "Label Field";
	static final String PREF_SHOW2TRACKS = "Show 2 Tracks";
	static final String PREF_HEIGHT = "Height"; // height per glyph? // linear transform value?
	static final String PREF_TRACK_SIZE = "Track Name Size";
	static final String PREF_DIRECTION_TYPE = "Direction Type";
	static final String PREF_VIEW_MODE = "View Mode";
	static final String PREF_DRAW_COLLAPSE_ICON = "Draw Collapse Icon";
	static final String PREF_SHOW_IGB_TRACK_MARK = "Show IGB Track Mark";
	static final String PREF_SHOW_LOCKED_TRACK_ICON = "Show Locked Track Icon";
	static final String PREF_SHOW_RESIDUE_MASK = "Show Residue Mask";
	static final String PREF_SHADE_BASED_ON_QUALITY_SCORE = "Shade Based On Quality Score";
	
	static final boolean default_show = true;
	static final boolean default_connected = true;
	static final boolean default_collapsed = false;
	static final boolean default_show2tracks = true;
	static final boolean default_expandable = true;
	static final boolean default_show_summary = false;
	static final boolean default_draw_collapse_icon = true;
	static final boolean default_show_igb_track_mark = true;
	static final boolean default_show_locked_track_icon = true;
	static final boolean default_color_by_score = false;
	static final boolean default_color_by_rgb = false;
	static final boolean default_showResidueMask = true;
	static final boolean default_shadeBasedOnQualityScore = true;
	static final int default_max_depth = 10;
	static final Color default_foreground = Color.decode("0x333399"); //Color.CYAN;
	static final Color default_background = Color.decode("0xDEE0E0"); //Color.BLACK;
	static final Color default_start = new Color(204, 255, 255);
	static final Color default_end = new Color(51, 255, 255);
	static final String default_label_field = "";
	static final int default_glyphDepth = 2;
	static final double default_height = 50.0;//25.0;
	static final double default_y = 0.0;
	static final float default_track_name_size = 12;
	static final float default_min_score_color = 1.0f;
	static final float default_max_score_color = 1000.f;
	static final DIRECTION_TYPE default_direction_type = DIRECTION_TYPE.NONE;
	public static final Object[] SUPPORTED_SIZE = {8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f, 18.0f, 19.0f, 20.0f};
	public static final Object[] LABELFIELD = {NO_LABEL, "id", "name", "score"};

	public static enum DIRECTION_TYPE {

		NONE,
		ARROW,
		COLOR,
		BOTH;

		public static DIRECTION_TYPE valueFor(String string) {
			for (DIRECTION_TYPE type : DIRECTION_TYPE.values()) {
				if (type.name().equalsIgnoreCase(string)) {
					return type;
				}
			}
			return default_direction_type;
		}

		public static DIRECTION_TYPE valueFor(int i) {
			if (i < DIRECTION_TYPE.values().length) {
				return DIRECTION_TYPE.values()[i];
			}
			return default_direction_type;
		}
	}
}
