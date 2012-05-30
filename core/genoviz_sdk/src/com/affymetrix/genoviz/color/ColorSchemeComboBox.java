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
package com.affymetrix.genoviz.color;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A combo box for selecting a color scheme.
 * It uses {@link ColorScheme} to get the schemes to display.
 * It can listen to a list selection and restrict the choice
 * to those schemes matching the number of items in the selection.
 * @author Eric Blossom
 */
public class ColorSchemeComboBox extends JComboBox implements ListSelectionListener {
	private static final long serialVersionUID = 1L;

	/**
	 * Action to automatically invoke upon selection
	 * so this can be used outside a dialog with an OK button.
	 */
	private Action a = null;
	private int limit = Integer.MAX_VALUE;

	/**
	 * React to a selection change
	 * by counting up the selected items
	 * and then setting the choices appropriately.
	 */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel m = null;
        Object o = e.getSource();
		if (o instanceof ListSelectionModel) {
			m = (ListSelectionModel) o;
		}
		else if (o instanceof JList) {
			JList l = (JList) o;
			m = l.getSelectionModel();
		}
		if (null != m) {
			int count = 0;
			for (int i = 0; i <= m.getMaxSelectionIndex(); i++) {
				if (m.isSelectedIndex(i)) {
					count += 1;
				}
			}
			setChoices(count);
		}
    }

	/**
	 * Color schemes with more foreground colors than this are screened out.
	 * @param theLimit cannot be negative.
	 */
	public void setLimit(int theLimit) {
		if (theLimit < 0) {
			throw new IllegalArgumentException("The limit cannot be negative.");
		}
		this.limit = theLimit;
	}

	/**
	 * Restricts choices
	 * to only those schemes with the given number of foreground colors.
	 * @param theSchemeSize a value of 0 lets all the schemes show
	 *                      regardless of the number of foreground colors.
	 */
    public void setChoices(int theSchemeSize) {
        this.removeAllItems();
		theSchemeSize = Math.min(theSchemeSize, this.limit);
        for (ColorScheme n: ColorScheme.values()) {
            if (0 == theSchemeSize || theSchemeSize == n.numberOfForegrounds()) {
                this.addItem(n);
            }
        }
        if (1 < this.getItemCount()) {
            this.setSelectedIndex(1);
            this.setSelectedIndex(0);
        }
    }

}
