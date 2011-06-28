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
	static final String NAME_OF_DEFAULT_INSTANCE = "* DEFAULT *";
	// The String constants named PREF_* are for use in the persistent preferences
	// They are not displayed to users, and should never change
	static final String PREF_SEPARATE = "Separate Tiers";
	static final String PREF_COLLAPSED = "Collapsed";
	static final String PREF_MAX_DEPTH = "Max Depth";
	static final String PREF_COLOR = "Color";
	static final String PREF_BACKGROUND = "Background";
	static final String PREF_HUMAN_NAME = "Human Name";
	static final String PREF_LABEL_FIELD = "Label Field";
	static final String PREF_GLYPH_DEPTH = "Glyph Depth";
	static final String PREF_HEIGHT = "Height"; // height per glyph? // linear transform value?
	static final String PREF_FONT_SIZE = "Font Size";
	static final String PREF_DIRECTION_TYPE = "Direction Type";
	static final boolean default_show = true;
	static final boolean default_separate = true;
	static final boolean default_collapsed = false;
	static final boolean default_expandable = true;
	static final int default_max_depth = 10;
	static final Color default_color = Color.CYAN;
	static final Color default_background = Color.BLACK;
	static final String default_label_field = "";
	static final int default_glyph_depth = 2;
	static final double default_height = 20.0;
	static final double default_y = 0.0;
	static final float default_font_size = 12;
	static final DIRECTION_TYPE default_direction_type = DIRECTION_TYPE.NONE;
	public static final Object[] SUPPORTED_SIZE = {8.0f, 10.0f, 12.0f, 14.0f, 16.0f, 18.0f, 20.0f};
	
	public static enum DIRECTION_TYPE{
		ARROW,
		COLOR,
		NONE;
		
		public static DIRECTION_TYPE valueFor(String string){
			for (DIRECTION_TYPE type : DIRECTION_TYPE.values()) {
				if(type.name().equalsIgnoreCase(string))
					return type;
			}
			return default_direction_type;
		}
		
		public static DIRECTION_TYPE valueFor(int i){
			if(i < DIRECTION_TYPE.values().length){
				return DIRECTION_TYPE.values()[i];
			}
			return default_direction_type;
		}
	}
	
}
