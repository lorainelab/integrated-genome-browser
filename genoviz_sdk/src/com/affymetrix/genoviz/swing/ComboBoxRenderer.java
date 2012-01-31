package com.affymetrix.genoviz.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class ComboBoxRenderer extends DefaultCellEditor
		implements TableCellEditor, TableCellRenderer, TableCellEditorRenderer {

	public JComboBox combobox;  //public because it must sometimes be set manually to the correct selected index
	private int cancelWhenZero = 0;
	private JPanel panel;

	public ComboBoxRenderer(Object[] options) {
		super(new JComboBox(options));
		setClickCountToStart(1);
		this.combobox = ((JComboBox) getComponent());
		this.combobox.setBackground(Color.white);
		this.combobox.setOpaque(true);
		this.combobox.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.combobox.setFocusable(false);
		this.combobox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (ComboBoxRenderer.this.cancelWhenZero == 0) {
					ComboBoxRenderer.this.stopCellEditing();
				}
			}
		});
		this.panel = new JPanel();
		this.panel.setOpaque(false);
		this.panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = 10;
		c.fill = 1;
		c.gridx = c.gridy = 0;
		c.gridwidth = c.gridheight = 1;
		c.weightx = c.weighty = 1.0D;
		c.insets.set(2, 3, 2, 3);
		this.panel.add(this.combobox, c);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		this.cancelWhenZero += 1;
		this.combobox.setSelectedItem(value);
		this.cancelWhenZero -= 1;
		return this.panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		this.cancelWhenZero += 1;
		this.combobox.setSelectedItem(value);
		this.cancelWhenZero -= 1;
		return this.panel;
	}

	@Override
	public Object getCellEditorValue() {
		return this.combobox.getSelectedItem();
	}

	@Override
	public boolean stopCellEditing() {
		if (this.cancelWhenZero == 0) {
			return super.stopCellEditing();
		}

		return false;
	}

	public boolean isFullyEngaged() {
		return this.combobox.isPopupVisible();
	}

	@Override
	public void cancelCellEditing() {
		if (this.cancelWhenZero == 0) {
			super.cancelCellEditing();
		}
	}
}

abstract interface TableCellEditorRenderer extends TableCellEditor {

	abstract boolean isFullyEngaged();
}