/**
 *   Copyright (c) 2001-2004 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.glyph;

import java.awt.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.util.NeoConstants;

import com.affymetrix.genometryImpl.util.ImprovedStringCharIter;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;

/**
 * Modified version of {@link com.affymetrix.genoviz.glyph.SequenceGlyph}.
 *
 * CharSeqGlyph differs from SequenceGlyph in that it can take either a 
 * String as residues (via sg.setResidues()) or a {@link CharIterator}
 * as a provider of residues (via sg.setResiduesProvider()) .
 * This allows one to glyphify large biological sequences while maintaining a more 
 * compressed representation of the sequences residues.
 * 
 * For now only allowing horizontal CharSeqGlyphs (unlike SequenceGlyph, which 
 *   can be horizontal or vertical).
 *
 * A glyph that shows a sequence of residues.
 * At low resolution (small scale) as a solid background rectangle
 * and at high resolution overlays the residue letters.
 *
 */
public final class CharSeqGlyph extends AbstractResiduesGlyph
				implements NeoConstants {

	int parent_seq_beg, parent_seq_end;
	SearchableCharIterator chariter;
	FillRectGlyph full_rect; // the background rectangle
	boolean residuesSet = false;
	Rectangle2D scratchrect;
	int residue_length = 0;
	static Font mono_default_font = new Font("Monospaced", Font.BOLD, 12);

	// default to true for backward compatability
	protected boolean hitable = true;
	private boolean show_background = true;
	private boolean drawingRects = false;

	public final boolean isDrawingRects() {
		return this.drawingRects;
	}

	public CharSeqGlyph() {
		super(HORIZONTAL);
		full_rect = new FillRectGlyph();
		scratchrect = new Rectangle2D();
		setResidueFont(mono_default_font);
	}

	@Override
	public void setCoords(double x, double y, double width, double height) {
		super.setCoords(x, y, width, height);
		full_rect.setCoords(x, y, width, height);
	}

	@Override
	public void setCoordBox(Rectangle2D coordbox) {
		super.setCoordBox(coordbox);
		full_rect.setCoordBox(coordbox);
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

	/**
	 * Overriding drawTraversal() to affect drawing order.
	 * <ol>
	 * <li> Draw background rectangle.
	 * <li> Draw any child glyphs.
	 * <li> Draw residues if at appropriate resolution.
	 * </ol>
	 *
	 * <p> Note that CharSeqGlyph only supports visual subselection
	 * if Scene.selectionAppearance is set to OUTLINE.
	 * If set to FILL, will visually fill whole glyph
	 * even if only part of glyph is actually selected.
	 */
	@Override
	public void drawTraversal(ViewI view) {
		if (coordbox.intersects(view.getCoordBox()) && isVisible) {
			int sel_style = view.getScene().getSelectionAppearance();

			// 1.) draw background rectangle
			if (selected && sel_style == SceneI.SELECT_FILL) {
				full_rect.setSelected(true);
				full_rect.drawTraversal(view);
				full_rect.setSelected(false);
			} else {
				if (show_background) {
					full_rect.drawTraversal(view);
				}
			}

			// 2.) draw any child glyphs
			if (children != null) {
				drawChildren(view);
			}

			// 3.) draw residues if at appropriate resolution
			if (selected) {
				drawSelected(view);
			} else {
				draw(view);
			}
		}
	}


	// Essentially the same as SequenceGlyph.drawHorizontal
	@Override
	public void draw(ViewI view) {
		Rectangle2D coordclipbox = view.getCoordBox();
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

			scratchrect.setRect(visible_seq_beg, coordbox.y,
							visible_seq_span, coordbox.height);
			view.transformToPixels(scratchrect, pixelbox);
			pixels_per_base = ((LinearTransform) view.getTransform()).getScaleX();
			int seq_pixel_offset = pixelbox.x;
			int seq_pixel_width = pixelbox.width;

			// ***** background already drawn in drawTraversal(), so just return if
			// ***** scale is < 1 pixel per base
			if (pixels_per_base < 1 || !residuesSet) {
				return;
			} // ***** otherwise semantic zooming to show more detail *****
			else {
				if (visible_seq_span > 0) {
					drawHorizontalResidues(g, pixels_per_base, chariter, seq_beg_index, seq_end_index, seq_pixel_offset);
				}
			}
		}
		super.draw(view);
	}

	/**
	 * Draw the sequence string for visible bases if possible.
	 *
	 * <p> We are showing letters regardless of the height constraints on the glyph.
	 */

	// Look at similarity with SequenceGlyph.drawHorizontalResidues
	protected void drawHorizontalResidues(Graphics g,
					double pixelsPerBase,
					SearchableCharIterator residue_provider,
					int seqBegIndex,
					int seqEndIndex,
					int pixelStart) {
		int baseline = (this.pixelbox.y + (this.pixelbox.height / 2)) + this.fontmet.getAscent() / 2 - 1;
		g.setFont(getResidueFont());
		g.setColor(getForegroundColor());
		fontmet = Toolkit.getDefaultToolkit().getFontMetrics(getResidueFont());

		if (this.font_width < pixelsPerBase) { // Ample room to draw residue letters.
			for (int i = seqBegIndex; i < seqEndIndex; i++) {
				double f = i - seqBegIndex;
				String str = String.valueOf(residue_provider.charAt(i));
				if (str != null) {
					g.drawString(str,
									(pixelStart + (int) (f * pixelsPerBase)),
									baseline);
				}
			}
		} else if (((double) ((int) pixelsPerBase) == pixelsPerBase) // Make sure it's an integral number of pixels per base.
						&& (this.font_width == pixelsPerBase) //&& ( this.fontmet.getHeight() < this.pixelbox.height )
						) { // pixelsPerBase matches the font width.
			// Draw the whole string in one go.

			String str = residue_provider.substring(seqBegIndex, seqEndIndex);
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
	public boolean hit(Rectangle2D coord_hitbox, ViewI view) {
		return isVisible && isHitable() && coord_hitbox.intersects(coordbox);
	}

	/**
	 * Overriding moveRelative to make sure the background rectangle glyph also gets moved.
	 * (We shouldn't need to do this for moveAbsolute,
	 *  since it calls moveRelative.)
	 */
	@Override
	public void moveRelative(double diffx, double diffy) {
		full_rect.moveRelative(diffx, diffy);
		super.moveRelative(diffx, diffy);
	}

	@Override
	public void addChild(GlyphI child, int position) {
		super.addChild(child, position);
		child.setCoords(child.getCoordBox().x, this.coordbox.y,
						child.getCoordBox().width, this.coordbox.height);
	}

	@Override
	public void addChild(GlyphI child) {
		super.addChild(child);
		child.setCoords(child.getCoordBox().x, this.coordbox.y,
						child.getCoordBox().width, this.coordbox.height);
	}

	public void setParentSeqStart(int beg) {
		parent_seq_beg = beg;
	}

	public void setParentSeqEnd(int end) {
		parent_seq_end = end;
	}

	public int getParentSeqStart() {
		return parent_seq_beg;
	}

	public int getParentSeqEnd() {
		return parent_seq_end;
	}

	@Override
	public void setBackgroundColor(Color c) {
		super.setBackgroundColor(c);
		full_rect.setBackgroundColor(c);
	}

	/**
	 * Overridden to make sure background rectangle gets its scene set properly.
	 */
	@Override
	public void setScene(Scene s) {
		super.setScene(s);
		full_rect.setScene(s);
	}

	/** Set whether or not the background will be filled-in
	 *  with solid color.  If false, background is transparent,
	 *  except if the selection mode is FILL and the glyph is selected.
	 *  Default is true.
	 */
	public void setShowBackground(boolean show) {
		show_background = show;
	}

	/** Whether or not the background will be filled-in
	 *  with solid color.  If false, background is transparent,
	 *  except if the selection mode is FILL and the glyph is selected.
	 *  Default is true.
	 */
	public final boolean getShowBackground() {
		return show_background;
	}
}
