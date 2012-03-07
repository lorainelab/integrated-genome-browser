package com.affymetrix.genoviz.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 *
 * @author david and nick
 */
public class StyledJTable extends JTable {

	private static final long serialVersionUID = 1L;
	// The list will save all the unchangeable column num
	public ArrayList<Integer> list = new ArrayList<Integer>();

	public StyledJTable(TableModel tm) {
		super(tm);
		init();
	}

	public StyledJTable() {
		super();
		init();
	}

	public StyledJTable(int numRows, int numColumns) {
		super(numRows, numColumns);
		init();
	}

	public StyledJTable(Object[][] rowData, Object[] columnNames) {
		super(rowData, columnNames);
		init();
	}

	public StyledJTable(TableModel dm, TableColumnModel cm) {
		super(dm, cm);
		init();
	}

	public StyledJTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
		super(dm, cm, sm);
		init();
	}

	@SuppressWarnings("rawtypes")
	public StyledJTable(Vector rowData, Vector columnNames) {
		super(rowData, columnNames);
		init();
	}

	private void init() {
		setCellSelectionEnabled(true);
		setSelectionForeground(Color.BLACK);

		setOpaque(false);
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

		TableCellEditor editor = this.getDefaultEditor(String.class);
		((DefaultCellEditor) editor).setClickCountToStart(1);
		this.setDefaultEditor(String.class, editor);
	}

	@Override
	public Component prepareRenderer(TableCellRenderer tcr, int r, int c) {
		Component component = super.prepareRenderer(tcr, r, c);
		return setComponentBackground(component, r, c);
	}

	@Override
	public Component prepareEditor(TableCellEditor tce, int r, int c) {
		Component component = super.prepareEditor(tce, r, c);
		return setComponentBackground(component, r, c);
	}

	public Component setComponentBackground(Component component, int r, int c) {
		if (!list.contains(c)) {
			if (isCellEditable(r, c)) {
				component.setBackground(Color.WHITE);
			} else {
				component.setBackground(new Color(235, 235, 235));
			}
		}

		return component;
	}

	public void stopCellEditing() {
		TableCellEditor tce = getCellEditor();
		if (tce != null) {
			tce.cancelCellEditing();
		}
	}
}
