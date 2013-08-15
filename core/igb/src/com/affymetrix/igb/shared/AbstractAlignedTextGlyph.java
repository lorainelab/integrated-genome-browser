package com.affymetrix.igb.shared;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.BitSet;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.util.ImprovedStringCharIter;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.AbstractResiduesGlyph;
import com.affymetrix.genoviz.util.NeoConstants;


/**
 *
 * @author jnicol
 *
 * A glyph that shows a sequence of aligned text.
 * At low resolution (small scale) as a solid background rectangle
 * and at high resolution overlays the residue letters.
 *
 */
public abstract class AbstractAlignedTextGlyph extends AbstractResiduesGlyph {
	protected SearchableCharIterator chariter;
	private int residue_length = 0;
	private final BitSet residueMask = new BitSet();
	private static final Font mono_default_font = NeoConstants.default_bold_font;
		
	//public boolean packerClip = false;	// if we're in an overlapped glyph (top of packer), don't draw residues -- for performance
	
	public void setParentSeqStart(int beg) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setParentSeqEnd(int end) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getParentSeqStart() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getParentSeqEnd() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public AbstractAlignedTextGlyph() {
		super();
		setResidueFont(mono_default_font);
		// default to true for backward compatability
		setHitable(true);
	}

	@Override
	public void setResidues(String residues) {
		setResiduesProvider(new ImprovedStringCharIter(residues), residues.length());
	}

	@Override
	public String getResidues() {
		return null;
	}

	protected abstract boolean getShowMask();

	/**
	 * If this is set, we will only display residues that disagree with the residue mask.
	 * This is useful for BAM visualization.
	 * @param residues
	 */
	public void setResidueMask(String residues) {
		if (residues != null && chariter != null) {
			int minResLen = Math.min(residues.length(), residue_length);
			char[] residuesArr = residues.toLowerCase().toCharArray();
			char[] displayResArr = chariter.substring(0, minResLen).toLowerCase().toCharArray();

			// determine which residues disagree with the reference sequence
			for(int i=0;i<minResLen;i++) {
				residueMask.set(i, displayResArr[i] != residuesArr[i]);
			}
//			if (residueMask.isEmpty()) {
//				// Save space and time if all residues match the reference sequence.
//				residue_length = 0;
//				chariter = null;
//			}
		}
	}

	public void setResidueMask(byte[] SEQ) {
		char[] seqArr = new String(SEQ).toLowerCase().toCharArray();
		char[] displayResArr = chariter.substring(0, Math.min(seqArr.length, residue_length)).toLowerCase().toCharArray();
		boolean setRes = false;
		for (int i = 0; i < displayResArr.length; i++) {
			setRes = (SEQ[i] != '=') && (displayResArr[i] != seqArr[i]);
			residueMask.set(i, setRes);
		}
//		if (residueMask.isEmpty()) {
//			// Save space and time if all residues match the reference sequence.
//			residue_length = 0;
//			chariter = null;
//		}
	}

	public void setResiduesProvider(SearchableCharIterator iter, int seqlength) {
		chariter = iter;
		residue_length = seqlength;
	}

	public SearchableCharIterator getResiduesProvider() {
		return chariter;
	}

