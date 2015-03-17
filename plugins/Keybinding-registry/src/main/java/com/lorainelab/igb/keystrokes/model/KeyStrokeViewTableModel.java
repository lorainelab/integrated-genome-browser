/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 *
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.lorainelab.igb.keystrokes.model;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import static com.affymetrix.common.CommonUtils.IS_MAC;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.ExistentialTriad;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.lorainelab.igb.keystrokes.KeyStrokesView;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.ActionColumn;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.IconColumn;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.IdColumn;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.KeyStrokeColumn;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.ToolbarColumn;
import com.lorainelab.igb.services.IgbService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = KeyStrokeViewTableModel.COMPONENT_NAME, immediate = true, provide = KeyStrokeViewTableModel.class)
public class KeyStrokeViewTableModel extends AbstractTableModel {

    public static final String COMPONENT_NAME = "KeyStrokeViewTableModel";
    private static final String DEFAULT_PREFS_API_RESOURCE = "igb_default_APIprefs.xml";
    private static final String COMMAND_KEY = "meta";
    private static final String CONTROL_KEY = "ctrl";
    private static final long serialVersionUID = 1L;
    private final static String[] columnNames = new String[KeyStrokesView.ColumnCount];

    private IgbService igbService;
    private static Set<String> actionKeys;
    private static final Logger logger = LoggerFactory.getLogger(KeyStrokeViewTableModel.class);

    public KeyStrokeViewTableModel() {
        actionKeys = Sets.newCopyOnWriteArraySet();
    }

    @Activate
    public void activator() {
        loadDefaultToolbarActionsAndKeystrokeBindings();
        addShortcuts();
        actionKeys.addAll(GenericActionHolder.getInstance().getGenericActionIds());
        refresh();
    }

    private void loadDefaultToolbarActionsAndKeystrokeBindings() {
        try (InputStream default_prefs_stream = KeyStrokeViewTableModel.class.getClassLoader().getResourceAsStream(DEFAULT_PREFS_API_RESOURCE);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
            //Save current preferences
            PreferenceUtils.getTopNode().exportSubtree(outputStream);
            if (default_prefs_stream != null) {
                logger.debug("loading default User preferences from: " + DEFAULT_PREFS_API_RESOURCE);
                Preferences.importPreferences(default_prefs_stream);

                /**
                 * Use 'command' instead of 'control' in keystrokes for Mac OS.
                 */
                if (IS_MAC) {
                    String[] keys = PreferenceUtils.getKeystrokesNode().keys();
                    for (int i = 0; i < keys.length; i++) {
                        String action = PreferenceUtils.getKeystrokesNode().keys()[i];
                        String keyStroke = PreferenceUtils.getKeystrokesNode().get(action, "");
                        if (keyStroke.contains(CONTROL_KEY)) {
                            keyStroke = keyStroke.replace(CONTROL_KEY, COMMAND_KEY);
                            PreferenceUtils.getKeystrokesNode().put(action, keyStroke);
                        }
                    }
                }
                //Load back saved preferences
                try (ByteArrayInputStream outputInputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                    Preferences.importPreferences(outputInputStream);
                }
            }
        } catch (Exception ex) {
            logger.debug("Problem parsing prefs from: {}", DEFAULT_PREFS_API_RESOURCE, ex);
        }
    }

    private void addShortcuts() {
        JFrame frm = igbService.getFrame();
        JPanel panel = (JPanel) frm.getContentPane();
        Preferences p = PreferenceUtils.getKeystrokesNode();
        try {
            for (String k : p.keys()) {
                String preferredKeyStroke = p.get(k, "");
                if (preferredKeyStroke.length() == 0) { // then this ain't our concern.
                    continue;
                }
                GenericActionHolder h = GenericActionHolder.getInstance();
                GenericAction a = h.getGenericAction(k);
                if (null == a) { // A keystroke in the preferences has no known action.
                    String message = "key stroke \"" + k
                            + "\" is not among our generic actionKeys.";
                    logger.trace(message);
                    try { // to load the missing class.
                        ClassLoader l = this.getClass().getClassLoader();
                        Class<?> type = l.loadClass(k);
                        if (type.isAssignableFrom(GenericAction.class)) {
                            Class<? extends GenericAction> c = type.asSubclass(GenericAction.class);
                            // Now what?
                        }
                        continue;
                    } catch (ClassNotFoundException cnfe) {
                        message = "Class " + cnfe.getMessage() + " not found.";
                        logger.trace(message);
                        continue; // Skip this one.
                    } finally {
                        message = "Keyboard shortcut " + preferredKeyStroke + " not set.";
                        logger.trace(message);
                    }
                }
                InputMap im = panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
                ActionMap am = panel.getActionMap();
                String actionIdentifier = a.getId();
                KeyStroke ks = KeyStroke.getKeyStroke(preferredKeyStroke);
                if (null == ks) { // nothing we can do.
                    String message = "Could not find preferred key stroke: "
                            + preferredKeyStroke;
                    logger.info(message);
                    continue; // Skip this one.
                }
                im.put(ks, actionIdentifier);
                am.put(actionIdentifier, a);
            }
        } catch (BackingStoreException bse) {
            logger.trace(bse.getMessage());
            logger.trace("Some keyboard shortcuts may not be set.");
        }
    }

