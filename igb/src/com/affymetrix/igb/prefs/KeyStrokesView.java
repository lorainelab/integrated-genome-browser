/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.prefs;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.shared.StyledJTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * A panel that shows the preferences mapping between KeyStroke's and Actions.
 */
public final class KeyStrokesView implements ListSelectionListener,
		PreferenceChangeListener {

	public final KeyStrokeViewTable table = new KeyStrokeViewTable();
	public static final KeyStrokeViewTableModel model = new KeyStrokeViewTableModel();
	;
	public static final int KeyStrokeColumn = 1;
	public static final int ToolbarColumn = 2;
	public static final int IdColumn = 3; // not displayed in table
	private final ListSelectionModel lsm;
	// private final TableRowSorter<DefaultTableModel> sorter;
	public KeyStrokeEditPanel edit_panel = null;
	private static KeyStrokesView singleton;
	private int selected = -1;

	public static synchronized KeyStrokesView getSingleton() {
		if (singleton == null) {
			singleton = new KeyStrokesView();
		}
		return singleton;
	}

	private KeyStrokesView() {
		super();
		lsm = table.getSelectionModel();
		lsm.addListSelectionListener(this);
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.setModel(model);
		table.setRowHeight(20);

		edit_panel = new KeyStrokeEditPanel();
		edit_panel.setEnabled(false);

		try {
			PreferenceUtils.getKeystrokesNode().flush();
		} catch (Exception e) {
		}
		PreferenceUtils.getKeystrokesNode().addPreferenceChangeListener(this);

		refresh();
	}

	private static String getSortField(GenericAction genericAction) {
		if (genericAction == null) {
			return "";
		}
		return genericAction.getDisplay() + (genericAction.isToggle() ? "1" : "2");
	}

	private static TreeSet<String> filterActions() {
		List<String> keys;
		synchronized(GenericActionHolder.getInstance()) {
			keys = new ArrayList<String>(GenericActionHolder.getInstance().getGenericActionIds());
		}
		TreeSet<String> actions = new TreeSet<String>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				GenericAction ga1 = GenericActionHolder.getInstance().getGenericAction(o1);
				if (ga1 == null) {
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "no GenericAction found for \"" + o1 + "\"");
				}
				GenericAction ga2 = GenericActionHolder.getInstance().getGenericAction(o2);
				if (ga2 == null) {
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "no GenericAction found for \"" + o2 + "\"");
				}
				return getSortField(ga1).compareTo(getSortField(ga2));
			}
		});
		for (String key : keys) {
			GenericAction genericAction = GenericActionHolder.getInstance().getGenericAction(key);
			boolean hasGetAction;
			try {
				Class<?> actionClass = genericAction.getClass();
				actionClass.getMethod("getAction");
				hasGetAction = true;
			}
			catch (NoSuchMethodException x) {
				hasGetAction = false;
			}
			if (hasGetAction && genericAction.getDisplay() != null && !"".equals(genericAction.getDisplay())) {
				actions.add(key);
			}
		}
		return actions;
	}

    /**
     * build the underlying data array - there is a fourth column, not shown in the
     * table, but needed by the seetValue() method
     * @param keystroke_node
     * @param toolbar_node
     * @return
     */
    private static Object[][] buildRows(Preferences keystroke_node, Preferences toolbar_node) {
    	TreeSet<String> keys = filterActions();//PreferenceUtils.getKeystrokesNodeNames();
		Object[][] rows;

		synchronized (keys) {
			int num_rows = keys.size();
			int num_cols = 4;
			rows = new Object[num_rows][num_cols];
			Iterator<String> iter = keys.iterator();
			for (int i = 0; iter.hasNext(); i++) {
				String key = iter.next();
				GenericAction genericAction = GenericActionHolder.getInstance().getGenericAction(key);
				if (genericAction == null) {
					Logger.getLogger(KeyStrokesView.class.getName()).log(Level.WARNING, "!!! no GenericAction for key = " + key);
				}
				rows[i][0] = (genericAction == null) ? "???" : genericAction.getDisplay();
				rows[i][1] = keystroke_node.get(key, "");
				rows[i][2] = toolbar_node.getBoolean(key, false);
				rows[i][3] = (genericAction == null) ? "" : genericAction.getId(); // not displayed
			}
		}
		return rows;
	}

	/**
	 * Re-populates the table with the shortcut data.
	 */
	private void refresh() {
		Object[][] rows = null;
		rows = buildRows(PreferenceUtils.getKeystrokesNode(), PreferenceUtils.getToolbarNode());
		model.setRows(rows);
	}

	public void invokeRefreshTable() { //Should fix the problems associated with updating entire table at every preference change.
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				refresh();
				model.fireTableDataChanged();
				if (selected > 0) {
					table.setRowSelectionInterval(selected, selected);
				}
			}
		});

	}

	/**
	 * This is called when the user selects a row of the table;
	 */
	public void valueChanged(ListSelectionEvent evt) {
		if (evt.getSource() == lsm && !evt.getValueIsAdjusting()) {
			int srow = table.getSelectedRow();
			if (srow >= 0) {
				String id = (String) table.getModel().getValueAt(srow, 0);
				editKeystroke(id);
			} else {
				edit_panel.setPreferenceKey(null, null, null, null);
			}
		}
	}

	private void editKeystroke(String id) {
		edit_panel.setPreferenceKey(PreferenceUtils.getKeystrokesNode(), PreferenceUtils.getToolbarNode(), id, "");
	}

	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getNode() != PreferenceUtils.getKeystrokesNode()) {
			return;
		}
		// Each time a keystroke preference is changed, update the
		// whole table.  Inelegant, but works. 
		invokeRefreshTable();
	}

	/*
	 * public void destroy() { removeAll(); if (lsm != null)
	 * {lsm.removeListSelectionListener(this);}
	 * PrefenceUtils.getKeystrokesNode().removePreferenceChangeListener(this);
	}
	 */
	class KeyStrokeViewTable extends StyledJTable {

		private static final long serialVersionUID = 1L;

		@Override
		public TableCellEditor getCellEditor(int row, int col) {
			DefaultCellEditor textEditor = new DefaultCellEditor(edit_panel.key_field);
			if (col == KeyStrokeColumn) {
				selected = row;
				return textEditor;
			}
			return super.getCellEditor(row, col);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int col) {
			TableCellRenderer renderer = super.getCellRenderer(row, col);
			if (col == ToolbarColumn) {
				((JCheckBox) renderer).setHorizontalAlignment(SwingConstants.CENTER);
			}
			else {
				((DefaultTableCellRenderer) renderer).setHorizontalAlignment(SwingConstants.LEFT);
			}
			return renderer;
		}
	}
}
