package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import static com.affymetrix.genoviz.util.NeoConstants.default_bold_font;
import java.awt.Font;


/**
 * Creating special class for BAM soft clipping glyph to be able to draw at low zoom.
 * 
 * @author lorainelab
 */
public class SoftClippingSeqGlyph extends SequenceGlyph {
    
    static final int MAX_CHAR_PIX = GeneralUtils.getFontMetrics(default_bold_font).stringWidth("G");
    static final int MIN_CHAR_PIX = 5;
    static final Font[] xpix2fonts = new Font[MAX_CHAR_PIX + 1];
    static final Font MONO_FONT = NeoConstants.default_bold_font;

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

    @Override
    protected void drawSelectedOutline(ViewI view) {
        super.drawSelectedOutline(view);
    }

}
