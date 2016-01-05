/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import java.awt.Graphics;

/**
 *
 * @author Tarun
 */
public class PointedFillRectGlyph extends FillRectGlyph {

    int x[] = new int[6];
    int y[] = new int[6];

    @Override
    public void draw(ViewI view) {
        view.transformToPixels(this.getCoordBox(), this.getPixelBox());
        if (this.getPixelBox().width == 0) {
            this.getPixelBox().width = 1;
        }
        if (this.getPixelBox().height == 0) {
            this.getPixelBox().height = 1;
        }
        Graphics g = view.getGraphics();
        g.setColor(getBackgroundColor());
        int halfThickness = 1;
        halfThickness = (getPixelBox().height - 1) / 2;
        x[0] = getPixelBox().x;
        x[2] = getPixelBox().x + getPixelBox().width;
        x[1] = Math.max(x[0] + 1, (x[2] - halfThickness));
        x[3] = x[1] - 1;
        x[4] = x[0];
        y[0] = getPixelBox().y;
        y[1] = y[0];
        y[2] = y[0] + halfThickness;
        y[3] = y[0] + getPixelBox().height;
        y[4] = y[3];
        g.fillPolygon(x, y, 5);
    }

}
