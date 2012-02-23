package com.affymetrix.genoviz.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author hiralv
 */
public abstract class JTextButtonCellRenderer extends AbstractCellEditor implements
		TableCellEditor, ActionListener, TableCellRenderer, MouseListener {

	public static final long serialVersionUID = 1l;
	protected final JLabel field;
	protected final JButton button;
	protected final JPanel panel;
	protected final JFrame frame;
	protected String temp;

	public JTextButtonCellRenderer(final JFrame frame) {
		super();
		panel = new JPanel();
		field = new JLabel();
		button = getButton();
		this.frame = frame;
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

		panel.setBackground(Color.WHITE);
	}

	protected abstract JButton getButton();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (field == null || value == null) {
			return null;
		}
		field.setText(value.toString());

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
