/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.event;

import com.affymetrix.genometry.event.GenericAction;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;

/**
 *
 * @author dcnorris
 */
public class ZoomOutEvent extends GenericAction {

    final Adjustable adj;

    public ZoomOutEvent(Adjustable adj) {
        super("", "16x16/actions/list-remove.png", null);
        this.adj = adj;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
    }

}
