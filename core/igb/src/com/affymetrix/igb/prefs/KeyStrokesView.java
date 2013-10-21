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

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.ExistentialTriad;
import com.affymetrix.genoviz.swing.SuperBooleanCellEditor;
import com.affymetrix.igb.action.*;
import com.affymetrix.igb.shared.JRPStyledTable;
import com.affymetrix.igb.shared.LockTierHeightAction;
import com.affymetrix.igb.shared.UnlockTierHeightAction;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * A panel that shows the preferences mapping between KeyStroke's and Actions.
 */
public final class KeyStrokesView {

	public final JRPStyledTable table = new KeyStrokeViewTable("KeyStrokesView");
	public static final KeyStrokeViewTableModel model = new KeyStrokeViewTableModel();
	public static final int IconColumn = 0;
	public static final int ToolbarColumn = 1;
	public static final int ActionColumn = 2;
	public static final int KeyStrokeColumn = 3;
	public static final int IdColumn = 4; // not displayed in table
	public static final int ColumnCount = 4;
	private final ListSelectionModel lsm;
	// private final TableRowSorter<DefaultTableModel> sorter;
	public KeyStrokeEditPanel edit_panel = null;
	private int selected = -1;

	private ListSelectionListener listSelectionListener = new ListSelectionListener() {
		/**
		 * This is called when the user selects a row of the table.
		 */
		@Override
		public void valueChanged(ListSelectionEvent evt) {
			if (evt.getSource() == lsm && !evt.getValueIsAdjusting()) {
				int srow = table.getSelectedRow();
				if (srow >= 0) {
					String id = (String) table.getModel().getValueAt(srow, IdColumn);
					editKeystroke(id);
				} else {
					edit_panel.setPreferenceKey(null, null, null, null);
				}
			}
		}

		private void editKeystroke(String id) {
			edit_panel.setPreferenceKey(PreferenceUtils.getKeystrokesNode(), PreferenceUtils.getToolbarNode(), id, "");
		}
	};
	
	private PreferenceChangeListener pcl = new PreferenceChangeListener() {
		@Override
		public void preferenceChange(PreferenceChangeEvent evt) {
			if (evt.getNode() != PreferenceUtils.getKeystrokesNode()) {
				return;
			}
			// Each time a keystroke preference is changed, update the whole table.
			// Inelegant, but it works. 
			invokeRefreshTable();
		}
	};
	
	public KeyStrokesView() {
		super();
		lsm = table.getSelectionModel();
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.setCellSelectionEnabled(false);
		table.setModel(model);
		table.setRowHeight(20);

		table.getColumnModel().getColumn(IconColumn).setPreferredWidth(25);
		table.getColumnModel().getColumn(IconColumn).setMinWidth(25);
		table.getColumnModel().getColumn(IconColumn).setMaxWidth(25);
		
		table.getColumnModel().getColumn(ToolbarColumn).setPreferredWidth(70);
		table.getColumnModel().getColumn(ToolbarColumn).setMinWidth(70);
		table.getColumnModel().getColumn(ToolbarColumn).setMaxWidth(70);
		
		table.getColumnModel().getColumn(ActionColumn).setPreferredWidth(215);
		
		table.getColumnModel().getColumn(KeyStrokeColumn).setPreferredWidth(35);

		edit_panel = new KeyStrokeEditPanel();
		edit_panel.setEnabled(false);

		try {
			PreferenceUtils.getKeystrokesNode().flush();
		} catch (Exception e) {
		}

		refresh();
		lsm.addListSelectionListener(listSelectionListener);
		PreferenceUtils.getKeystrokesNode().addPreferenceChangeListener(pcl);
	}

	private static String getSortField(GenericAction genericAction) {
		if (genericAction == null) {
			return "";
		}
		return genericAction.getDisplay() + (genericAction.isToggle() ? "1" : "2");
	}

