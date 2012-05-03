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
package com.affymetrix.igb.color;

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
	 * Restricts choices
	 * to only those schemes with the given number of foreground colors.
	 * @param theSchemeSize a value of 0 lets all the schemes show
	 *                      regardless of the number of foreground colors.
	 */
    public void setChoices(int theSchemeSize) {
        this.removeAllItems();
        for (ColorScheme n: ColorScheme.values()) {
            if (0 == theSchemeSize || theSchemeSize == n.numberOfForegrounds()) {
                StringBuilder sb = new StringBuilder("<html>");
                sb.append("<span style=\"background-color: ").append(n.getBackground()).append("\"> &nbsp; ");
                for (String c: n.getForegroundColors()) {
                    // http://www.unicode.org/charts/PDF/U2580.pdf
                    // http://en.wikipedia.org/wiki/Box-drawing_character
                    // Look for "Block Elements" in the above page.
                    sb.append("<span style=\"color: ").append(c).append("\"> &#9608; </span>");
                }
                sb.append(" &nbsp;</span> ").append(n);
                this.addItem(sb.toString());
            }
        }
        if (1 < this.getItemCount()) {
            this.setSelectedIndex(1);
            this.setSelectedIndex(0);
        }
    }

}
