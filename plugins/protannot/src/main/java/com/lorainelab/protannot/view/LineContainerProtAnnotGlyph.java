/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.LineContainerGlyph;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 *
 * @author Tarun
 */
public class LineContainerProtAnnotGlyph extends LineContainerGlyph {
    
    @Override
    public void draw(ViewI view) {
        view.transformToPixels(getCoordBox(), getPixelBox());
        if (getPixelBox().width == 0) {
            getPixelBox().width = 1;
        }
        if (getPixelBox().height == 0) {
            getPixelBox().height = 1;
        }
        Graphics g = view.getGraphics();
        g.setColor(getBackgroundColor());

        Rectangle compbox = view.getComponentSizeRect();
        setPixelBox(getPixelBox().intersection(compbox));
    }
    
}
