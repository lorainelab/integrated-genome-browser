package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;

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
	};
	public ITrackStyleExtended getAnnotStyle();
	public FileTypeCategory getFileTypeCategory();
	public Direction getDirection();
}
