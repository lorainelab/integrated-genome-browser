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
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.ExistentialTriad;
import com.lorainelab.igb.keystrokes.KeyStrokesView;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.ActionColumn;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.IconColumn;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.IdColumn;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.KeyStrokeColumn;
import static com.lorainelab.igb.keystrokes.KeyStrokesView.ToolbarColumn;
import com.lorainelab.igb.services.IgbService;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.ImageIcon;
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
    private static final long serialVersionUID = 1L;
    private final static String[] columnNames = new String[KeyStrokesView.ColumnCount];

    private IgbService igbService;
    private static TreeSet<String> actions;
    private static final Logger logger = LoggerFactory.getLogger(KeyStrokeViewTableModel.class);
    
    public KeyStrokeViewTableModel() {
        actions = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                GenericAction ga1 = GenericActionHolder.getInstance().getGenericAction(o1);
                if (ga1 == null) {
                    String errMsg = "No GenericAction found for \"" + o1 + "\".";
                    logger.error(errMsg);
                }
                GenericAction ga2 = GenericActionHolder.getInstance().getGenericAction(o2);
                if (ga2 == null) {
                    String errMsg = "No GenericAction found for \"" + o2 + "\".";
                    logger.error(errMsg);
                }
                return getSortField(ga1).compareTo(getSortField(ga2));
            }
        });
    }

    @Activate
    public void activator() {
        filterActions();
        refresh();
    }

    private static String getSortField(GenericAction genericAction) {
        if (genericAction == null) {
            return "";
        }
        return genericAction.getDisplay() + (genericAction.isToggle() ? "1" : "2");
    }

    @Reference(multiple = true, dynamic = true)
    public void addAction(GenericAction action) {
        logger.debug("Action received");

        if (action != null) {
            actions.add(action.getId());
            logger.debug(action.getId());
            fireTableDataChanged();
        }
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
        //TreeSet<String> keys = filterActions();//PreferenceUtils.getKeystrokesNodeNames();
        Object[][] rows;

        synchronized (actions) {
            int num_rows = actions.size();
            int num_cols = 5;
            rows = new Object[num_rows][num_cols];
            Iterator<String> iter = actions.iterator();
            for (int i = 0; iter.hasNext(); i++) {
                String key = iter.next();
                GenericAction genericAction = GenericActionHolder.getInstance().getGenericAction(key);
                if (genericAction == null) {
                    String logMsg = "!!! no GenericAction for key = " + key;
                    logger.warn(logMsg);
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
        setRows(rows);
    }

    //remove this method when all actions are moved to registry.
    private void filterActions() {
        Set<String> keys = GenericActionHolder.getInstance().getGenericActionIds();

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
    }
}
