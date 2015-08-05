/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot;

import com.affymetrix.genoviz.bioviews.ViewI;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStroke0;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStroke1;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStroke2;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStrokeNeg0;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStrokeNeg1;
import static com.affymetrix.genoviz.glyph.EfficientSolidGlyph.dashStrokeNeg2;
import com.affymetrix.genoviz.glyph.LineContainerGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

/**
 *
 * @author Tarun
 */
public class LineContainerDashGlyph extends LineContainerGlyph {

    @Override
    public void draw(ViewI view) {
        super.draw(view);
        Rectangle pixelbox = getPixelBox();
        drawDirectedLine(view.getGraphics(), pixelbox.x, pixelbox.y + (pixelbox.height / 2), pixelbox.width, NeoConstants.RIGHT);
    }
    
    static void drawDirectedLine(Graphics g, final int x, final int y, final int width, final int direction) {
        switch (direction) {
            case NeoConstants.RIGHT:
                Graphics2D g2R = (Graphics2D) g;
                Stroke old_strokeR = g2R.getStroke();
                g2R.setStroke(dashStroke0);
                g2R.drawLine(x, y, x + width, y);
                g2R.setStroke(dashStroke1);
                g2R.drawLine(x, y + 1, x + width, y + 1);
                g2R.drawLine(x, y - 1, x + width, y - 1);
                g2R.setStroke(dashStroke2);
                g2R.drawLine(x, y + 2, x + width, y + 2);
                g2R.drawLine(x, y - 2, x + width, y - 2);
                g2R.setStroke(old_strokeR);
                break;
            case NeoConstants.LEFT:
                Graphics2D g2L = (Graphics2D) g;
                Stroke old_strokeL = g2L.getStroke();
                g2L.setStroke(dashStrokeNeg0);
                g2L.drawLine(x, y, x + width, y);
                g2L.setStroke(dashStrokeNeg1);
                g2L.drawLine(x, y + 1, x + width, y + 1);
                g2L.drawLine(x, y - 1, x + width, y - 1);
                g2L.setStroke(dashStrokeNeg2);
                g2L.drawLine(x, y + 2, x + width, y + 2);
                g2L.drawLine(x, y - 2, x + width, y - 2);
                g2L.setStroke(old_strokeL);
                break;
            default:
                g.fillRect(x, y, width, 1);
        }
    }

}
