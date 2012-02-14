/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genoviz.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 *
 * @author lorainelab
 */
public class StyledJTable extends JTable implements MouseListener {

	private static final long serialVersionUID = 1L;

	public StyledJTable(TableModel tm) {
		super(tm);
		init();
	}

	private void init() {
		setCellSelectionEnabled(false);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		setFocusable(false);
		getSelectionModel().setSelectionMode(0);

		setOpaque(true);
		setBackground(Color.white);
		setIntercellSpacing(new Dimension(1, 1));
		setShowGrid(true);
		setGridColor(new Color(11184810));
		setRowHeight(20);


		JTableHeader header = getTableHeader();
		header.setBorder(new PartialLineBorder(Color.black, 1, "B"));
		header.setForeground(Color.black);
		header.setBackground(Color.white);
		header.setReorderingAllowed(false);
		header.setResizingAllowed(true);

		setAutoscrolls(true);
		setRequestFocusEnabled(false);
	}

	@Override
	public Component prepareRenderer(TableCellRenderer tcr, int i, int i2) {
		Component component = super.prepareRenderer(tcr, i, i2);
		return setComponentBackground(component, i, i2);
	}

	@Override
	public Component prepareEditor(TableCellEditor tce, int i, int i2) {
		Component component = super.prepareEditor(tce, i, i2);
		return setComponentBackground(component, i, i2);
	}

	private Component setComponentBackground(Component c, int i, int i2) {
		if (isCellEditable(i, i2)) {
			c.setBackground(Color.WHITE);
		} else {
			c.setBackground(new Color(235, 235, 235));
		}
		return c;
	}

	public void stopCellEditing() {
		TableCellEditor tce = getCellEditor();
		if (tce != null) {
			tce.cancelCellEditing();
		}
	}

	public void mouseEntered(MouseEvent e) {
		switchEditors(e);
	}

	public void mouseExited(MouseEvent e) {
		stopCellEditing();
	}

	public void mouseClicked(MouseEvent e) {
		switchEditors(e);
	}

	public void mousePressed(MouseEvent e) {
		switchEditors(e);
	}

	public void mouseReleased(MouseEvent e) {
		//do nothing
	}

	private void switchEditors(MouseEvent paramMouseEvent) {
		Point point = paramMouseEvent.getPoint();
		if (point != null) {
			int rowIndex = rowAtPoint(point);
			int columnIndex = columnAtPoint(point);
			if ((rowIndex != getEditingRow()) || (columnIndex != getEditingColumn())) {
				if (isEditing()) {
					TableCellEditor tce = getCellEditor();
					if (((tce instanceof TableCellEditorRenderer)) && (!((TableCellEditorRenderer) tce).isFullyEngaged())
							&& (!tce.stopCellEditing())) {
						tce.cancelCellEditing();
					}
				}
				if ((!isEditing())
						&& (rowIndex != -1) && (isCellEditable(rowIndex, columnIndex))) {
					editCellAt(rowIndex, columnIndex);
				}
			}
		}
	}
}
