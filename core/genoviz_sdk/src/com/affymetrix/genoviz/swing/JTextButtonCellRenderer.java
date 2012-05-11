package com.affymetrix.genoviz.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author hiralv
 */
public abstract class JTextButtonCellRenderer extends AbstractCellEditor implements
		TableCellEditor, ActionListener, TableCellRenderer, UIResource, MouseListener {

	public static final long serialVersionUID = 1l;
	private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
	protected final JLabel field;
	protected final JButton button;
	public final JPanel panel;
	protected String temp;

	public JTextButtonCellRenderer() {
		super();
		panel = new JPanel();
		field = new JLabel();
		button = getButton();
		button.addActionListener(this);
		field.addMouseListener(this);
		panel.addMouseListener(this);

		field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;

		c.anchor = GridBagConstraints.LINE_START;
		panel.add(field, c);

		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.LINE_END;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add(button, c);
	}

	protected abstract JButton getButton();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (field == null || value == null) {
			return null;
		}
		field.setText(value.toString());

		if (!isSelected) {
			panel.setBorder(noFocusBorder);
		}

		return panel;
	}

	//Implement the one CellEditor method that AbstractCellEditor doesn't.
	public Object getCellEditorValue() {
		return temp;
	}

	public abstract void actionPerformed(ActionEvent e);

	//Implement the one method defined by TableCellEditor.
	public Component getTableCellEditorComponent(JTable table,
			Object value,
			boolean isSelected,
			int row,
			int column) {
		temp = value.toString();
		return panel;
	}

	public void mouseReleased(MouseEvent e) {
		fireEditingCanceled();
		panel.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}
}
