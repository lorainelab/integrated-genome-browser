package com.affymetrix.igb.glyph;

import java.awt.geom.Rectangle2D;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.StyleGlyphI;

public class UnloadedGlyph extends AbstractViewModeGlyph implements StyleGlyphI {

	public UnloadedGlyph(BioSeq seq, SeqSymmetry sym, int slots, double height, ITrackStyleExtended style) {
		super();
		setStyle(style);
		this.setPacker(new FasterExpandPacker());
		if (getChildCount() <= 0) {
			Glyph glyph;

			for (int i = 0; i < slots; i++) {
				// Add empty child.
				glyph = new Glyph() {
				};
				glyph.setCoords(0, 0, 0, height);
				addChild(glyph);
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
	public String getViewMode() {
		return "unloaded";
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
}
