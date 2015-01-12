/*  Copyright (c) 2012 Genentech, Inc.
 *
 *  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.action;

import com.affymetrix.genoviz.color.ColorScheme;
import com.affymetrix.genoviz.color.ColorSchemeComboBox;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import javax.swing.Action;
import javax.swing.JOptionPane;

/**
 * Presents a dialog from which a color scheme can be chosen. The schemes will
 * be appropriate for the number of tiers (tracks) selected. Each scheme
 * consists of a background color and one foreground color for each selected
 * tier.
 *
 * @author Eric Blossom
 */
public class ColorSchemeChoiceAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static ColorSchemeChoiceAction ACTION;

    /**
     * Get an (the) instance of a chooser.
     *
     * @return the same static instance every time.
     */
    public static ColorSchemeChoiceAction getAction() {
        if (ACTION == null) {
            ACTION = new ColorSchemeChoiceAction();
        }
        return ACTION;
    }

    private ColorSchemeComboBox picker = new ColorSchemeComboBox();
    private ColorScheme choice = null;
    private AffyLabelledTierMap ltm = null;

    /**
     * Create a default action for selecting a color scheme. The icons are by
     * Mark James of <a href="http://www.famfamfam.com/about/">Fam Fam Fam</a>.
     * This is private to force the use of {@link #getAction}.
     */
    private ColorSchemeChoiceAction() {
        super("Color Scheme...",
                "16x16/actions/colorschemechooser.png",
                "22x22/actions/colorschemechooser.png");
        putValue(Action.SHORT_DESCRIPTION,
                "Choose a color scheme for the selected tracks.");
        AffyTieredMap m = this.getSeqMapView().getSeqMap();
        picker.setChoices(0);
        if (m instanceof AffyLabelledTierMap) {
            this.ltm = (AffyLabelledTierMap) m;
            ltm.addListSelectionListener(picker);
        }
        picker.addItemListener(this.middleMan);
    }

    /**
     * Listens for the choice of a color scheme. Parses the choice and stores
     * the chosen scheme for later use if the users "OK"s the dialog.
     */
    private ItemListener middleMan = ie -> {
        switch (ie.getStateChange()) {
            case ItemEvent.DESELECTED:
                break;
            case ItemEvent.SELECTED:
                Object o = ie.getItem();
                ColorScheme s = ColorScheme.ELISE;
                if (o instanceof ColorScheme) {
                    s = (ColorScheme) o;
                } else {
                    String str = o.toString();
                    int i = str.lastIndexOf(' ');
                    if (0 < i) {
                        s = ColorScheme.valueOf(str.substring(i + 1));
                    }
                }
                ColorSchemeChoiceAction.this.choice = s;
                break;
            default:
                System.err.println(
                        "SchemeChoser.$ItemListener.itemStateChanged: Unexpected state change: "
                        + ie.getStateChange());
        }
    };

    /**
     * Pops up a dialog and sets colors based on the user's chosen scheme.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        int ok = JOptionPane.showConfirmDialog(null, picker, "Pick a color scheme.",
                JOptionPane.OK_CANCEL_OPTION);
        switch (ok) {
            case JOptionPane.OK_OPTION:
                ColorScheme s = this.choice;
                if (null != s) {
                    Color bg = Color.decode(s.getBackground());
                    List<TierLabelGlyph> l = ltm.getOrderedTierLabels();
                    int j = 0;
                    int colors = s.numberOfForegrounds();
            for (TierLabelGlyph tlg : l) {
                TierGlyph g = tlg.getReferenceTier();
                if (tlg.isSelected()) {
                    Color c = Color.decode(s.getForeground(j));
                    g.getAnnotStyle().setBackground(bg);
                    g.getAnnotStyle().setForeground(c);
                    j = (j + 1) % colors;
                }
            }
                    refreshMap(false, false);
                }
                break;
            case JOptionPane.CANCEL_OPTION:
                break;
            default:
                System.err.println(this.getClass().getName()
                        + ": BUG: Unexpected return from dialog: " + ok);
        }
    }

}
