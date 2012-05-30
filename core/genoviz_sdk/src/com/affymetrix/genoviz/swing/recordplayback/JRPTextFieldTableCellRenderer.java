package com.affymetrix.genoviz.swing.recordplayback;

import com.affymetrix.genoviz.swing.TableCellEditorRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;

public class JRPTextFieldTableCellRenderer extends DefaultCellEditor
		implements TableCellRenderer, TableCellEditorRenderer {
	private static final long serialVersionUID = 1L;
	private JRPTextField jrpTextField;

	public JRPTextFieldTableCellRenderer(String id, String text) {
		super(new JRPTextField(id,text));
		this.jrpTextField = new JRPTextField(id,text);
		this.jrpTextField = ((JRPTextField) this.editorComponent);
		this.jrpTextField.setBackground(Color.white);
		this.jrpTextField.setHorizontalAlignment(JRPTextField.RIGHT);

		this.jrpTextField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				stopCellEditing();
			}
		});

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