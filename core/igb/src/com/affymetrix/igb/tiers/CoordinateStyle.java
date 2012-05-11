package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.awt.Color;

public final class CoordinateStyle {
	public static final String PREF_COORDINATE_COLOR = "Coordinate color";
	public static final String PREF_COORDINATE_BACKGROUND = "Coordinate background";
	public static final String PREF_COORDINATE_NAME = "Coordinate name";
	public static final Color default_coordinate_color = Color.BLACK;
	public static final Color default_coordinate_background = Color.WHITE;

	/** An un-collapsible, but hideable, instance. */
	public static final TrackStyle coordinate_annot_style = new TrackStyle() {

		{ // a non-static initializer block
			setTrackName("Coordinates");
		}

		@Override
		public boolean getSeparate() {
			return false;
		}

		@Override
		public boolean getCollapsed() {
			return false;
		}

		@Override
		public boolean getExpandable() {
			return false;
		}

		@Override
		public void setForeground(Color c) {
			PreferenceUtils.putColor(PreferenceUtils.getTopNode(), PREF_COORDINATE_COLOR, c);
		}

		@Override
		public Color getForeground() {
			return PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_COORDINATE_COLOR, default_coordinate_color);
		}

		@Override
		public void setBackground(Color c) {
			PreferenceUtils.putColor(PreferenceUtils.getTopNode(), PREF_COORDINATE_BACKGROUND, c);
		}

		@Override
		public Color getBackground() {
			return PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_COORDINATE_BACKGROUND, default_coordinate_background);
		}
	};
}