	// Essentially the same as SequenceGlyph.drawHorizontal
	@Override
	public void draw(ViewI view) {
		if (isOverlapped() || (residueMask.isEmpty() && getShowMask())) {
			return;	// don't draw residues
		}
		Rectangle2D.Double coordclipbox = view.getCoordBox();
		int visible_ref_beg, visible_seq_beg, seq_beg_index;
		visible_ref_beg = (int) coordclipbox.x;

		// determine first base displayed
		visible_seq_beg = Math.max(seq_beg, visible_ref_beg);
		seq_beg_index = visible_seq_beg - seq_beg;

		if (seq_beg_index > residue_length) {
			return;	// no residues to draw
		}

		double pixel_width_per_base = (view.getTransform()).getScaleX();
		if (residueMask.isEmpty() && pixel_width_per_base < 1) {
			return;	// If we're drawing all the residues, return if there's less than one pixel per base
		}
		if (pixel_width_per_base < 0.2) {
			return;	// If we're masking the residues, draw up to 5 residues at one pixel.
		}

		int visible_ref_end = (int) (coordclipbox.x + coordclipbox.width);
		// adding 1 to visible ref_end to make sure base is drawn if only
		// part of it is visible
		visible_ref_end = visible_ref_end + 1;
		int visible_seq_end = Math.min(seq_end, visible_ref_end);
		int visible_seq_span = visible_seq_end - visible_seq_beg;
		if (visible_seq_span > 0) {
			// ***** semantic zooming to show more detail *****
			Rectangle2D.Double scratchrect = new Rectangle2D.Double(visible_seq_beg, getCoordBox().y,
					visible_seq_span, getCoordBox().height);
			view.transformToPixels(scratchrect, getPixelBox());
			int seq_end_index = visible_seq_end - seq_beg;
			if (seq_end_index > residue_length) {
				seq_end_index = residue_length;
			}
			if (Math.abs((long) seq_end_index - (long) seq_beg_index) > 100000) {
				// something's gone wrong.  Ignore.
				Logger.getLogger(AbstractAlignedTextGlyph.class.getName()).fine("Invalid string: " + seq_beg_index + "," + seq_end_index);
				return;
			}
			int seq_pixel_offset = getPixelBox().x;
			Graphics g = view.getGraphics();
			drawHorizontalResidues(view, pixel_width_per_base, seq_beg_index, seq_end_index, seq_pixel_offset);
		}
	}

	/**
	 * Draw the sequence string for visible bases if possible.
	 *
	 * <p> We are showing letters regardless of the height constraints on the glyph.
	 */
	private void drawHorizontalResidues(ViewI view,
			double pixelsPerBase,
			int seqBegIndex,
			int seqEndIndex,
			int pixelStart) {
		char[] charArray = chariter.substring(seqBegIndex, seqEndIndex).toCharArray();
		BitSet bitSet = residueMask.get(seqBegIndex,seqEndIndex);
		drawResidueRectangles(view, pixelsPerBase, charArray, seqBegIndex, seqEndIndex, bitSet);
		drawResidueStrings(view, pixelsPerBase, charArray, seqBegIndex, bitSet, pixelStart, getShowMask());
	}

	protected abstract void drawResidueRectangles(
			ViewI view, double pixelsPerBase, char[] charArray, int seqBegIndex, int seqEndIndex, BitSet residueMask);

	protected Color getResidueStringsColor(){
		return Color.BLACK;
	}
	
	private void drawResidueStrings(
			ViewI view, double pixelsPerBase, char[] charArray, int seqBegIndex, BitSet residueMask, int pixelStart, boolean show_mask) {
		if (this.font_width <= pixelsPerBase) {
			Graphics g = view.getGraphics();
			// Ample room to draw residue letters.
			g.setFont(getResidueFont());
			g.setColor(getResidueStringsColor());
			int baseline = (this.getPixelBox().y + (this.getPixelBox().height / 2)) + this.fontmet.getAscent() / 2 - 1;
			int pixelOffset = (int) (pixelsPerBase - this.font_width);
			pixelOffset = pixelOffset > 2 ? pixelOffset/2 : pixelOffset;
			for (int i = 0; i < charArray.length; i++) {
				if(show_mask && !residueMask.get(i)) {
					continue;	// skip drawing of this residue
				}
				g.drawChars(charArray, i, 1, pixelStart + (int) (i * pixelsPerBase) + pixelOffset, baseline);
			}
		}
	}

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view) {
		if (isVisible() && isHitable()) {
			calcPixels(view);
			return pixel_hitbox.intersects(getPixelBox());
		} else {
			return false;
		}
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
		return isVisible() && isHitable() && coord_hitbox.intersects(getCoordBox());
	}

}
