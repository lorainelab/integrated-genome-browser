package com.affymetrix.igb.swing;

import com.affymetrix.genoviz.swing.TableCellEditorRenderer;
import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class JRPTextFieldTableCellRenderer extends DefaultCellEditor
		implements TableCellRenderer, TableCellEditorRenderer {

	private static final long serialVersionUID = 1L;
	private JRPTextField jrpTextField;

	public JRPTextFieldTableCellRenderer(String id, String text, Color fg, Color bg) {
		super(new JRPTextField(id, text));
		this.jrpTextField = new JRPTextField(id, text);
		this.jrpTextField = ((JRPTextField) this.editorComponent);
		this.jrpTextField.setForeground(fg);
		this.jrpTextField.setBackground(bg);
		this.jrpTextField.setHorizontalAlignment(JRPTextField.RIGHT);

		this.jrpTextField.addActionListener(e -> stopCellEditing());

		setClickCountToStart(1);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (jrpTextField != null && value != null) {
			this.jrpTextField.setText(value.toString());
			this.jrpTextField.setOpaque(true);
			return this.jrpTextField;
		} else {
			return null;
		}
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		this.jrpTextField.setText(value.toString());
		this.jrpTextField.setOpaque(true);
		return this.jrpTextField;
	}

	@Override
	public Object getCellEditorValue() {
		return this.jrpTextField.getText();
	}

	public boolean isFullyEngaged() {
		return this.jrpTextField.hasFocus();
	}
}
