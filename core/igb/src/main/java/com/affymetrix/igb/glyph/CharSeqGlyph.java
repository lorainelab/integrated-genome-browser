package com.affymetrix.igb.glyph;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.ImprovedStringCharIter;
import com.affymetrix.genometry.util.SearchableCharIterator;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.LabelledRectGlyph;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.util.ResidueColorHelper;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CharSeqGlyph differs from SequenceGlyph in that it can take either a
 * String as residues (via sg.setResidues()) or a {@link SearchableCharIterator}
 * as a provider of residues (via sg.setResiduesProvider()) .
 * This allows one to glyphify large biological sequences while maintaining a
 * more
 * compressed representation of the sequences residues.
 *
 * A glyph that shows a sequence of residues.
 * At low resolution (small scale) as a solid background rectangle
 * and at high resolution overlays the residue letters.
 *
 */
public class CharSeqGlyph extends SequenceGlyph {

    private static final Font mono_default_font = NeoConstants.default_bold_font;
    static final int max_char_xpix = GeneralUtils.getFontMetrics(default_bold_font).stringWidth("G"); // maximum allowed pixel width of chars
    static final int min_char_xpix = 5;  // minimum allowed pixel width of chars
    // xpix2fonts: index is char width in pixels, entry is Font that gives that char width (or smaller)
    static final Font[] xpix2fonts = new Font[max_char_xpix + 1];

    private static final ResidueColorHelper helper = ResidueColorHelper.getColorHelper();

    static {
        setBaseFont(mono_default_font);
    }

    public static void setBaseFont(Font base_fnt) {
        int pntcount = 3;
        while (true) {
            // converting to float to trigger correct deriveFont() method...
            Font newfnt = base_fnt.deriveFont((float) (pntcount));
            FontMetrics fm = GeneralUtils.getFontMetrics(newfnt);
            int text_width = fm.stringWidth("G");

            if (text_width > max_char_xpix) {
                break;
            }

            xpix2fonts[text_width] = newfnt;
            pntcount++;
        }
        Font smaller_font = null;
        for (int i = 0; i < xpix2fonts.length; i++) {
            if (xpix2fonts[i] != null) {
                smaller_font = xpix2fonts[i];
            } else {
                xpix2fonts[i] = smaller_font;
            }
        }
    }

    private static void drawResidueRectangles(Graphics g, double pixelsPerBase, char[] charArray, int x, int y, int height) {
        int intPixelsPerBase = (int) Math.ceil(pixelsPerBase);
        for (int j = 0; j < charArray.length; j++) {
            g.setColor(helper.determineResidueColor(charArray[j]));

            //Create a colored rectangle.
            //We calculate the floor of the offset as we want the offset to stay to the extreme left as possible.
            int offset = (int) (j * pixelsPerBase);
            //ceiling is done to the width because we want the width to be as wide as possible to avoid losing pixels.
            g.fillRect(x + offset, y, intPixelsPerBase, height);
        }
    }

    public static CharSeqGlyph initSeqGlyph(BioSeq viewSeq, AxisGlyph axis) {
        CharSeqGlyph seq_glyph = new CharSeqGlyph();
        //seq_glyph.setForegroundColor(axis_fg);
        seq_glyph.setForegroundColor(Color.BLACK);
        seq_glyph.setShowBackground(false);
        seq_glyph.setHitable(true);
        seq_glyph.setDrawOrder(Glyph.DRAW_CHILDREN_FIRST);
        seq_glyph.setCoords(viewSeq.getMin(), axis.getCoordBox().getY() + axis.getCoordBox().getHeight() + 4, viewSeq.getLengthDouble(), 10);
        seq_glyph.setResiduesProvider(viewSeq, viewSeq.getLength());
        SeqSymmetry compsym = viewSeq.getComposition();
        if (compsym != null) {
            showFillRect(viewSeq, seq_glyph, compsym, axis);
        }
        return seq_glyph;
    }

