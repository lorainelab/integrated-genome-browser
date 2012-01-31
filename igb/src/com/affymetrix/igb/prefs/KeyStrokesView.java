/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *    
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.  
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.prefs;

import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import javax.swing.table.TableCellEditor;

/**
 *  A panel that shows the preferences mapping between KeyStroke's and Actions. 
 */
public final class KeyStrokesView implements ListSelectionListener,
		PreferenceChangeListener {

	private static final long serialVersionUID = 1L;
	public final KeyStrokeViewTable table = new KeyStrokeViewTable();
//  private final static String[] col_headings = {"Action", "Key Stroke", "Toolbar ?"};
	public static final KeyStrokeViewTableModel model = new KeyStrokeViewTableModel();;
	public static final int KeySrokeColumn = 1;
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

		//sorter = new TableRowSorter<KeyStrokeViewTableModel>(model);

		table.setModel(model);
		//table.setRowSorter(sorter);
		table.setRowSelectionAllowed(true);
		table.setEnabled(true);

		edit_panel = new KeyStrokeEditPanel();
		edit_panel.setEnabled(false);

		try {
			PreferenceUtils.getKeystrokesNode().flush();
		} catch (Exception e) {
		}
		PreferenceUtils.getKeystrokesNode().addPreferenceChangeListener(this);

		refresh();
	}

//  private static Object[][] buildRows(Preferences keystroke_node, Preferences toolbar_node) {
	private static Object[][] buildRows(Preferences keystroke_node) {
		Collection<String> keys = PreferenceUtils.getKeystrokesNodeNames();
		Object[][] rows;

		synchronized (keys) {
			int num_rows = keys.size();
//		int num_cols = 3;
			int num_cols = 2;
			rows = new Object[num_rows][num_cols];
			Iterator<String> iter = keys.iterator();
			for (int i = 0; iter.hasNext(); i++) {
				String key = iter.next();
				rows[i][0] = key;
				rows[i][1] = keystroke_node.get(key, "");
//			rows[i][2] = toolbar_node.getBoolean(key, false) ? "x" : "";
			}
		}
		return rows;
	}

	/** Re-populates the table with the shortcut data. */
	private void refresh() {
		Object[][] rows = null;
//    rows = buildRows(PreferenceUtils.getKeystrokesNode(), PreferenceUtils.getToolbarNode());
		rows = buildRows(PreferenceUtils.getKeystrokesNode());
		model.setRows(rows);
	}
	
	public void invokeRefreshTable() { //Should fix the problems associated with updating entire table at every preference change.
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            refresh();
			model.fireTableDataChanged();
			if(selected > 0){
				table.setRowSelectionInterval(selected, selected);
			}
        }
    }); 

  }

	/** This is called when the user selects a row of the table;
	 */
	public void valueChanged(ListSelectionEvent evt) {
		if (evt.getSource() == lsm && !evt.getValueIsAdjusting()) {
			int srow = table.getSelectedRow();
			if (srow >= 0) {
				String id = (String) table.getModel().getValueAt(srow, 0);
				editKeystroke(id);
			} else {
//        edit_panel.setPreferenceKey(null, null, null, null);
				edit_panel.setPreferenceKey(null, null, null);
			}
		}
	}

	private void editKeystroke(String id) {
//    edit_panel.setPreferenceKey(PreferenceUtils.getKeystrokesNode(), PreferenceUtils.getToolbarNode(), id, "");
		edit_panel.setPreferenceKey(PreferenceUtils.getKeystrokesNode(), id, "");
	}

	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getNode() != PreferenceUtils.getKeystrokesNode()) {
			return;
		}
		// Each time a keystroke preference is changed, update the
		// whole table.  Inelegant, but works. 
		invokeRefreshTable();
	}

	/*public void destroy() {
	removeAll();
	if (lsm != null) {lsm.removeListSelectionListener(this);}
	PrefenceUtils.getKeystrokesNode().removePreferenceChangeListener(this);
	}*/
	class KeyStrokeViewTable extends JTable {

		private static final long serialVersionUID = 1L;

		@Override
		public TableCellEditor getCellEditor(int row, int col) {
			DefaultCellEditor textEditor = new DefaultCellEditor(edit_panel.key_field);
			if (col == KeySrokeColumn) {
				selected = row;
				return textEditor;
			}
			return super.getCellEditor(row, col);
		}
	}
}
