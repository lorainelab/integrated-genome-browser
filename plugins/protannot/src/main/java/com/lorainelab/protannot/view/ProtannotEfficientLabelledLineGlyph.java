/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;

/**
 *
 * @author Tarun
 */
public class ProtannotEfficientLabelledLineGlyph extends EfficientLabelledLineGlyph {
    
    public ProtannotEfficientLabelledLineGlyph(String label) {
        super();
        setLabel(label);
        show_label = true;
        label_loc = NORTH;
        direction = RIGHT;
    }
}
