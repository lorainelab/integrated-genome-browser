package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import static com.affymetrix.genoviz.util.NeoConstants.default_bold_font;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * Creating special class for BAM soft-clipping glyph to be able to draw at low zoom.
 *
 * @author hiralv
 */
public class SoftClippingSeqGlyph extends SequenceGlyph {

    static final int MAX_CHAR_PIX = GeneralUtils.getFontMetrics(default_bold_font).stringWidth("G");
    static final int MIN_CHAR_PIX = 5;
    static final Font[] xpix2fonts = new Font[MAX_CHAR_PIX + 1];
    static final Font MONO_FONT = NeoConstants.default_bold_font;

    static {
        setBaseFont(MONO_FONT);
    }

    static void setBaseFont(Font base_fnt) {
        int pntcount = 3;
        while (true) {
            // converting to float to trigger correct deriveFont() method...
            Font newfnt = base_fnt.deriveFont((float) (pntcount));
            FontMetrics fm = GeneralUtils.getFontMetrics(newfnt);
            int text_width = fm.stringWidth("G");

            if (text_width > MAX_CHAR_PIX) {
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

    @Override
    public void draw(ViewI view) {
        if (isOverlapped()) {
            return;	// don't draw residues
        }

        super.draw(view);

    }

    @Override
    public boolean supportsSubSelection() {
        return false;
    }

    private Color getEffectiveContrastColor(Color color) {
        Color constractColor = default_bg_color;
        if (null != color) {
            int red = color.getRed();
            int green = color.getGreen();
            int blue = color.getBlue();

            int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;
            constractColor = (yiq >= 128) ? Color.BLACK : Color.WHITE;
        }
        return constractColor;
    }

    @Override
    protected void drawHorizontalResidues(Graphics g, double pixelsPerBase, String residues, int seqBegIndex, int seqEndIndex, int pixelStart) {        
        if (this.font_width > pixelsPerBase) {
            return;
        }
        
        int baseline = (getPixelBox().y + (getPixelBox().height / 2)) + this.fontmet.getAscent() / 2 - 1;
        g.setFont(getResidueFont());
        g.setColor(getEffectiveContrastColor(g.getColor()));
        int pixelOffset = (int) (pixelsPerBase - this.font_width);
        pixelOffset = pixelOffset > 2 ? pixelOffset / 2 : pixelOffset;
        
        for (int i = seqBegIndex; i < seqEndIndex; i++) {
            double f = i - seqBegIndex;
            String str = String.valueOf(residues.charAt(i));
            if (str != null) {
                    g.drawString(str,
                                    (pixelStart + (int) (f * pixelsPerBase) + pixelOffset),
                                    baseline);
            }
        } 
    }

    @Override
    protected void drawSelectedOutline(ViewI view) {
        super.drawSelectedOutline(view);
    }

}
