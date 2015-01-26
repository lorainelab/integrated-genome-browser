package com.affymetrix.igb.tiers;

import com.affymetrix.genometry.util.PreferenceUtils;
import java.awt.Color;

public final class CoordinateStyle {

    /**
     * One of the acceptable values of {@link #PREF_COORDINATE_LABEL_FORMAT},
     * {@link #PREF_COORDINATE_LABEL_FORMAT}.
     */
    public static final String VALUE_COORDINATE_LABEL_FORMAT_FULL = "FULL";
    /**
     * One of the acceptable values of {@link #PREF_COORDINATE_LABEL_FORMAT}
     * {@link #PREF_COORDINATE_LABEL_FORMAT}.
     */
    public static final String VALUE_COORDINATE_LABEL_FORMAT_COMMA = "COMMA";
    /**
     * One of the acceptable values of {@link #PREF_COORDINATE_LABEL_FORMAT},
     * {@link #PREF_COORDINATE_LABEL_FORMAT}.
     */
    public static final String VALUE_COORDINATE_LABEL_FORMAT_ABBREV = "ABBREV";
    public static final String PREF_COORDINATE_LABEL_FORMAT = "Coordinate label format";
    /**
     * One of the acceptable values of {@link #PREF_COORDINATE_LABEL_FORMAT},
     * {@link #PREF_COORDINATE_LABEL_FORMAT}.
     */
    public static final String VALUE_COORDINATE_LABEL_FORMAT_NO_LABELS = "NO_LABELS";

    public static final String PREF_COORDINATE_COLOR = "Coordinate color";
    public static final String PREF_COORDINATE_BACKGROUND = "Coordinate background";
    public static final String PREF_COORDINATE_NAME = "Coordinate name";
    public static final Color default_coordinate_color = Color.BLACK;
    public static final Color default_coordinate_background = Color.WHITE;

    /**
     * An un-collapsible, but hideable, instance.
     */
    public static final TrackStyle coordinate_annot_style = new TrackStyle() {
        private Color foreground, background;

        { // a non-static initializer block
            setTrackName("Coordinates");
            foreground = PreferenceUtils.getColor(PREF_COORDINATE_COLOR, default_coordinate_color);
            background = PreferenceUtils.getColor(PREF_COORDINATE_BACKGROUND, default_coordinate_background);
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

        @Override
        public void restoreToDefault() {
            setForeground(default_coordinate_color);
            setBackground(default_coordinate_background);
            PreferenceUtils.save(PreferenceUtils.getTopNode(), CoordinateStyle.PREF_COORDINATE_LABEL_FORMAT, CoordinateStyle.VALUE_COORDINATE_LABEL_FORMAT_COMMA);
        }
    };
}
