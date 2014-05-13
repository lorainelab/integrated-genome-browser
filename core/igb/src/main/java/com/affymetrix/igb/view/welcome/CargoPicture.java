/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.welcome;

import be.pwnt.jflow.shape.Picture;
import java.awt.image.BufferedImage;

/**
 * Holds information on what to do once the user clicks on the icon
 *
 * @author jfvillal
 */
class CargoPicture extends Picture {

    private Object Cargo;

    CargoPicture(BufferedImage image) {
        super(image);
    }

    public Object getCargo() {
        return Cargo;
    }

    public void setCargo(Object Cargo) {
        this.Cargo = Cargo;
    }
}
