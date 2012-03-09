package com.affymetrix.igb.viewmode;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.shared.AbstractViewModeGlyph;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.StyleGlyphI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class UnloadedGlyphFactory implements MapViewGlyphFactoryI {
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}
	private static final UnloadedGlyphFactory instance = new UnloadedGlyphFactory();
	public static UnloadedGlyphFactory getInstance() {
		return instance;
	}

	// glyph class
	private class UnloadedGlyph extends AbstractViewModeGlyph implements StyleGlyphI {

		public UnloadedGlyph(BioSeq seq, SeqSymmetry sym, int slots, double height, ITrackStyleExtended style, Direction tier_direction) {
			super();
			setStyle(style);
			this.setPacker(new FasterExpandPacker());
			this.setDirection(tier_direction);
			if (getChildCount() <= 0) {
				Glyph glyph;

				for (int i = 0; i < slots; i++) {
					// Add empty child.
					glyph = new Glyph() {
					};
					glyph.setCoords(0, 0, 0, height);
					addChild(glyph);
				}
				
				if(style.getFeature() != null){
					sym = style.getFeature().getRequestSym();
				}
				
				// Add middle glyphs.
				SeqSymmetry inverse = SeqUtils.inverse(sym, seq);
				int child_count = inverse.getChildCount();
				//If any request was made.
				if (child_count > 0) {
					for (int i = 0; i < child_count; i++) {
						SeqSymmetry child = inverse.getChild(i);
						for (int j = 0; j < child.getSpanCount(); j++) {
							SeqSpan ospan = child.getSpan(j);
							if (ospan.getLength() > 1) {
								glyph = new FillRectGlyph();
								glyph.setCoords(ospan.getMin(), 0, ospan.getLength() - 1, 0);
								addMiddleGlyph(glyph);
							}
						}
					}
				} else {
					glyph = new FillRectGlyph();
					glyph.setCoords(seq.getMin(), 0, seq.getLength() - 1, 0);
					addMiddleGlyph(glyph);
				}
			}
		}

		@Override
		public void setPreferredHeight(double height, ViewI view) {
		}

		@Override
		public int getActualSlots() {
			return 1;
		}

		// overriding pack to ensure that tier is always the full width of the scene
		@Override
		public void pack(ViewI view, boolean manual) {
			super.pack(view, manual);
			Rectangle2D.Double mbox = getScene().getCoordBox();
			Rectangle2D.Double cbox = this.getCoordBox();
			this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
		}

		@Override
		public void draw(ViewI view) {
			drawMiddle(view);
			super.draw(view);
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

	private UnloadedGlyphFactory() {
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
		BioSeq seq = smv.getAnnotatedSeq();
		SeqSymmetry useSym = new SimpleMutableSeqSymmetry();
		int slots = smv.getAverageSlots();
		double height = style.getHeight();
		if(!style.isGraphTier()){
			height = style.getLabelField() == null || style.getLabelField().isEmpty() ? height : height * 2;
		}
		return new UnloadedGlyph(seq, useSym, slots, height, style, tier_direction);
	}

	@Override
	public String getName() {
		return "unloaded";
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		//TODO: Fix this for cytobands
		if (category == FileTypeCategory.Sequence){
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isURISupported(String uri) {
		return true;
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return true;
	}
}
