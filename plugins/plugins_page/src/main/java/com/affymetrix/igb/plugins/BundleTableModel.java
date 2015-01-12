package com.affymetrix.igb.plugins;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.RowSorter.SortKey;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * the TableModel for the table in the plugins view
 */
public class BundleTableModel extends DefaultTableModel implements Constants {

    private static final long serialVersionUID = 1L;
    private static final int WIDE_COLUMN_MULTIPLIER = 5;
    private static final int NARROW_COLUMN = 60;

    /**
     * this panel has the Bundle Symbolic Name along with an (optional) info
     * icon that links to the documntation web page of the bundle.
     */
    public static class NameInfoPanel extends JPanel implements Comparable<NameInfoPanel> {

        private static final long serialVersionUID = 1L;
        private static final HashMap<Bundle, NameInfoPanel> PANEL_MAP = new HashMap<>(); // kludge
        private final JLabel text;
        private final JLabel icon;
        private final String tooltip;

        /**
         * get the NameInfoPanel for the given bundle
         *
         * @param bundle
         * @return the NameInfoPanel for the specified bundle
         */
        public static NameInfoPanel getPanel(Bundle bundle) {
            return PANEL_MAP.get(bundle);
        }

        public NameInfoPanel(Bundle bundle) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            text = new JLabel(bundle.getHeaders().get(BUNDLE_NAME) + " ");
            add(text);
            if (bundle.getHeaders().get(Constants.BUNDLE_DOCURL) != null) {
                icon = new JLabel(pluginsHandler.getIcon("info.png"));
                add(icon);
            } else {
                icon = null;
            }
            tooltip = bundle.getSymbolicName();
            PANEL_MAP.put(bundle, this);
        }

        /**
         * @param x x position of cursor on window
         * @param y y position of cursor on window
         * @return if the specified position is over the info icon
         */
        public boolean isOnInfoIcon(int x, int y) {
            if (icon == null) {
                return false;
            }
            Rectangle iconBounds = icon.getBounds();
            return x >= iconBounds.getX() && x <= iconBounds.getX() + icon.getWidth()
                    && y >= iconBounds.getY() && y <= iconBounds.getY() + icon.getHeight();
        }

        @Override
        public String toString() {
            return text.getText() + " " + (icon != null);
        }

        public String getTooltip() {
            return tooltip;
        }

