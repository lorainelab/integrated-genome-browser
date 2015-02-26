package com.lorainelab.igb.genoviz.extensions;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import java.util.Optional;

/**
 *
 * @author hiralv
 */
public interface StyledGlyph extends GlyphI {

    public static enum Direction {

        FORWARD(" (+)"), NONE(""), REVERSE(" (-)"), BOTH(" (+|-)"), AXIS("");
        private final String display;

        private Direction(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    public ITrackStyleExtended getAnnotStyle();

    public Optional<FileTypeCategory> getFileTypeCategory();

    public Direction getDirection();
}
