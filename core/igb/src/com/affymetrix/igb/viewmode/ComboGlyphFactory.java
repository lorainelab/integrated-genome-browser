package com.affymetrix.igb.viewmode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.*;
import com.affymetrix.igb.shared.TierGlyph.Direction;

/**
 * creates a glyph that contains other glyphs, a result of the Join action
 */
public class ComboGlyphFactory extends MapViewGlyphFactoryA {
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}
	private static final ComboGlyphFactory instance = new ComboGlyphFactory();
	public static ComboGlyphFactory getInstance() {
		return instance;
	}

	@Override
	public TierGlyph createViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, Direction direction, SeqMapViewExtendedI gviewer) {
		//return new ComboGlyph(gviewer, style);
		return null;
	}

	// glyph class
	public class ComboGlyph extends MultiGraphGlyph {
		private GraphFasterExpandPacker expand_packer = new GraphFasterExpandPacker();
		private CollapsePacker collapse_packer = new CollapsePacker();
		public ComboGlyph(GraphGlyph graphGlyph) {
			super(graphGlyph);
			setStyle(style);
		}
	
		@Override
		public void setStyle(ITrackStyleExtended style) {
			super.setStyle(style);
			if (style.getCollapsed()) {
				setPacker(collapse_packer);
			} else {
				setPacker(expand_packer);
			}
		}
		
		@Override
		public void draw(ViewI view)  {}
		
		@Override
		public void addChild(GlyphI glyph) {
			double height = getCoordBox().getHeight() + glyph.getCoordBox().getHeight();
			getCoordBox().setRect(getCoordBox().getX(), getCoordBox().getY(), getCoordBox().getWidth(), height);
			super.addChild(glyph);
		}

		@Override
		public void removeChild(GlyphI glyph) {
			double height = getCoordBox().getHeight() - glyph.getCoordBox().getHeight();
			getCoordBox().setRect(getCoordBox().getX(), getCoordBox().getY(), getCoordBox().getWidth(), height);
			super.removeChild(glyph);
		}

		@Override
		public Map<String, Class<?>> getPreferences() {
			return PREFERENCES;
		}

		@Override
		public boolean isCombo() {
			return true;
		}

//		@Override
//		public String getName() {
//			return "combo";
//		}
//
//		@Override
//		public GraphType getGraphStyle() {
//			return null;
//		}

		public GlyphI getChildWithStyle(ITrackStyleExtended style) {
			int numChildren = getChildCount();
			for (int i = 0; i < numChildren; i++) {
				ViewModeGlyph glyph = (ViewModeGlyph)getChild(i);
				if (glyph.getAnnotStyle() == style) { // can use == instead of equals()
					return glyph;
				}
			}
			return null;
		}
	}
	// end glyph class

	private ComboGlyphFactory() {
		super();
	}

	@Override
	public void init(Map<String, Object> options) {
	}

	@Override
	public TierGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style,
		Direction tier_direction, SeqMapViewExtendedI smv) {
		//ComboGlyph comboGlyph = new ComboGlyph(smv, style);
		//comboGlyph.setCoords(0, style.getY(), smv.getAnnotatedSeq().getLength(), 0);
		//return comboGlyph;
		return null;
	}

	@Override
	public String getName() {
		return "combo";
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		return category == FileTypeCategory.Graph;
	}
	
	@Override
	public boolean canAutoLoad(String uri) {
		return false;
	}
}
