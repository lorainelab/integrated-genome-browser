/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import com.affymetrix.genoviz.bioviews.ViewI;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStroke0;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStroke1;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStroke2;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

/**
 *
 * @author Tarun
 */
public class LineContainerDashGlyph extends LineContainerProtAnnotGlyph {

    @Override
    public void draw(ViewI view) {
        Rectangle pixelBox = view.getScratchPixBox();
        view.transformToPixels(this.getCoordBox(), pixelBox);
        pixelBox = optimizeBigRectangleRendering(view, pixelBox);
        if (pixelBox.width == 0) {
            pixelBox.width = 1;
        }
        if (pixelBox.height == 0) {
            pixelBox.height = 1;
        }
        Graphics g = view.getGraphics();
        g.setColor(getBackgroundColor());

        drawDirectedLine(view.getGraphics(), pixelBox.x, pixelBox.y + (pixelBox.height / 2), pixelBox.width);
    }

    static void drawDirectedLine(Graphics g, final int x, final int y, final int width) {
        Graphics2D g2R = (Graphics2D) g;
        Stroke old_strokeR = g2R.getStroke();
        g2R.setStroke(dashStroke0);
        g2R.drawLine(x, y, x + width - 2, y);
        g2R.setStroke(dashStroke1);
        g2R.drawLine(x, y + 1, x + width - 2, y + 1);
        g2R.drawLine(x, y - 1, x + width - 2, y - 1);
        g2R.setStroke(dashStroke2);
        g2R.drawLine(x, y + 2, x + width - 2, y + 2);
        g2R.drawLine(x, y - 2, x + width - 2, y - 2);
        g2R.setStroke(old_strokeR);
    }

}