	private static TreeSet<String> filterActions() {
		// this still throws ConcurrentModificationException
		Set<String> keys = GenericActionHolder.getInstance().getGenericActionIds();
		TreeSet<String> actions = new TreeSet<String>(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				GenericAction ga1 = GenericActionHolder.getInstance().getGenericAction(o1);
				if (ga1 == null) {
					String errMsg =	"No GenericAction found for \"" + o1 + "\".";
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
							errMsg);
				}
				GenericAction ga2 = GenericActionHolder.getInstance().getGenericAction(o2);
				if (ga2 == null) {
					String errMsg =	"No GenericAction found for \"" + o2 + "\".";
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
							errMsg);
				}
//				if(ga1 != null && ga2 != null){
//					boolean ga1cnb = ga1.getLargeIconPath() == null || smallTimeActions.contains(ga1); 
//					boolean ga2cnb = ga2.getLargeIconPath() == null || smallTimeActions.contains(ga2);
//				
//					if(ga1cnb && !ga2cnb){
//						return 1;
//					}
//					
//					if(!ga1cnb && ga2cnb){
//						return -1;
//					}
//				}
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
			} catch (NoSuchMethodException x) {
				hasGetAction = false;
			} catch (NullPointerException npe) {
				System.err.println(KeyStrokesView.class.getName() + ".filterActions: " + npe.getMessage());
				System.err.println(KeyStrokesView.class.getName() + ".filterActions: Trying to get class for " + key);
				hasGetAction = false;
			}
			if (hasGetAction && genericAction.getDisplay() != null && !"".equals(genericAction.getDisplay())) {
				actions.add(key);
			}
		}
		return actions;
	}

	/**
	 * These are actions that are inappropriate for the tool bar.
	 * This is kind of a kludge until we can figure out a better way.
	 */
	static final Set<GenericAction> smallTimeActions = new HashSet<GenericAction>();
	static {
		// Prefs Panel Only
		//smallTimeActions.add(ExportPreferencesAction.getAction());
		//smallTimeActions.add(ImportPreferencesAction.getAction());
		smallTimeActions.add(ClearPreferencesAction.getAction());
		smallTimeActions.add(PreferencesHelpAction.getAction());
		smallTimeActions.add(PreferencesHelpTabAction.getAction());
		// Actions that have a toggle should not be in the tool bar.
		// Their toggles can be, but the actions have large icons (for the toggle).
		// No. Michael says leave these in. 2012-06-15
		smallTimeActions.add(ExpandAction.getAction());
		smallTimeActions.add(CollapseAction.getAction());
		smallTimeActions.add(ShowOneTierAction.getAction());
		smallTimeActions.add(ShowTwoTiersAction.getAction());
		smallTimeActions.add(FloatTiersAction.getAction());
		smallTimeActions.add(UnFloatTiersAction.getAction());
		smallTimeActions.add(LockTierHeightAction.getAction());
		smallTimeActions.add(UnlockTierHeightAction.getAction());
		smallTimeActions.add(StartAutoScrollAction.getAction());
		smallTimeActions.add(StopAutoScrollAction.getAction());
		
		//Do not show any of selectable actions
		smallTimeActions.add(ShowPlusStrandAction.getAction());
		smallTimeActions.add(ShowMinusStrandAction.getAction());
		smallTimeActions.add(ClampViewAction.getAction());
//		smallTimeActions.add(ShrinkWrapAction.getAction());
		smallTimeActions.add(ToggleHairlineAction.getAction());
		smallTimeActions.add(ToggleHairlineLabelAction.getAction());
		smallTimeActions.add(DrawCollapseControlAction.getAction());
		smallTimeActions.add(ShowIGBTrackMarkAction.getAction());
		smallTimeActions.add(ToggleEdgeMatchingAction.getAction());
		smallTimeActions.add(ShowLockedTrackIconAction.getAction());
	}
	/**
	 * Build the underlying data array.
	 * There is a fourth column, not shown in the table,
	 * but needed by the setValue() method.
	 *
	 * @param keystroke_node
	 * @param toolbar_node
	 */
	private static Object[][] buildRows(Preferences keystroke_node, Preferences toolbar_node) {
		TreeSet<String> keys = filterActions();//PreferenceUtils.getKeystrokesNodeNames();
		Object[][] rows;

		synchronized (keys) {
			int num_rows = keys.size();
			int num_cols = 5;
			rows = new Object[num_rows][num_cols];
			Iterator<String> iter = keys.iterator();
			for (int i = 0; iter.hasNext(); i++) {
				String key = iter.next();
				GenericAction genericAction = GenericActionHolder.getInstance().getGenericAction(key);
				if (genericAction == null) {
					String logMsg = "!!! no GenericAction for key = " + key;
					Logger.getLogger(KeyStrokesView.class.getName()).log(Level.WARNING, logMsg);
				}
				rows[i][ActionColumn] = (genericAction == null) ? "???" : genericAction.getDisplay();
				rows[i][KeyStrokeColumn] = keystroke_node.get(key, "").toUpperCase();
				rows[i][ToolbarColumn] = ExistentialTriad.valueOf(toolbar_node.getBoolean(key, false));
				if (null == genericAction.getValue(Action.LARGE_ICON_KEY)) {
					rows[i][ToolbarColumn] = ExistentialTriad.CANNOTBE;
				}
				if (smallTimeActions.contains(genericAction)) {
					rows[i][ToolbarColumn] = ExistentialTriad.CANNOTBE;
				}
				rows[i][IconColumn] = genericAction == null || rows[i][ToolbarColumn] == ExistentialTriad.CANNOTBE ? null : genericAction.getValue(Action.SMALL_ICON);
				rows[i][IdColumn] = (genericAction == null) ? "" : genericAction.getId(); // not displayed
			}
		}
		return rows;
	}

	/**
	 * Re-populates the table with the shortcut data.
	 */
	private void refresh() {
		Object[][] rows;
		rows = buildRows(PreferenceUtils.getKeystrokesNode(), PreferenceUtils.getToolbarNode());
		model.setRows(rows);
	}

	/**
	 * Should fix the problems associated with updating entire table
	 * at every preference change.
	 */
	public void invokeRefreshTable() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				refresh();
				model.fireTableDataChanged();
				if (selected > 0) {
					table.setRowSelectionInterval(selected, selected);
				}
			}
		});

	}

	class KeyStrokeViewTable extends JRPStyledTable {

		public KeyStrokeViewTable(String id) {
			super(id);
			//setDefaultEditor(ExistentialTriad.class, new DefaultCellEditor(new JComboBox(ExistentialTriad.values())));
			setDefaultEditor(ExistentialTriad.class, new SuperBooleanCellEditor());
			setDefaultRenderer(ExistentialTriad.class, new SuperBooleanCellEditor());
		}

		private static final long serialVersionUID = 1L;

		@Override
		public TableCellEditor getCellEditor(int row, int col) {
			final DefaultCellEditor textEditor = new KeyEditor(edit_panel.key_field);
			if (col == KeyStrokeColumn) {
				selected = row;
				textEditor.setClickCountToStart(1);
				return textEditor;
			}
			return super.getCellEditor(row, col);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int col) {
			TableCellRenderer renderer = super.getCellRenderer(row, col);
			if (col == ToolbarColumn) {
				((SuperBooleanCellEditor) renderer).setHorizontalAlignment(SwingConstants.CENTER);
			} else if (col == IconColumn) {
			} else {
				((DefaultTableCellRenderer) renderer).setHorizontalAlignment(SwingConstants.LEFT);
			}
			return renderer;
		}
	}

	private class KeyEditor extends DefaultCellEditor {

		KeyEditor(JTextField tf) {
			super(tf);
			tf.addKeyListener(listener);
		}

		@Override
		public boolean stopCellEditing() {
			editorComponent.removeKeyListener(listener);
			return super.stopCellEditing();
		}
		
		private KeyListener listener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent evt) {
				KeyStroke ks = KeyStroke.getKeyStroke(evt.getKeyCode(), evt.getModifiers());
				if (ks.getKeyCode() == KeyEvent.VK_ENTER) {
					stopCellEditing();
				}
			}
			public void keyTyped(KeyEvent e) { }
			public void keyReleased(KeyEvent e) { }
		};
	}
}