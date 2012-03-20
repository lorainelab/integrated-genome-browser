package com.affymetrix.igb.viewmode;

import java.awt.Graphics;
import java.awt.Point;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.CollapsePacker;
import com.affymetrix.igb.shared.GraphFasterExpandPacker;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

/**
 * creates a glyph that contains other glyphs, a result of the Join action
 */
public class ComboGlyphFactory implements MapViewGlyphFactoryI {
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}
	private static final ComboGlyphFactory instance = new ComboGlyphFactory();
	public static ComboGlyphFactory getInstance() {
		return instance;
	}

	// glyph class
	public class ComboGlyph extends AbstractGraphGlyph {
		private GraphFasterExpandPacker expand_packer = new GraphFasterExpandPacker();
		private CollapsePacker collapse_packer = new CollapsePacker();
		public ComboGlyph(SeqMapViewExtendedI smv, ITrackStyleExtended style) {
			super(new GraphState(style));
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
		public void drawMiddle(ViewI view) {
			if (getChildren() != null)  {
				ViewModeGlyph child;
				int numChildren = getChildren().size();
				for ( int i = 0; i < numChildren; i++ ) {
					child = (ViewModeGlyph)getChildren().get( i );
					child.drawMiddle(view);
				}
			}
		}

		@Override
		public void addChild(GlyphI glyph) {
			double height = getCoordBox().getHeight() + glyph.getCoordBox().getHeight();
			getCoordBox().setRect(getCoordBox().getX(), getCoordBox().getY(), getCoordBox().getWidth(), height);
			super.addChild(glyph);
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

		@Override
		public boolean isCombo() {
			return true;
		}

		@Override
		public void addSym(SeqSymmetry sym) {
		}

		@Override
		public String getName() {
			return "combo";
		}

		@Override
		protected void doBigDraw(Graphics g, GraphSym graphSym,
				Point curr_x_plus_width, Point max_x_plus_width, float ytemp,
				int draw_end_index, int i) {
		}

		@Override
		public GraphType getGraphStyle() {
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
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style,
		Direction tier_direction, SeqMapViewExtendedI smv) {
		ComboGlyph comboGlyph = new ComboGlyph(smv, style);
		return comboGlyph;
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
	public boolean isURISupported(String uri) {
		return true;
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return false;
	}
}
