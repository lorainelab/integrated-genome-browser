package com.affymetrix.igb.viewmode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.AbstractViewModeGlyph;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.StyleGlyphI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

/**
 * creates a glyph with no children, so that it is never displayed
 */
public class DummyGlyphFactory implements MapViewGlyphFactoryI {
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}
	private static final DummyGlyphFactory instance = new DummyGlyphFactory();
	public static DummyGlyphFactory getInstance() {
		return instance;
	}

	// glyph class
	private class DummyGlyph extends AbstractViewModeGlyph implements StyleGlyphI {

		public DummyGlyph(ITrackStyleExtended style, Direction tier_direction) {
			super();
			setStyle(style);
			this.setPacker(new FasterExpandPacker());
			this.setDirection(tier_direction);
		}

		@Override
		public void setPreferredHeight(double height, ViewI view) {
		}

		@Override
		public int getActualSlots() {
			return 0;
		}

		@Override
		public Map<String, Class<?>> getPreferences() {
			return PREFERENCES;
		}

		@Override
		public void setPreferences(Map<String, Object> preferences) {
		}
	}
	// end glyph class

	private DummyGlyphFactory() {
		super();
	}

	@Override
	public void init(Map<String, Object> options) {
	}

	@Override
	public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI smv) {
		// not implemented
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style,
		Direction tier_direction, SeqMapViewExtendedI smv) {
		return new DummyGlyph(style, tier_direction);
	}

	@Override
	public String getName() {
		return "dummy";
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		return false;
	}
	
	@Override
	public boolean isURISupported(String uri) {
		return false;
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return false;
	}
}
