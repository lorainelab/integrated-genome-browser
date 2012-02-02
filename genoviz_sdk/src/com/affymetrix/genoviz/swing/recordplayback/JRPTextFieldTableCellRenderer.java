package com.affymetrix.genoviz.swing.recordplayback;

import com.affymetrix.genoviz.swing.TableCellEditorRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;

public class JRPTextFieldTableCellRenderer extends DefaultCellEditor
		implements TableCellRenderer, TableCellEditorRenderer {

	private JRPTextField jrpTextField;
	private static final Border ActiveEditingBorder = new CompoundBorder(new LineBorder(Color.black, 1), new EmptyBorder(0, 2, 0, 2));
	private static final Border PassiveBorder = new EmptyBorder(1, 3, 1, 3);

	public JRPTextFieldTableCellRenderer(String id, String text) {
		super(new JRPTextField(id,text));
		this.jrpTextField = new JRPTextField(id,text);
		this.jrpTextField.setBackground(Color.white);
		this.jrpTextField.setHorizontalAlignment(JRPTextField.CENTER);
		this.jrpTextField = ((JRPTextField) this.editorComponent);

		this.jrpTextField.addFocusListener(new FocusListener() {

			public void focusGained(FocusEvent e) {
				JRPTextFieldTableCellRenderer.this.jrpTextField.setBorder(JRPTextFieldTableCellRenderer.ActiveEditingBorder);
			}

			public void focusLost(FocusEvent e) {
				JRPTextFieldTableCellRenderer.this.jrpTextField.setBorder(JRPTextFieldTableCellRenderer.PassiveBorder);
			}
		});
		this.jrpTextField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JRPTextFieldTableCellRenderer.this.stopCellEditing();
			}
		});
		this.jrpTextField.setBorder(PassiveBorder);

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