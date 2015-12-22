/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.context.menu.model;

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;

/**
 *
 * @author dcnorris
 */
public class AnnotationContextEvent {

    private final List<SeqSymmetry> selectedItems;

    public AnnotationContextEvent(List<SeqSymmetry> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public List<SeqSymmetry> getSelectedItems() {
        return selectedItems;
    }

}
