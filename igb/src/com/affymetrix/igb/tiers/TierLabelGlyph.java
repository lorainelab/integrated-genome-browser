package com.affymetrix.igb.tiers;

import java.awt.*;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.util.NeoConstants;

/**
 * A glyph used to display a label for a TierGlyph.
 */
public final class TierLabelGlyph extends SolidGlyph implements NeoConstants {

    private Font fnt;
    private boolean show_background = false;
    private boolean show_outline = false;
    private String word_breaker_regexp = "\\s|_|-|\\.|/|\\\\|\\|";
    private int text_padding = 3;

    @Override
    public String toString() {
        return ("TierLabelGlyph: label: \"" + getLabelString() + "\"  +coordbox: " + coordbox);
    }

    /**
     *  Constructor.
     *  @param reference_tier the tier in the main part of the AffyLabelledTierMap,
     *    must not be null
     */
    public TierLabelGlyph(TierGlyph reference_tier) {
        this.setInfo(reference_tier);
    }

    /** Overridden such that the info must be of type TierGlyph.  It is used
     *  to store the reference tier that will be returned by getReferenceTier().
     */
    @Override
    public void setInfo(Object o) {
        if (!(o instanceof TierGlyph)) {
            throw new IllegalArgumentException();
        }
        super.setInfo(o);
    }

    /** Returns the reference tier from the main map in AffyLabelledTierMap.
     *  Equivalent to value returned by getInfo().  Will not be null.
     */
    public TierGlyph getReferenceTier() {
        return (TierGlyph) getInfo();
    }

    private String getDirectionString(TierGlyph tg) {
        switch (tg.direction) {
            case TierGlyph.DIRECTION_FORWARD:
                return "(+)";
            case TierGlyph.DIRECTION_REVERSE:
                return "(-)";
            case TierGlyph.DIRECTION_BOTH:
                return "(+/-)";
            default: // DIRECTION_NONE
                return "";
        }
    }

    /**
     * Returns the label of the reference tier, or some default string if there isn't one.
     * @return
     */
    private String getLabelString() {
        TierGlyph reference_tier = getReferenceTier();
        if (reference_tier == null || reference_tier.getLabel() == null) {
            return ".......";
        }
        String direction_str = getDirectionString(reference_tier);
        if (direction_str.length() == 0) {
            return reference_tier.getLabel();
        }
        return reference_tier.getLabel() + " " + direction_str;
    }

    public void setShowBackground(boolean show) {
        show_background = show;
    }

    private boolean getShowBackground() {
        return show_background;
    }

    public void setShowOutline(boolean show) {
        show_outline = show;
    }

    private boolean getShowOutline() {
        return show_outline;
    }

    @Override
    public void draw(ViewI view) {
        TierGlyph reftier = this.getReferenceTier();
        Color fgcolor = reftier.getForegroundColor();
        Color bgcolor = reftier.getFillColor();

        Graphics g = view.getGraphics();
        g.setPaintMode();

        view.transformToPixels(coordbox, pixelbox);

        if (getShowBackground()) { // show background
            if (bgcolor != null) {
                g.setColor(bgcolor);
                g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
            }
        }
        if (getShowOutline()) {
            g.setColor(fgcolor);
            g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width - 1, pixelbox.height - 1);
            g.drawRect(pixelbox.x + 1, pixelbox.y + 1, pixelbox.width - 3, pixelbox.height - 3);
        }

        g.setColor(fgcolor);
        drawLabel(g, view.getPixelBox());

        super.draw(view);
    }

    void drawLabel(Graphics g, Rectangle boundingPixelBox) {
        // assumes that pixelbox coordinates are already computed



        if (null != fnt) {
            g.setFont(fnt);
        }

        String label = getLabelString();
        // this was for test:
        // label = "hey_this_is_going_to_be-a-long-text-to-test.the.behaviour";
        if (label == null) {
            return;
        }

        FontMetrics fm = g.getFontMetrics();
        int text_height = fm.getAscent() + fm.getDescent();
        // only show text if it will fit in pixelbox
        if (text_height > pixelbox.height) {
            return;
        }

        // Lower bound of visible glyph
        int lowerY = Math.max(pixelbox.y, boundingPixelBox.y);

        // Upper bound of visible glyph
        int upperY = Math.min(
                pixelbox.y + pixelbox.height,
                boundingPixelBox.y + boundingPixelBox.height);

        int text_width = fm.stringWidth(label);
        if (text_width > pixelbox.width) {

            // OLD: if text wider than glyph's pixelbox, then show beginning of text
            OLD:
            pixelbox.x += text_padding + 1;

            // NOW: split line by breakers and show it.


            // splitting
            String[] words = label.split(word_breaker_regexp);
            // cc,wc and lc  are character, word and line counts.
            int charCounter = 0, wordCounter = 0, lineCounter = 0;

            // idea: combine words until they get wider:

            while (wordCounter < words.length) {

                int word_width = fm.stringWidth(words[wordCounter]);
                // i will be the number of proceding words to add to words[wc]
                int numberOfWords = 1;
                // cc is counted to add word breakers!
                charCounter += words[wordCounter].length();

                while (wordCounter + numberOfWords < words.length) {
                    String nextword = label.charAt(charCounter) + words[wordCounter + numberOfWords];

                    int nextword_width = fm.stringWidth(nextword);

                    if (word_width + nextword_width > pixelbox.width - 2 * text_padding - 1) {
                        break;
                    }
                    words[wordCounter] += nextword;
                    word_width += nextword_width;
                    charCounter += nextword.length();
                    //words[wordCounter + numberOfWords] = "";
                    numberOfWords++;
                }
                g.drawString(words[wordCounter], pixelbox.x, (lowerY + upperY + text_height * (lineCounter-1)) / 2 + lineCounter * 2 + text_padding);
                wordCounter += numberOfWords;
                lineCounter++;
                charCounter++;
            }

        } else {
            // if text wider than glyph's pixelbox, then center text
            pixelbox.x += pixelbox.width / 2 - text_width / 2;
            g.drawString(label, pixelbox.x, (lowerY + upperY + text_height) / 2);
        }

    }

    /** Draws the outline in a way that looks good for tiers.  With other glyphs,
     *  the outline is usually drawn a pixel or two larger than the glyph.
     *  With TierGlyphs, it is better to draw the outline inside of or contiguous
     *  with the glyph's borders.
     **/
    @Override
    protected void drawSelectedOutline(ViewI view) {
        draw(view);

        Graphics g = view.getGraphics();
        g.setColor(view.getScene().getSelectionColor());
        view.transformToPixels(getPositiveCoordBox(), pixelbox);
        g.drawRect(pixelbox.x, pixelbox.y,
                pixelbox.width - 1, pixelbox.height - 1);

        g.drawRect(pixelbox.x + 1, pixelbox.y + 1,
                pixelbox.width - 3, pixelbox.height - 3);
    }

    @Override
    public void setFont(Font f) {
        this.fnt = f;
    }

    @Override
    public Font getFont() {
        return this.fnt;
    }
}