    private static void showFillRect(BioSeq viewSeq, CharSeqGlyph seqGlyph, SeqSymmetry compsym, AxisGlyph axis) {
        int compcount = compsym.getChildCount();
        // create a color, c3, in between the foreground and background colors
        Color c1 = axis.getForegroundColor();
        Color c2 = axis.getBackgroundColor();
        Color c3 = new Color((c1.getRed() + 2 * c2.getRed()) / 3, (c1.getGreen() + 2 * c2.getGreen()) / 3, (c1.getBlue() + 2 * c2.getBlue()) / 3);
        for (int i = 0; i < compcount; i++) {
            // Make glyphs for contigs
            SeqSymmetry childsym = compsym.getChild(i);
            SeqSpan childspan = childsym.getSpan(viewSeq);
            SeqSpan ospan = SeqUtils.getOtherSpan(childsym, childspan);
            SolidGlyph cgl;
            if (ospan.getBioSeq().isComplete(ospan.getMin(), ospan.getMax())) {
                cgl = new FillRectGlyph();
                cgl.setColor(c3);
            } else {
                switch (viewSeq.getId()) {
                    case IGBConstants.GENOME_SEQ_ID: {
                        // hide axis numbering
                        axis.setLabelFormat(AxisGlyph.NO_LABELS);
                        cgl = new LabelledRectGlyph();
                        String text = ospan.getBioSeq().getId();
                        if (text.toLowerCase().startsWith("chr")) {
                            text = text.substring(3);
                        }
                        ((LabelledRectGlyph) cgl).setText(text);
                        break;
                    }
                    case IGBConstants.ENCODE_REGIONS_ID: {
                        cgl = new LabelledRectGlyph();
                        String text = childsym.getID();
                        if (text != null) {
                            ((LabelledRectGlyph) cgl).setText(text);
                        }
                        break;
                    }
                    default:
                        cgl = new OutlineRectGlyph();
                        break;
                }
                cgl.setColor(c1);
            }
            cgl.setHitable(false);
            cgl.setCoords(childspan.getMinDouble(), 0, childspan.getLengthDouble(), 10);
            // also note that "Load residues in view" produces additional
            // contig-like glyphs that can partially hide these glyphs.
            seqGlyph.addChild(cgl);
        }
    }
    private SearchableCharIterator chariter;
    private int residue_length = 0;

    public CharSeqGlyph() {
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
        Rectangle2D.Double coordclipbox = view.getCoordBox();
        int visible_ref_beg, visible_seq_beg, seq_beg_index;
        visible_ref_beg = (int) coordclipbox.x;

        // determine first base displayed
        visible_seq_beg = Math.max(seq_beg, visible_ref_beg);
        seq_beg_index = visible_seq_beg - seq_beg;

        if (null != chariter && seq_beg_index <= residue_length) {
            double pixel_width_per_base = (view.getTransform()).getScaleX();
            // ***** background already drawn in drawTraversal(), so just return if
            // ***** scale is < 1 pixel per base
            if (pixel_width_per_base < 1) {
                return;
            }
            int visible_ref_end = (int) (coordclipbox.x + coordclipbox.width);
            // adding 1 to visible ref_end to make sure base is drawn if only
            // part of it is visible
            visible_ref_end += 1;
            int visible_seq_end = Math.min(seq_end, visible_ref_end);
            int visible_seq_span = visible_seq_end - visible_seq_beg;
            // ***** otherwise semantic zooming to show more detail *****
            if (visible_seq_span > 0) {
                Rectangle2D.Double scratchrect = new Rectangle2D.Double(visible_seq_beg, getCoordBox().y,
                        visible_seq_span, getCoordBox().height);
                view.transformToPixels(scratchrect, getPixelBox());
                int seq_end_index = visible_seq_end - seq_beg;
                if (seq_end_index > residue_length) {
                    seq_end_index = residue_length;
                }
                if (Math.abs((long) seq_end_index - (long) seq_beg_index) > 100000) {
                    // something's gone wrong.  Ignore.
                    Logger.getLogger(CharSeqGlyph.class.getName()).log(Level.FINE, "Invalid string: {0},{1}", new Object[]{seq_beg_index, seq_end_index});
                    return;
                }
                int seq_pixel_offset = getPixelBox().x;
                String str = chariter.substring(seq_beg_index, seq_end_index);
                Graphics g = view.getGraphics();
                drawHorizontalResidues(g, pixel_width_per_base, str, seq_beg_index, seq_end_index, seq_pixel_offset);
            }
        }
        super.draw(view);
    }

    /**
     * Draw the sequence string for visible bases if possible.
     *
     * <p>
     * We are showing letters regardless of the height constraints on the glyph.
     */
    @Override
    protected void drawHorizontalResidues(Graphics g, double pixelsPerBase, String residueStr, int seqBegIndex, int seqEndIndex, int pixelStart) {
        char[] charArray = residueStr.toCharArray();
        drawResidueRectangles(g, pixelsPerBase, charArray, getPixelBox().x, getPixelBox().y, getPixelBox().height);
        drawResidueStrings(g, pixelsPerBase, charArray, pixelStart);
    }

    private void drawResidueStrings(Graphics g, double pixelsPerBase, char[] charArray, int pixelStart) {
        if (min_char_xpix > pixelsPerBase) {
            return;
        }

        int index = (int) (pixelsPerBase > max_char_xpix ? max_char_xpix : pixelsPerBase);
        Font xmax_font = xpix2fonts[index];
        setFont(xmax_font);
        // Ample room to draw residue letters.
        g.setFont(getResidueFont());
        g.setColor(getForegroundColor());
        int baseline = (this.getPixelBox().y + (this.getPixelBox().height / 2)) + this.fontmet.getAscent() / 2 - 1;
        int pixelOffset = (int) (pixelsPerBase - this.font_width);
        pixelOffset = pixelOffset > 2 ? pixelOffset / 2 : pixelOffset;
        for (int i = 0; i < charArray.length; i++) {
            g.drawChars(charArray, i, 1, pixelStart + (int) (i * pixelsPerBase) + pixelOffset, baseline);
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
