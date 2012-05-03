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

import com.affymetrix.igb.color.ColorScheme;
import com.affymetrix.igb.color.ColorSchemeComboBox;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.Action;
import javax.swing.JOptionPane;

/**
 * @author blossome
 */
public class ColorSchemeChoiceAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ColorSchemeChoiceAction ACTION;

	public static ColorSchemeChoiceAction getAction() {
		if (ACTION == null) {
			ACTION = new ColorSchemeChoiceAction();
		}
		return ACTION;
	}

    private ItemListener listener;
    private ItemEvent happening = null;
    private ColorSchemeComboBox b = new ColorSchemeComboBox();
	private ColorScheme choice = null;
	private AffyLabelledTierMap ltm = null;


	/**
	 * Create a default action for selecting a color scheme.
	 * The icons are by Mark James
	 * of <a href="http://www.famfamfam.com/about/">Fam Fam Fam</a>.
	 */
	public ColorSchemeChoiceAction() {
		super("Color Scheme...",
				"16x16/actions/colorschemechooser.png",
				"22x22/actions/colorschemechooser.png");
		putValue(Action.SHORT_DESCRIPTION,
				"Choose a color scheme for the selected tracks.");
		AffyTieredMap m = this.getSeqMapView().getSeqMap();
        b.setChoices(0);
		if (m instanceof AffyLabelledTierMap) {
			this.ltm = (AffyLabelledTierMap) m;
			ltm.addListSelectionListener(b);
		}
	}

	public ColorSchemeChoiceAction(String theName, ItemListener theListener) {
		// TODO Change the mnemonic to something more appropriate.
        super(theName, KeyEvent.VK_L);
        this.listener = theListener;
    }

	/**
	 * Convert from an HTML entity string into a Java color object.
	 * @param theEntity an HTML entity like "#F0F0F0"
	 * @return the corresponding RGB color.
	 */
	private Color newColor(String theEntity) {
		int i = 0xFFFFFF;
		if ('#' != theEntity.charAt(0)) {
			theEntity = ColorScheme.SVGColors.get(theEntity);
		}
		if ('#' == theEntity.charAt(0)) {
			i = Integer.parseInt(theEntity.substring(1), 16);
		}
		Color c = new Color(i);
		return c;
	}

    ItemListener middleMan = new ItemListener() {

        @Override
        public void itemStateChanged(ItemEvent ie) {
            switch (ie.getStateChange()) {
            case ItemEvent.DESELECTED:
                break;
            case ItemEvent.SELECTED:
                Object o = ie.getItem();
                ColorScheme s = ColorScheme.ELISE;
                String str = (String) o;
                int i = str.lastIndexOf(' ');
                if (0 < i) {
                    s = ColorScheme.valueOf(str.substring(i+1));
                }
				ColorSchemeChoiceAction.this.choice = s;
                break;
            default:
                System.err.println("SchemeChoser.$ItemListener.itemStateChanged: Unexpected state change: " + ie.getStateChange());
            }
        }
        
    };

    @Override
    public void actionPerformed(ActionEvent e) {
        //b.addItemListener(this.listener);
        b.addItemListener(this.middleMan);
        int ok = JOptionPane.showConfirmDialog(null, b, "Pick a color scheme.",
				JOptionPane.OK_CANCEL_OPTION);
        switch (ok) {
        case JOptionPane.OK_OPTION:
			ColorScheme s = this.choice;
            if (null != s) {
                Color bg = newColor(s.getBackground());
				List<TierLabelGlyph> l = ltm.getOrderedTierLabels();
				int j = 0;
				int colors = s.numberOfForegrounds();
				for (int i = 0; i < l.size(); i++) {
					TierLabelGlyph tlg = l.get(i);
					TierGlyph g = tlg.getReferenceTier();
					if (tlg.isSelected()) {
						Color c = newColor(s.getForeground(j));
						g.getAnnotStyle().setBackground(bg);
						g.getAnnotStyle().setForeground(c);
						//g.setFillColor(c);
						j = (j + 1) % colors;
					}
				}
				refreshMap(false, false);
				// None of the three above updates work.
				// Data Management Table is updating fully when FG loses focus.
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
