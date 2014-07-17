/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.swing;

import java.awt.Component;
import java.util.TreeMap;
import javax.swing.JPopupMenu;

/**
 * @author tkanapar
 */
public class JRPPopupMenu extends JPopupMenu {

    private static final long serialVersionUID = 1L;
    private final TreeMap<Integer, Component> popups = new TreeMap<Integer, Component>();

    public JRPPopupMenu() {
        super();
    }

    @Override
    public Component add(Component comp, int weight) {
        if (popups.containsKey(weight)) {
            popups.put(popups.lastKey() + 1, comp);
        } else {
            popups.put(weight, comp);
        }
        super.addImpl(comp, null, -1);
        return comp;
    }

    public TreeMap<Integer, Component> getPopupsMap() {
        return popups;
    }

    @Override
    public void removeAll() {
        popups.clear();
        super.removeAll();
    }

    
}
