/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot.view;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import static com.affymetrix.genoviz.util.NeoConstants.NORTH;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tarun
 */
public class ProtannotEfficientLabelledLineGlyph extends EfficientLabelledLineGlyph {

    String label;
    private static final int maxFontSize = 36;
    private static final int minFontSize = 7;

    private static final Map<Integer, Font> fontSizeReference = new HashMap<Integer, Font>();

    static {
        fontSizeReference.put(7, new Font(Font.MONOSPACED, Font.PLAIN, 9));
        fontSizeReference.put(8, new Font(Font.MONOSPACED, Font.PLAIN, 10));
        fontSizeReference.put(9, new Font(Font.MONOSPACED, Font.PLAIN, 11));
        fontSizeReference.put(10, new Font(Font.MONOSPACED, Font.PLAIN, 12));
        fontSizeReference.put(11, new Font(Font.MONOSPACED, Font.PLAIN, 14));
        fontSizeReference.put(12, new Font(Font.MONOSPACED, Font.PLAIN, 14));
        fontSizeReference.put(13, new Font(Font.MONOSPACED, Font.PLAIN, 15));
        fontSizeReference.put(14, new Font(Font.MONOSPACED, Font.PLAIN, 17));
        fontSizeReference.put(15, new Font(Font.MONOSPACED, Font.PLAIN, 18));
        fontSizeReference.put(16, new Font(Font.MONOSPACED, Font.PLAIN, 19));
        fontSizeReference.put(17, new Font(Font.MONOSPACED, Font.PLAIN, 21));
        fontSizeReference.put(18, new Font(Font.MONOSPACED, Font.PLAIN, 21));
        fontSizeReference.put(19, new Font(Font.MONOSPACED, Font.PLAIN, 22));
        fontSizeReference.put(20, new Font(Font.MONOSPACED, Font.PLAIN, 23));
        fontSizeReference.put(21, new Font(Font.MONOSPACED, Font.PLAIN, 25));
        fontSizeReference.put(22, new Font(Font.MONOSPACED, Font.PLAIN, 26));
        fontSizeReference.put(23, new Font(Font.MONOSPACED, Font.PLAIN, 27));
        fontSizeReference.put(24, new Font(Font.MONOSPACED, Font.PLAIN, 28));
        fontSizeReference.put(25, new Font(Font.MONOSPACED, Font.PLAIN, 29));
        fontSizeReference.put(26, new Font(Font.MONOSPACED, Font.PLAIN, 30));
        fontSizeReference.put(27, new Font(Font.MONOSPACED, Font.PLAIN, 32));
        fontSizeReference.put(28, new Font(Font.MONOSPACED, Font.PLAIN, 33));
        fontSizeReference.put(29, new Font(Font.MONOSPACED, Font.PLAIN, 34));
        fontSizeReference.put(30, new Font(Font.MONOSPACED, Font.PLAIN, 34));
        fontSizeReference.put(31, new Font(Font.MONOSPACED, Font.PLAIN, 36));
        fontSizeReference.put(32, new Font(Font.MONOSPACED, Font.PLAIN, 37));
        fontSizeReference.put(33, new Font(Font.MONOSPACED, Font.PLAIN, 38));
        fontSizeReference.put(34, new Font(Font.MONOSPACED, Font.PLAIN, 40));
        fontSizeReference.put(35, new Font(Font.MONOSPACED, Font.PLAIN, 41));
        fontSizeReference.put(36, new Font(Font.MONOSPACED, Font.PLAIN, 41));
        fontSizeReference.put(37, new Font(Font.MONOSPACED, Font.PLAIN, 43));
        fontSizeReference.put(38, new Font(Font.MONOSPACED, Font.PLAIN, 44));
        fontSizeReference.put(39, new Font(Font.MONOSPACED, Font.PLAIN, 45));
        fontSizeReference.put(40, new Font(Font.MONOSPACED, Font.PLAIN, 46));
        fontSizeReference.put(41, new Font(Font.MONOSPACED, Font.PLAIN, 48));
        fontSizeReference.put(42, new Font(Font.MONOSPACED, Font.PLAIN, 48));
        fontSizeReference.put(43, new Font(Font.MONOSPACED, Font.PLAIN, 49));
        fontSizeReference.put(44, new Font(Font.MONOSPACED, Font.PLAIN, 51));
    }

    public ProtannotEfficientLabelledLineGlyph(String label) {
        super();
        this.label = label;
        setLabel(null);
        show_label = true;
        label_loc = NORTH;
        direction = RIGHT;
    }

    @Override
    public void draw(ViewI view) {
        super.draw(view);
        if (label != null && (label.length() > 0)) {
            Rectangle pixelbox = view.getScratchPixBox();
            drawLabel(pixelbox, view.getGraphics());
        }
    }

    private void drawLabel(Rectangle pixelbox, Graphics g) {
        Graphics g2 = g;
        String drawLabel = label;
        int labelGap = pixelbox.height / 4;
        int ypixPerChar = ((pixelbox.height / 2) - labelGap);

        if (ypixPerChar > maxFontSize) {
            ypixPerChar = maxFontSize;
        }
        if (ypixPerChar < minFontSize) {
            ypixPerChar = minFontSize;
        }
        g2.setFont(fontSizeReference.get(ypixPerChar));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(drawLabel);
        while (textWidth > pixelbox.width && drawLabel.length() > 3) {
            drawLabel = drawLabel.substring(0, drawLabel.length() - 2) + "\u2026";
            textWidth = fm.stringWidth(drawLabel);
        }
        int textHeight = fm.getAscent();
        if (textWidth <= pixelbox.width && textHeight <= (labelGap + pixelbox.height / 2)) {
            int xpos = pixelbox.x + (pixelbox.width / 2) - (textWidth / 2);
            g2.setPaintMode(); //added to fix bug with collapsed or condensed track labels
            g2.drawString(drawLabel, xpos, pixelbox.y + pixelbox.height / 2 - labelGap);
        }
    }

}
