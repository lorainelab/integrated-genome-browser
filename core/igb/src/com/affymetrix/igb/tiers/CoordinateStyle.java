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
		private Color foreground, background;
		
		{ // a non-static initializer block
			setTrackName("Coordinates");
			foreground = PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_COORDINATE_COLOR, default_coordinate_color);
			background = PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_COORDINATE_BACKGROUND, default_coordinate_background);
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
			foreground = c;
		}

		@Override
		public Color getForeground() {
			return foreground;
		}

		@Override
		public void setBackground(Color c) {
			PreferenceUtils.putColor(PreferenceUtils.getTopNode(), PREF_COORDINATE_BACKGROUND, c);
			background = c;
		}

		@Override
		public Color getBackground() {
			return background;
		}
		
		@Override
		public Color getLabelForeground() {
			return getForeground();
		}

		@Override
		public Color getLabelBackground() {
			return getBackground();
		}
	};
}
