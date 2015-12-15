package com.affymetrix.igb.glyph;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import static com.affymetrix.genoviz.util.NeoConstants.default_bold_font;
import com.affymetrix.igb.util.ResidueColorHelper;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_EDGE_MATCH_COLOR;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Creating special class for BAM insertion glyph to be able to draw at low zoom.
 *
 * @author hiralv
 */
public class TriangleInsertionSeqGlyph extends SequenceGlyph {

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
    private Color bgcolor = Color.white;
    private final boolean showMask;
    private final Color maskColor;

    public TriangleInsertionSeqGlyph(boolean showMask, Color maskColor) {
        this.showMask = showMask;
        this.maskColor = maskColor;
    }

    @Override
    public void draw(ViewI view) {
        if (isOverlapped()) {
            return;	// don't draw residues
        }

        Rectangle pixelbox = view.getScratchPixBox();
        view.transformToPixels(this.getCoordBox(), pixelbox);

        pixelbox = optimizeBigRectangleRendering(view, pixelbox);

        pixelbox.width = Math.max(pixelbox.width, getMinPixelsWidth());
        pixelbox.height = Math.max(pixelbox.height, getMinPixelsHeight());

        Graphics g = view.getGraphics();
        g.setColor(getColor());
        g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
        super.draw(view);
        if (pixelbox.width > 2 && pixelbox.height > 2) {
            g.setColor(bgcolor);
            Graphics2D g2d = (Graphics2D) g.create();
//            g.fillRect(pixelbox.x + 1, pixelbox.y + 1, pixelbox.width - 2, pixelbox.height - 2);
            Triangle triangle = new Triangle(
                    new Point2D.Double(pixelbox.width / 2, pixelbox.height / 4),
                    new Point2D.Double(pixelbox.width, 0),
                    new Point2D.Double(0, 0));
            g2d.translate(pixelbox.x, pixelbox.y);
            g2d.fill(triangle);
        }

    }

    /**
     * Sets the outline color; the fill color is automatically calculated as a darker shade.
     */
    @Override
    public void setColor(Color bgcolor) {
        super.setColor(bgcolor);
        this.bgcolor = bgcolor.darker();
    }

    @Override
    public boolean supportsSubSelection() {
        return false;
    }

    @Override
    public Color getForegroundColor() {
        return getEffectiveContrastColor(bgcolor);
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
    protected void drawHorizontalResidues(Graphics g, double pixelsPerBase, String residueStr, int seqBegIndex, int seqEndIndex, int pixelStart) {
        char[] charArray = residueStr.toCharArray();
        drawResidueRectangles(g, pixelsPerBase, charArray, getPixelBox().x, getPixelBox().y, getPixelBox().height);
        drawResidueStrings(g, pixelsPerBase, charArray, pixelStart);
    }

    private void drawResidueRectangles(Graphics g, double pixelsPerBase, char[] charArray, int x, int y, int height) {
        int intPixelsPerBase = (int) Math.ceil(pixelsPerBase);
        for (int j = 0; j < charArray.length; j++) {
            if (showMask) {
                g.setColor(maskColor);
            } else {
                g.setColor(ResidueColorHelper.getColorHelper().determineResidueColor(charArray[j]));
            }

            //Create a colored rectangle.
            //We calculate the floor of the offset as we want the offset to stay to the extreme left as possible.
            int offset = (int) (j * pixelsPerBase);
            //ceiling is done to the width because we want the width to be as wide as possible to avoid losing pixels.
            g.fillRect(x + offset, y, intPixelsPerBase, height);
        }
    }

    private void drawResidueStrings(Graphics g, double pixelsPerBase, char[] charArray, int pixelStart) {
        if (!showMask) {
            if (MIN_CHAR_PIX > pixelsPerBase) {
                return;
            }
            int index = (int) (pixelsPerBase > MAX_CHAR_PIX ? MAX_CHAR_PIX : pixelsPerBase);
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
    }

    @Override
    protected void drawSelectedOutline(ViewI view) {
        Color previousBgColor = bgcolor;
        bgcolor = PreferenceUtils.getColor(PREF_EDGE_MATCH_COLOR, Color.RED);
        draw(view);
        bgcolor = previousBgColor;
    }

    private class Triangle extends Path2D.Double {

        public Triangle(Point2D... points) {
            moveTo(points[0].getX(), points[0].getY());
            lineTo(points[1].getX(), points[1].getY());
            lineTo(points[2].getX(), points[2].getY());
            closePath();
        }

    }
}
