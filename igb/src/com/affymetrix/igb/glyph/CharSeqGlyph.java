package com.affymetrix.igb.glyph;

import java.awt.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

import com.affymetrix.genometryImpl.util.ImprovedStringCharIter;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.awt.geom.Rectangle2D;

/**
 * CharSeqGlyph differs from SequenceGlyph in that it can take either a 
 * String as residues (via sg.setResidues()) or a {@link CharIterator}
 * as a provider of residues (via sg.setResiduesProvider()) .
 * This allows one to glyphify large biological sequences while maintaining a more 
 * compressed representation of the sequences residues.
 *
 * A glyph that shows a sequence of residues.
 * At low resolution (small scale) as a solid background rectangle
 * and at high resolution overlays the residue letters.
 *
 */
public final class CharSeqGlyph extends SequenceGlyph
		 {
	SearchableCharIterator chariter;
	boolean residuesSet = false;
	int residue_length = 0;
	static Font mono_default_font = new Font("Monospaced", Font.BOLD, 12);

	// default to true for backward compatability
	protected boolean hitable = true;
	public static final String PREF_A_COLOR = "Adenine color";
	public static final String PREF_T_COLOR = "Thymine color";
	public static final String PREF_G_COLOR = "Guanine color";
	public static final String PREF_C_COLOR = "Cytosine color";
	public static final Color default_A_color = Color.GREEN;
	public static final Color default_T_color = Color.PINK;
	public static final Color default_G_color = Color.YELLOW;
	public static final Color default_C_color = Color.CYAN;

	public CharSeqGlyph() {
		super();
		setResidueFont(mono_default_font);
	}

	public void setResidues(String residues) {
		chariter = new ImprovedStringCharIter(residues);
		residue_length = residues.length();
		residuesSet = true;
	}

	public String getResidues() {
		return null;
	}

	public void setResiduesProvider(SearchableCharIterator iter, int seqlength) {
		chariter = iter;
		residue_length = seqlength;
		residuesSet = true;
	}

	public SearchableCharIterator getResiduesProvider() {
		return chariter;
	}

	// Essentially the same as SequenceGlyph.drawHorizontal
	@Override
	public void draw(ViewI view) {
		Rectangle2D.Double coordclipbox = view.getCoordBox();
		Graphics g = view.getGraphics();
		double pixels_per_base;
		int visible_ref_beg, visible_ref_end,
				visible_seq_beg, visible_seq_end, visible_seq_span,
				seq_beg_index, seq_end_index;
		visible_ref_beg = (int) coordclipbox.x;
		visible_ref_end = (int) (coordclipbox.x + coordclipbox.width);
		// adding 1 to visible ref_end to make sure base is drawn if only
		// part of it is visible
		visible_ref_end = visible_ref_end + 1;

		// ******** determine first base and last base displayed ********
		visible_seq_beg = (seq_beg < visible_ref_beg) ? visible_ref_beg : seq_beg;
		visible_seq_end = (seq_end > visible_ref_end) ? visible_ref_end : seq_end;
		visible_seq_span = visible_seq_end - visible_seq_beg;
		seq_beg_index = visible_seq_beg - seq_beg;
		seq_end_index = visible_seq_end - seq_beg;

		if (null != chariter && seq_beg_index <= residue_length) {

			if (seq_end_index > residue_length) {
				seq_end_index = residue_length;
			}

			Rectangle2D.Double scratchrect = new Rectangle2D.Double(visible_seq_beg, coordbox.y,
					visible_seq_span, coordbox.height);
			view.transformToPixels(scratchrect, pixelbox);
			pixels_per_base = ( view.getTransform()).getScaleX();
			int seq_pixel_offset = pixelbox.x;

			// ***** background already drawn in drawTraversal(), so just return if
			// ***** scale is < 1 pixel per base
			if (pixels_per_base < 1 || !residuesSet) {
				return;
			} // ***** otherwise semantic zooming to show more detail *****
			if (visible_seq_span > 0) {
				String str = chariter.substring(seq_beg_index, seq_end_index);
				drawHorizontalResidues(g, pixels_per_base, str, seq_beg_index, seq_end_index, seq_pixel_offset);
			}
		}
		super.draw(view);
	}

	/**
	 * Draw the sequence string for visible bases if possible.
	 *
	 * <p> We are showing letters regardless of the height constraints on the glyph.
	 */
	protected void drawHorizontalResidues(Graphics g,
			double pixelsPerBase,
			String str,
			int seqBegIndex,
			int seqEndIndex,
			int pixelStart) {
		int baseline = (this.pixelbox.y + (this.pixelbox.height / 2)) + this.fontmet.getAscent() / 2 - 1;
	
		drawResidueRectangles(g, pixelsPerBase, str);

		drawResidueStrings(g, pixelsPerBase, str, pixelStart, baseline);
	}

	private void drawResidueRectangles(Graphics g, double pixelsPerBase, String str) {
		for (int j = 0; j < str.length(); j++) {
			if (str.charAt(j) == 'A') {
				g.setColor(UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_A_COLOR, default_A_color));
			} else if (str.charAt(j) == 'T') {
				g.setColor(UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_T_COLOR, default_T_color));
			} else if (str.charAt(j) == 'G') {
				g.setColor(UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_G_COLOR, default_G_color));
			} else if (str.charAt(j) == 'C') {
				g.setColor(UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_C_COLOR, default_C_color));
			}
			if (str.charAt(j) == 'A' || str.charAt(j) == 'T' || str.charAt(j) == 'G' || str.charAt(j) == 'C') {
				//We calculate the floor of the offset as we want the offset to stay to the extreme left as possible.
				int offset = (int) (j * pixelsPerBase);
				//ceiling is done to the width because we want the width to be as wide as possible to avoid losing pixels.
				g.fillRect(pixelbox.x + offset, pixelbox.y, (int) Math.ceil(pixelsPerBase), pixelbox.height);
			}
		}
	}

	private void drawResidueStrings(Graphics g, double pixelsPerBase, String str, int pixelStart, int baseline) {
		g.setFont(getResidueFont());
		g.setColor(getForegroundColor());
		fontmet = GeneralUtils.getFontMetrics(getResidueFont());
		if (this.font_width < pixelsPerBase) {
			// Ample room to draw residue letters.
			for (int i = 0; i < str.length(); i++) {
				String c = String.valueOf(str.charAt(i));
				if (c != null) {
					g.drawString(c, pixelStart + (int) (i * pixelsPerBase), baseline);
				}
			}
		} else if (((double) ((int) pixelsPerBase) == pixelsPerBase) && (this.font_width == pixelsPerBase)) {
			// pixelsPerBase matches the font width.
			// Draw the whole string in one go.
			if (str != null) {
				g.drawString(str, pixelStart, baseline);
			}
		}
	}

	/** If false, then {@link #hit(Rectangle, ViewI)} and
	 *  {@link #hit(Rectangle2D, ViewI)} will always return false.
	 */
	public void setHitable(boolean h) {
		this.hitable = h;
	}

	@Override
	public boolean isHitable() {
		return hitable;
	}

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view) {
		if (isVisible && isHitable()) {
			calcPixels(view);
			return pixel_hitbox.intersects(pixelbox);
		} else {
			return false;
		}
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
		return isVisible && isHitable() && coord_hitbox.intersects(coordbox);
	}
}
