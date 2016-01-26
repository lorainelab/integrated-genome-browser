/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.context.menu.model;

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;

/**
 * ## AnnotationContextEvent
 * A class containing contextual information about the event right
 * click event (e.g. selection information).
 * @author dcnorris
 * @module.info context-menu-api
 */
public class AnnotationContextEvent {

    private final List<SeqSymmetry> selectedItems;

    public AnnotationContextEvent(List<SeqSymmetry> selectedItems) {
        this.selectedItems = selectedItems;
    }

    /**
     *
     * @return list of currently selected SeqSymmetry objects
     */
    public List<SeqSymmetry> getSelectedItems() {
        return selectedItems;
    }

}