        @Override
        public int compareTo(NameInfoPanel o) {
            return toString().toLowerCase().compareTo(o.toString().toLowerCase());
        }
    }

    /**
     * Swing Renderer class for the NameInfoPanel
     */
    public static class NameInfoRenderer implements TableCellRenderer, UIResource {

        private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            NameInfoPanel nameInfoPanel = (NameInfoPanel) value;
            if (nameInfoPanel == null) {
                return null;
            }
            if (isSelected) {
                nameInfoPanel.setForeground(table.getSelectionForeground());
                nameInfoPanel.setBackground(table.getSelectionBackground());
            } else {
                nameInfoPanel.setForeground(table.getForeground());
                nameInfoPanel.setBackground(table.getBackground());
            }
            if (hasFocus) {
                nameInfoPanel.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else {
                nameInfoPanel.setBorder(noFocusBorder);
            }

            nameInfoPanel.setToolTipText(nameInfoPanel.getTooltip());
            return nameInfoPanel;
        }
    }

    /**
     * parent class for all the columns in the table
     */
    private static abstract class BundleColumn {

        public abstract String getTitle();

        public Class<?> getCellClass() {
            return JLabel.class;
        }

        public boolean isEditable() {
            return false;
        }

        public abstract Object getValue(Bundle bundle);

        public void setValue(Bundle bundle, Object aValue, IPluginsHandler pluginsHandler) {
        }

        public void formatColumn(JTable jTable, TableColumn tc) {
        }
    }

    /**
     * helper class for bundle versions - they need to display the latest
     * version in parenthesis if necessary.
     */
    private static class VersionInfo {

        private final Bundle bundle;
        private final IPluginsHandler pluginsHandler;

        public VersionInfo(Bundle bundle, IPluginsHandler pluginsHandler) {
            super();
            this.bundle = bundle;
            this.pluginsHandler = pluginsHandler;
        }

        public Version getVersion() {
            return bundle.getVersion();
        }

        public Version getLatestVersion() {
            return pluginsHandler.getLatestVersion(bundle);
        }

        public String toString() {
            return pluginsHandler.isUpdatable(bundle)
                    ? "<html>" + getVersion() + " (<b>" + getLatestVersion() + "</b>)</html>)"
                    : "" + getVersion();
        }
    }
    private static final ArrayList<BundleColumn> columns = new ArrayList<>();

    static {
        columns.add(new BundleColumn() { // install

            @Override
            public String getTitle() {
                return PluginsView.BUNDLE.getString("installColumn");
            }

            @Override
            public boolean isEditable() {
                return true;
            }

            @Override
            public Object getValue(Bundle bundle) {
                return bundle.getState() != Bundle.UNINSTALLED;
            }

            @Override
            public void setValue(Bundle bundle, Object aValue, IPluginsHandler pluginsHandler) {
                if (bundle.getState() == Bundle.UNINSTALLED) {
                    pluginsHandler.installBundle(bundle);
                } else {
                    pluginsHandler.uninstallBundle(bundle);
                }
            }

            @Override
            public void formatColumn(JTable jTable, TableColumn tc) {
                tc.setCellEditor(jTable.getDefaultEditor(Boolean.class));
                tc.setCellRenderer(jTable.getDefaultRenderer(Boolean.class));
                tc.setMinWidth(NARROW_COLUMN);
                tc.setMaxWidth(NARROW_COLUMN);
                tc.setPreferredWidth(NARROW_COLUMN);
            }
        });
        columns.add(new BundleColumn() { // name

            @Override
            public String getTitle() {
                return PluginsView.BUNDLE.getString(BUNDLE_NAME);
            }

            @Override
            public Class<?> getCellClass() {
                return NameInfoPanel.class;
            }

            @Override
            public Object getValue(Bundle bundle) {
                return new NameInfoPanel(bundle);
            }

            @Override
            public void formatColumn(JTable jTable, TableColumn tc) {
                tc.setCellRenderer(new NameInfoRenderer());
            }
        });
        columns.add(new BundleColumn() { // description

            @Override
            public String getTitle() {
                return PluginsView.BUNDLE.getString(BUNDLE_DESCRIPTION);
            }

            @Override
            public Object getValue(Bundle bundle) {
                if (bundle.getHeaders() == null || bundle.getHeaders().get(BUNDLE_DESCRIPTION) == null || bundle.getHeaders().get(BUNDLE_DESCRIPTION).isEmpty()) {
                    return "no description";
                }
                Object descriptionObj = bundle.getHeaders().get(BUNDLE_DESCRIPTION);
                String description = (descriptionObj == null) ? "" : descriptionObj.toString();
                description = description.replaceAll("\n", " ");
                description = description.replaceAll(" +", " ");
                return description;
            }

            @Override
            public void formatColumn(JTable jTable, TableColumn tc) {
                tc.setPreferredWidth(tc.getPreferredWidth() * WIDE_COLUMN_MULTIPLIER);
                tc.setCellRenderer(
                        new TableCellRenderer() {

                            @Override
                            public Component getTableCellRendererComponent(JTable table, Object value,
                                    boolean isSelected, boolean hasFocus, int row, int column) {
                                Component component = new DefaultTableCellRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                                ((JLabel) component).setToolTipText((String) value);
                                return component;
                            }
                        });
            }
        });
        columns.add(new BundleColumn() { // version

            @Override
            public String getTitle() {
                return PluginsView.BUNDLE.getString(BUNDLE_VERSION);
            }

            @Override
            public Object getValue(Bundle bundle) {
                return new VersionInfo(bundle, pluginsHandler);
            }
        });
        columns.add(new BundleColumn() { // repository

            @Override
            public String getTitle() {
                return PluginsView.BUNDLE.getString("repository");
            }

            @Override
            public Object getValue(Bundle bundle) {
                return pluginsHandler.getRepository(bundle);
            }
        });
    }
    private static IPluginsHandler pluginsHandler;

    public static void setPluginsHandler(IPluginsHandler _pluginsHandler) {
        pluginsHandler = _pluginsHandler;
    }
    public static final List<SortKey> SORT_KEYS;

    static {
        List<SortKey> sortKeys = new ArrayList<>(2);
        sortKeys.add(new SortKey(0, SortOrder.ASCENDING));
        sortKeys.add(new SortKey(1, SortOrder.ASCENDING));

        SORT_KEYS = Collections.<SortKey>unmodifiableList(sortKeys);
    }

    public BundleTableModel() {
        super();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns.get(columnIndex).getCellClass();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns.get(columnIndex).getTitle();
    }

    @Override
    public int getRowCount() {
        return pluginsHandler.getFilteredBundleCount();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).getValue(pluginsHandler.getFilteredBundle(rowIndex));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).isEditable();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        columns.get(columnIndex).setValue(pluginsHandler.getFilteredBundle(rowIndex), aValue, pluginsHandler);
    }

    /**
     * get the index of the column in the table, given the column name
     *
     * @param key the name of the column
     * @return the index of the column in the table
     */
    public int getColumnIndex(String key) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getTitle().equals(PluginsView.BUNDLE.getString(key))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * set the parent table
     *
     * @param jTable the parent table that uses this TableModel
     */
    public void setJTable(JTable jTable) {
        for (int i = 0; i < columns.size(); i++) {
            BundleColumn bundleColumn = columns.get(i);
            TableColumn checkboxColumn = jTable.getColumnModel().getColumn(i);
            bundleColumn.formatColumn(jTable, checkboxColumn);
        }
    }
}