    @Reference(multiple = true, dynamic = true)
    public void addAction(GenericAction action) {
        logger.debug("Action received");
        actionKeys.add(action.getId());
        logger.debug(action.getId());
        fireTableDataChanged();
    }

    public void removeAction(GenericAction action) {

    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    static {
        columnNames[KeyStrokesView.IconColumn] = "";
        columnNames[KeyStrokesView.ToolbarColumn] = "Toolbar ?";
        columnNames[KeyStrokesView.ActionColumn] = "Action";
        columnNames[KeyStrokesView.KeyStrokeColumn] = "Key Stroke";
    }
    private Object[][] rows;

    @Override
    public int getRowCount() {
        return (rows == null) ? 0 : rows.length;
    }

    @Override
    public int getColumnCount() {
        return KeyStrokesView.ColumnCount;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (col == KeyStrokesView.ToolbarColumn) {
            return (rows == null) ? Boolean.FALSE : rows[row][col];
        }
        return (rows == null) ? "" : rows[row][col];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column == KeyStrokesView.KeyStrokeColumn) {
            return true;
        }
        if (column == KeyStrokesView.ToolbarColumn) {
            if (rows[row][column] == ExistentialTriad.IS
                    || rows[row][column] == ExistentialTriad.ISNOT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (column == KeyStrokesView.IconColumn) {
            return ImageIcon.class;
        }
        if (column == KeyStrokesView.ToolbarColumn) {
            return ExistentialTriad.class;
        }
        return String.class;
    }

    public void setRows(Object[][] rowData) {
        rows = rowData;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == KeyStrokesView.ToolbarColumn && rows != null) {
            rows[rowIndex][columnIndex] = aValue;
            String pref_name = (String) rows[rowIndex][KeyStrokesView.IdColumn];
            boolean bValue;
            if (aValue instanceof ExistentialTriad) {
                ExistentialTriad t = (ExistentialTriad) aValue;
                bValue = t.booleanValue();
            } else { // Vestigial; This used to be a boolean.
                bValue = (Boolean) aValue;
            }
            PreferenceUtils.getToolbarNode().putBoolean(pref_name, bValue);
            GenericAction genericAction = GenericActionHolder.getInstance()
                    .getGenericAction(pref_name);
            if (genericAction == null) {
                System.err.println(this.getClass().getName() + ".setValueAt: "
                        + pref_name + " action not found.");
            } else {
                if (bValue) {
                    int index = igbService.addToolbarAction(genericAction);
                    PreferenceUtils.getToolbarNode().putInt(pref_name + ".index", index);
                } else {
                    igbService.removeToolbarAction(genericAction);
                    PreferenceUtils.getToolbarNode().remove(pref_name + ".index");
                }
            }
        }
    }

    public void refresh() {
        buildRows(PreferenceUtils.getKeystrokesNode(), PreferenceUtils.getToolbarNode());
        fireTableDataChanged();
    }

    /**
     * Build the underlying data array. There is a fourth column, not shown in the table, but needed by the setValue()
     * method.
     *
     * @param keystroke_node
     * @param toolbar_node
     */
    private void buildRows(Preferences keystroke_node, Preferences toolbar_node) {
        sortActionKeys();
        Object[][] rows = new Object[actionKeys.size()][5];
        int i = 0;
        for (String key : actionKeys) {
            GenericAction genericAction = GenericActionHolder.getInstance().getGenericAction(key);
            if (genericAction == null) {
                String logMsg = "!!! no GenericAction for key = " + key;
                logger.warn(logMsg);
                return;
            }
            rows[i][ActionColumn] = genericAction.getDisplay();
            rows[i][KeyStrokeColumn] = keystroke_node.get(key, "").toUpperCase();
            rows[i][ToolbarColumn] = ExistentialTriad.valueOf(toolbar_node.getBoolean(key, false));
            if (null == genericAction.getValue(Action.LARGE_ICON_KEY)) {
                rows[i][ToolbarColumn] = ExistentialTriad.CANNOTBE;
            }
            if (!genericAction.isToolbarAction()) {
                rows[i][ToolbarColumn] = ExistentialTriad.CANNOTBE;
            }
            rows[i][IconColumn] = rows[i][ToolbarColumn] == ExistentialTriad.CANNOTBE ? null : genericAction.getValue(Action.SMALL_ICON);
            rows[i][IdColumn] = genericAction.getId(); // not displayed
            i++;
        }
        setRows(rows);
    }

    private List<String> sortActionKeys() {
        return Ordering.natural().immutableSortedCopy(actionKeys);
    }

}
