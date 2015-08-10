/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.tieredmap.GlyphSearchNode;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;
import com.google.common.base.Strings;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
public class ProtAnnotMapTierGlyph extends MapTierGlyph {

    private static final Logger logger = LoggerFactory.getLogger(ProtAnnotMapTierGlyph.class);

    public ProtAnnotMapTierGlyph() {
        super();
        setSpacer(5);
    }

    @Override
    public void draw(ViewI view) {
        showLabel = false; // a hack to prevent Parent MapTierGlyph to draw the label
        super.draw(view);
        showLabel = true;
        GlyphI child = getChildren().get(0);
        Graphics2D g = view.getGraphics();

        // No font is readable at less than 5 pixels!
        FontMetrics fm = g.getFontMetrics();
        // 0.8 is a kludge, but getAscent() overestimates the amount
        // of space needed for normal capital letters; it includes
        // room for weirdly tall characters like '|' and accents.
        int fontSize = (int) (0.4 * (fm.getAscent() + fm.getDescent()));
        int textYPos = getPixelBox().y + (int) (0.4 * fm.getAscent());
        int textXPos = (child.getPixelBox(view).x + child.getPixelBox(view).width) / 2;
        int xCoord = (int) (child.getCoordBox().x + child.getCoordBox().width / 2);
        Point2D.Double labelStartCoord = new Point2D.Double(xCoord, 0);
        Point labelStartPoint = new Point();
        view.transformToPixels(labelStartCoord, labelStartPoint);
        int bottom = getPixelBox().y + getPixelBox().height - fm.getDescent();
        if (outline_color != null) {
            textYPos += 2;
            bottom -= 1;
        }
        g.setColor(label_color);
        drawlabel(view, (int) labelStartPoint.getX(), textYPos);
        if (moreStrings != null) {
            if (label_spacing == -1) {
                label_spacing = fontSize + 2;
            }
            for (String moreString : moreStrings) {
                textYPos += label_spacing;
                if (textYPos >= bottom) {
                    break;
                }
                g.drawString(moreString, textXPos, textYPos);
            }
        }

    }

    /**
     * Remove all children of the glyph
     */
    @Override
    public void removeChildren() {

        try {

            Field gsnField = MapTierGlyph.class.getDeclaredField("gsn");
            gsnField.setAccessible(true);
            GlyphSearchNode gsn = (GlyphSearchNode) gsnField.get(this);

            List kids = this.getChildren();

            if (kids != null) {
                Iterator iterator = kids.iterator();
                while (iterator.hasNext()) {
                    Object kid = iterator.next();
                    int last_removed_position = getChildren().indexOf((GlyphI) kid);
                    Field lrpField = MapTierGlyph.class.getDeclaredField("last_removed_position");
                    lrpField.setAccessible(true);
                    lrpField.setInt(this, last_removed_position);
                    lrpField.setAccessible(false);
                    gsn.removeGlyph((GlyphI) kid);
                    iterator.remove();
                    //this.removeChild((GlyphI) kid);
                }
            }
            gsn.removeChildren();
            // CLH: This is a hack. Instead of removing gsn,
            // I just assign a new one. Is this a massive leak???
            //
            // EEE: Yes, so I added the gsn.removeChildren() to help.
            //gsn = new GlyphSearchNode();
            gsnField.set(this, new GlyphSearchNode());

            gsnField.setAccessible(false);

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void drawlabel(ViewI view, int x, int y) {
        Graphics2D g = view.getGraphics();
        GlyphI child = getChild(0);
        if (child == null || !showLabel || Strings.isNullOrEmpty(label)) {
            return;
        }
        FontMetrics fontMetrics = g.getFontMetrics();
        int textHeight = fontMetrics.getAscent();
        int availableHeight = getPixelBox().height - child.getPixelBox(view).height;
        int availableWidth = child.getPixelBox(view).width;

        int bestFontSize = (int) (0.4 * (fontMetrics.getAscent() + fontMetrics.getDescent()));
        int temp = 0;
        int testHeight = 0;
        if (textHeight < availableHeight) {
            do {
                testHeight = g.getFontMetrics(new Font(fontMetrics.getFont().getName(), fontMetrics.getFont().getStyle(), bestFontSize)).getHeight();
                temp = bestFontSize;
                bestFontSize++;
            } while (testHeight < availableHeight);
            bestFontSize = temp;
        } else {
            do {
                testHeight = g.getFontMetrics(new Font(fontMetrics.getFont().getName(), fontMetrics.getFont().getStyle(), bestFontSize)).getHeight();
                bestFontSize--;
            } while (testHeight > availableHeight);
        }
        g.setFont(new Font(fontMetrics.getFont().getName(), fontMetrics.getFont().getStyle(), bestFontSize));
        fontMetrics = g.getFontMetrics();
        String drawLabel = label;

        int textWidth = fontMetrics.stringWidth(drawLabel);
        while (textWidth > availableWidth) {
            drawLabel = drawLabel.substring(0, drawLabel.length() - 2) + "\u2026";
            textWidth = fontMetrics.stringWidth(drawLabel);
        }

        g.drawString(drawLabel, x - textWidth / 2, y);
    }

}