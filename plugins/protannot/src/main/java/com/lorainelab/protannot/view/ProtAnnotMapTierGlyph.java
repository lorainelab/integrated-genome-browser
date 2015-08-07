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
    private static final int MIN_FONT_SIZE = 7;
    private static final int MAX_FONT_SIZE = 36;

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
        FontMetrics fm = g.getFontMetrics();
        g.setColor(label_color);
        drawlabel(view);
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

    private void drawlabel(ViewI view) {
        Graphics2D g = view.getGraphics();
        GlyphI child = getChild(0);
        if (child == null || !showLabel || Strings.isNullOrEmpty(label)) {
            return;
        }

        int xCoord = (int) (child.getCoordBox().x + child.getCoordBox().width / 2);
        Point2D.Double labelStartCoord = new Point2D.Double(xCoord, 0);
        Point labelStartPoint = new Point();
        view.transformToPixels(labelStartCoord, labelStartPoint);

        FontMetrics fontMetrics = g.getFontMetrics();
        int textHeight = fontMetrics.getAscent();
        int availableHeight = (getPixelBox().height - child.getPixelBox(view).height) / 2;
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
        if (bestFontSize < MIN_FONT_SIZE) {
            return;
        }
        if(bestFontSize > MAX_FONT_SIZE) {
            bestFontSize = MAX_FONT_SIZE;
        }
        g.setFont(new Font(fontMetrics.getFont().getName(), fontMetrics.getFont().getStyle(), bestFontSize));
        fontMetrics = g.getFontMetrics();
        String drawLabel = label;

        int textWidth = fontMetrics.stringWidth(drawLabel);
        while (textWidth > availableWidth && drawLabel.length() > 3) {
            drawLabel = drawLabel.substring(0, drawLabel.length() - 2) + "\u2026";
            textWidth = fontMetrics.stringWidth(drawLabel);
        }

        g.drawString(drawLabel, labelStartPoint.x - textWidth / 2, getPixelBox().y + fontMetrics.getAscent());
    }

}
