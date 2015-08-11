package com.lorainelab.protannot;

import com.affymetrix.genometry.AminoAcid;
import com.affymetrix.genometry.util.ImprovedStringCharIter;
import com.affymetrix.genometry.util.SearchableCharIterator;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Draws colored rectangles and corresponding character based upon nucleotide.
 *
 * @see com.affymetrix.genoviz.glyph.SequenceGlyph
 */
public class ColoredResiduesGlyph extends SequenceGlyph {

    private SearchableCharIterator chariter;
    private boolean residuesSet = false;
    private int residue_length = 0;
    private static final Font mono_default_font = NeoConstants.default_bold_font;
    private boolean drawRect;

    private final ProtAnnotPreferencesService protAnnotPreferencesService;


    public ColoredResiduesGlyph(ProtAnnotPreferencesService protAnnotPreferencesService, boolean drawRectangle) {
        super();
        setResidueFont(mono_default_font);
        this.drawRect = drawRectangle;
        // default to true for backward compatability
        setHitable(true);
        this.protAnnotPreferencesService = protAnnotPreferencesService;
    }

    @Override
    public void setResidues(String residues) {
        chariter = new ImprovedStringCharIter(residues);
        residue_length = residues.length();
        residuesSet = true;
    }

    @Override
    public String getResidues() {
        return null;
    }

    public void setResiduesProvider(SearchableCharIterator iter, int seqlength) {
        chariter = iter;
        residue_length = seqlength;
        residuesSet = true;
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
        visible_ref_end += 1;

        // ******** determine first base and last base displayed ********
        visible_seq_beg = (seq_beg < visible_ref_beg) ? visible_ref_beg : seq_beg;
        visible_seq_end = (seq_end > visible_ref_end) ? visible_ref_end : seq_end;
        visible_seq_span = visible_seq_end - visible_seq_beg;
        seq_beg_index = visible_seq_beg - seq_beg;
        seq_end_index = visible_seq_end - seq_beg;

        if (chariter != null) {

            if (seq_beg_index <= residue_length) {

                if (seq_end_index > residue_length) {
                    seq_end_index = residue_length;
                }

                Rectangle2D.Double scratchrect = new Rectangle2D.Double(visible_seq_beg, getCoordBox().y,
                        visible_seq_span, getCoordBox().height);
                view.transformToPixels(scratchrect, getPixelBox());
                pixels_per_base = (view.getTransform()).getScaleX();

                // ***** background already drawn in drawTraversal(), so just return if
                // ***** scale is < 1 pixel per base
                if (pixels_per_base < 1 || !residuesSet) {
                    return;
                } // ***** otherwise semantic zooming to show more detail *****
                if (visible_seq_span > 0) {
                    int seq_pixel_offset = getPixelBox().x;
                    String str = chariter.substring(seq_beg_index, seq_end_index);
                    drawHorizontalResidues(g, pixels_per_base, str, seq_beg_index, seq_end_index, seq_pixel_offset);
                }
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
    protected void drawHorizontalResidues(Graphics g,
            double pixelsPerBase,
            String str,
            int seqBegIndex,
            int seqEndIndex,
            int pixelStart) {
        int baseline = (this.getPixelBox().y + (this.getPixelBox().height / 2)) + this.fontmet.getAscent() / 2 - 1;

        if (drawRect) {
            drawResidueRectangles(g, pixelsPerBase, str);
        }
        drawResidueStrings(g, pixelsPerBase, str, pixelStart, baseline);
    }

    private void drawResidueRectangles(Graphics g, double pixelsPerBase, String str) {
        for (int j = 0; j < str.length(); j++) {
            char charAt = str.charAt(j);
            if (charAt == 'A' || charAt == 'a') {
                g.setColor(new Color(protAnnotPreferencesService.getResidueRGB(AminoAcid.Alanine)));
            } else if (charAt == 'T' || charAt == 't') {
                g.setColor(new Color(protAnnotPreferencesService.getResidueRGB(AminoAcid.Threonine)));
            } else if (charAt == 'G' || charAt == 'g') {
                g.setColor(new Color(protAnnotPreferencesService.getResidueRGB(AminoAcid.Glycine)));
            } else if (charAt == 'C' || charAt == 'c') {
                g.setColor(new Color(protAnnotPreferencesService.getResidueRGB(AminoAcid.Cysteine)));
            } else {
                continue;
            }

            //Create a colored rectangle.
            //We calculate the floor of the offset as we want the offset to stay to the extreme left as possible.
            int offset = (int) (j * pixelsPerBase);
            //ceiling is done to the width because we want the width to be as wide as possible to avoid losing pixels.
            g.fillRect(getPixelBox().x + offset, getPixelBox().y, (int) Math.ceil(pixelsPerBase), getPixelBox().height);
        }
    }

    private void drawResidueStrings(Graphics g, double pixelsPerBase, String str, int pixelStart, int baseline) {
        g.setFont(getResidueFont());
        g.setColor(getForegroundColor());
        if (this.font_width <= pixelsPerBase) {
            // Ample room to draw residue letters.
            for (int i = 0; i < str.length(); i++) {
                String c = String.valueOf(str.charAt(i));
                if (c != null) {
                    g.drawString(c, pixelStart + (int) (i * pixelsPerBase), baseline);
                }
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
