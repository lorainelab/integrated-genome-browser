/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.genoviz.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * An editor to support the three valued logic of {@link ExistentialTriad}.
 * For things that can never be, a label with a hyphen is displayed
 * and it cannot be changed.
 * For is/is not a check box is use.
 * @author Eric Blossom
 */
public class SuperBooleanCellEditor extends AbstractCellEditor 
implements TableCellEditor, TableCellRenderer {

	JLabel lbl = new JLabel("-");
	JCheckBox bx = new JCheckBox();
	Component ed = bx;

	public SuperBooleanCellEditor() {
		this.lbl.setHorizontalAlignment(SwingConstants.CENTER);
		this.bx.setHorizontalAlignment(SwingConstants.CENTER);
		this.bx.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireEditingStopped(); // switch to renderer and recored the change.
			}
			
		});
	}

	@Override
	public Object getCellEditorValue() {
		if (this.ed instanceof JLabel) {
			return ExistentialTriad.CANNOTBE;
		}
		return this.bx.isSelected()? ExistentialTriad.IS: ExistentialTriad.ISNOT;
	}

	@Override
	public Component getTableCellEditorComponent(
			JTable table,
			Object value,
			boolean isSelected,
			int row, int column) {
		ExistentialTriad t = (ExistentialTriad) table.getValueAt(row, column);
		if (ExistentialTriad.CANNOTBE == t) {
			this.ed = this.lbl;
		}
		else {
			this.bx.setSelected(ExistentialTriad.IS == t);
			this.ed = this.bx;
		}
		return this.ed;
	}

	@Override
	public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean isSelected,
			boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	public void setHorizontalAlignment(int alignment) {
		this.lbl.setHorizontalAlignment(alignment);
		this.bx.setHorizontalAlignment(alignment);
	}

}
