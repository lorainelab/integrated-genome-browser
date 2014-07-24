/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.awt.AdjustableJSlider;
import javax.swing.JScrollBar;

/**
 *
 * @author dcnorris
 */
public class CustomLabelledTierMap extends AffyLabelledTierMap {

    private AdjustableJSlider xzoomer;
    private AdjustableJSlider yzoomer;

    public CustomLabelledTierMap(boolean hscroll_show, boolean vscroll_show) {
        super(hscroll_show, vscroll_show);
        initZoomers();
    }

    private void initZoomers() {
        xzoomer = new AdjustableJSlider(JScrollBar.HORIZONTAL);
        yzoomer = new AdjustableJSlider(JScrollBar.VERTICAL);
        this.setRangeZoomer(xzoomer);
        this.setOffsetZoomer(yzoomer);
    }

    public AdjustableJSlider getXzoomer() {
        return xzoomer;
    }

    public AdjustableJSlider getYzoomer() {
        return yzoomer;
    }

}
