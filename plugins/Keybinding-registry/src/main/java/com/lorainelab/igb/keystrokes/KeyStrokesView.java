/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.lorainelab.igb.keystrokes;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.ExistentialTriad;
import com.affymetrix.genoviz.swing.SuperBooleanCellEditor;
import com.affymetrix.igb.swing.jide.JRPStyledTable;
import com.lorainelab.igb.keystrokes.model.KeyStrokeViewTableModel;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import net.miginfocom.swing.MigLayout;
import org.slf4j.LoggerFactory;

/**
 * A panel that shows the preferences mapping between KeyStroke's and Actions.
 */
@Component(name = KeyStrokesView.COMPONENT_NAME, immediate = true, provide = {KeyStrokesView.class, PreferencesPanelProvider.class})
public final class KeyStrokesView implements PreferencesPanelProvider {

    public static final String COMPONENT_NAME = "KeyStrokesView";
    private JRPStyledTable table = new KeyStrokeViewTable("KeyStrokesView");
    public KeyStrokeViewTableModel model;
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
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(KeyStrokesView.class);
    private final JPanel keyStrokePanel;

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
                    edit_panel.setPreferenceKey(null, null);
                }
            }
        }

        private void editKeystroke(String id) {
            edit_panel.setPreferenceKey(id, "");
        }
    };

    private PreferenceChangeListener pcl = evt -> {
        if (evt.getNode() != PreferenceUtils.getKeystrokesNode()) {
            return;
        }
        // Each time a keystroke preference is changed, update the whole table.
        // Inelegant, but it works.
        refresh();
    };

    public KeyStrokesView() {
        table = new KeyStrokeViewTable("KeyStrokesView");
        keyStrokePanel = new JPanel(new MigLayout("fill"));
        JScrollPane scrollPane = new javax.swing.JScrollPane();
        scrollPane.setViewportView(table);
        keyStrokePanel.setName("Toolbar");
        keyStrokePanel.setToolTipText("Edit Locations");
        keyStrokePanel.add(scrollPane, "grow");
        lsm = table.getSelectionModel();
    }

    @Activate
    public void activate() {
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
        TreeSet<String> actions = new TreeSet<>(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                GenericAction ga1 = GenericActionHolder.getInstance().getGenericAction(o1);
                if (ga1 == null) {
                    String errMsg = "No GenericAction found for \"" + o1 + "\".";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                            errMsg);
                }
                GenericAction ga2 = GenericActionHolder.getInstance().getGenericAction(o2);
                if (ga2 == null) {
                    String errMsg = "No GenericAction found for \"" + o2 + "\".";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                            errMsg);
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
     * Build the underlying data array. There is a fourth column, not shown in
     * the table, but needed by the setValue() method.
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
                if (!genericAction.isToolbarAction()) {
                    rows[i][ToolbarColumn] = ExistentialTriad.CANNOTBE;
                }
                rows[i][IconColumn] = genericAction == null || rows[i][ToolbarColumn] == ExistentialTriad.CANNOTBE ? null : genericAction.getValue(Action.SMALL_ICON);
                rows[i][IdColumn] = (genericAction == null) ? "" : genericAction.getId(); // not displayed
            }
        }
        return rows;
    }

    @Reference(optional = false)
    public void setModel(KeyStrokeViewTableModel model) {
        this.model = model;
    }

    /**
     * Should fix the problems associated with updating entire table at every
     * preference change.
     */
    @Override
    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            Object[][] rows;
            rows = buildRows(PreferenceUtils.getKeystrokesNode(), PreferenceUtils.getToolbarNode());
            model.setRows(rows);
            model.fireTableDataChanged();
            if (selected > 0) {
                table.setRowSelectionInterval(selected, selected);
            }
        });

    }

    @Override
    public String getName() {
        return COMPONENT_NAME;
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

    @Override
    public int getTabWeight() {
        return 1;
    }

    @Override
    public JPanel getPanel() {
        return keyStrokePanel;
    }

    private class KeyEditor extends DefaultCellEditor {

        private static final long serialVersionUID = 1L;

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
                    refresh();
                }
            }

            public void keyTyped(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
            }
        };
    }
}
