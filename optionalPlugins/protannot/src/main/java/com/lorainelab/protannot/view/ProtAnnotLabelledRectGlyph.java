/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.LabelledRectGlyph;
import static com.affymetrix.genoviz.glyph.LabelledRectGlyph.min_width_needed_for_text;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
public class ProtAnnotLabelledRectGlyph extends LabelledRectGlyph {

    private static final Logger logger = LoggerFactory.getLogger(ProtAnnotLabelledRectGlyph.class);

    @Override
    public void draw(ViewI view) {
        super.draw(view);
        if (getText() != null) {
            Graphics g = view.getGraphics();

            // CLH: Added a check to make sure there is at least _some_ room
            // before we start getting setting the font and checking metrics.
            // No need to do this on a 1 px wide rectangle!
            if (getPixelBox().width >= min_width_needed_for_text) {
                Font savefont = g.getFont();
                Font f2 = this.getFont();
                if (f2 != savefont) {
                    g.setFont(f2);
                } else {
                    // If they are equal, there's no need to restore the font
                    // down below.
                    savefont = null;
                }
                FontMetrics fm = g.getFontMetrics();

                int midline = getPixelBox().y + getPixelBox().height / 2;

                String drawLabel = getText();
                int textWidth = fm.stringWidth(drawLabel);
                while (textWidth > getPixelBox().width && drawLabel.length() > 3) {
                    drawLabel = drawLabel.substring(0, drawLabel.length() - 2) + "\u2026";
                    textWidth = fm.stringWidth(drawLabel);
                }

                int mid = getPixelBox().x + (getPixelBox().width / 2) - (textWidth / 2);
                // define adjust such that: ascent-adjust = descent+adjust
                int adjust = (int) ((fm.getAscent() - fm.getDescent()) / 2.0);
                g.setColor(this.getForegroundColor());
                g.drawString(drawLabel, mid, midline + adjust);

                if (null != savefont) {
                    g.setFont(savefont);
                }
            }
        }
    }
}
