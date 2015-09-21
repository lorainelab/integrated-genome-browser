package com.lorainelab.protannot;

import com.lorainelab.protannot.model.ProtannotParser;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * Displays Properties (name, value pairs) associated with whatever Glyph objects the user has selected.
 */
public class ModPropertySheet extends JPanel {

    private final JLabel title;
    private final JScrollPane scroll_pane;
    private final JViewport jvp;
    private static final String DEFAULT_TITLE = " ";
    private Properties[] props;
    private final JTable table;
    private final PropertySheetHelper helper;

    /**
     * Create a new PropertySheet containing no data.
     */
    public ModPropertySheet() {
        super();
        title = new JLabel(DEFAULT_TITLE);
        table = new JTable();
        helper = new PropertySheetHelper(table);
        jvp = new JViewport();
        scroll_pane = new JScrollPane(table);

        setUpPanel();
    }

    private void setUpPanel() {
        jvp.setView(title);
        scroll_pane.setColumnHeaderView(jvp);

        setLayout(new BorderLayout());
        add(title, BorderLayout.NORTH);
        add(scroll_pane, BorderLayout.CENTER);

        table.addMouseListener(helper);
        table.addMouseMotionListener(helper);
        table.setRowSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setAutoCreateRowSorter(true);
        table.setEnabled(true);
    }

    /**
     * Set the title, a JLabel attached to a JViewPort.
     *
     * @param ttl Name of the title
     */
    void setTitle(String ttl) {
        this.title.setText(ttl);
        jvp.setView(title);
    }

    /**
     * Gets column heading from properties.
     *
     * @param props Properties from which header names are to be retrieved.
     * @return Returns array of string containing header names.
     */
    private static String[] getColumnHeadings(
            Properties[] props) {
        // will contain number of Properties + 1
        String[] col_headings = null;
        // the number of items being described
        int num_items = props.length;

        col_headings = new String[num_items + 1];
        col_headings[0] = "";
        for (int i = 1; i < col_headings.length; i++) {
            Properties properties = props[i - 1];
            Object value = properties.getProperty("Match id");
            if (value == null) {
                value = properties.getProperty("mRNA accession");
                if (value == null) {
                    value = "";
                }
            }
            // now we just number the columns - TODO: use
            // a label that lets the user connect the heading
            // with what they see on the display
            col_headings[i] = (String) value;
        }
        return col_headings;
    }

    /**
     * Build and return rows for the table to be shown in this PropertySheet. If there are no Properties to be shown,
     * then returns default rows.
     *
     * @param name_values - a List containing name-values for a one or more Properties
     * @param props - the list of Properties
     * @return String[]
     */
    private static String[][] buildRows(List<String[]> name_values, Properties[] props) {
        int num_props = props.length;
        List<String[]> nv = new ArrayList<>();
        for (String[] vals : name_values) {
            String content = vals[0];
            if (!ProtannotParser.IDSTR.equals(content) && !ProtannotParser.NAMESTR.equals(content)) {
                nv.add(vals);
            }
        }
        String[][] rows = null;
        rows = new String[nv.size()][num_props + 1];
        for (int i = 0; i < nv.size(); i++) {
            String[] vals = nv.get(i);
            rows[i][0] = vals[0];
            System.arraycopy(vals, 1, rows[i], 1, vals.length - 1);
        }

        String[][] temp = new String[6][num_props + 1];
        List<String[]> sortedImportant = new ArrayList<>();
        List<String[]> sortedUnimportant = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            switch (rows[i][0]) {
                case "InterPro name":
                    insertInList(temp, 0, rows[i]);
                    break;
                case "application":
                    insertInList(temp, 1, rows[i]);
                    break;
                case "library":
                    insertInList(temp, 2, rows[i]);
                    break;
                case "URL":
                    insertInList(temp, 3, rows[i]);
                    break;
                case "InterPro accession":
                    insertInList(temp, 4, rows[i]);
                    break;
                case "InterPro description":
                    insertInList(temp, 5, rows[i]);
                    break;
                default:
                    sortedUnimportant.add(rows[i]);
                    break;
            }

        }

        for (int i = 0; i < temp.length; i++) {
            if (temp[i][0] != null) {
                sortedImportant.add(temp[i]);
            }
        }
        List<String[]> all = new ArrayList<>(sortedImportant);
        all.addAll(sortedUnimportant);
        if (all.size() == rows.length) {
            for (int i = 0; i < rows.length; i++) {
                rows[i] = all.get(i);
            }
        }
        return rows;
    }

    private static void insertInList(String[][] list, int position, String[] data) {
        list[position] = data;
    }

    /**
     * Show data associated with the given properties. Uses buildRows() to retrieve ordered name-value pairs.
     *
     * @param props - the given Properties
     * @see java.util.Properties
     * @see #buildRows(List, Properties[])
     */
    void showProperties(Properties[] props) {
        this.props = props;
        List<String[]> name_values = ModPropertyKeys.getNameValues(props);
        String[][] rows = buildRows(name_values, props);
        String[] col_headings = getColumnHeadings(props);

        TableModel model = new DefaultTableModel(rows, col_headings) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
        table.setDefaultRenderer(Object.class, helper);

        setTableSize(rows, table);
        validate();
    }

    // measure column headings so we can make size decisions
    private static void setTableSize(String[][] rows, JTable table) {
        int extra = 50;
        int champion = 0;
        int candidate = 0;
        FontMetrics metrix = table.getFontMetrics(table.getFont());
        for (String[] row : rows) {
            candidate = metrix.stringWidth(row[0]);
            champion = (candidate > champion ? candidate : champion);
        }

        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i == 0) {
                table.getColumnModel().getColumn(0).setPreferredWidth(champion + extra);
                table.getColumnModel().getColumn(0).setMaxWidth(champion + extra);
            }
        }

        Dimension size = new Dimension(1000, 1000);
        size.height = table.getSize().height;
        table.setSize(size);

    }

    /**
     * Returns properties of selected glyph.
     *
     * @return Return properties of selected glyph.
     */
    Properties[] getProperties() {
        return this.props;
    }

    @Override
    public Dimension getSize() {
        return table.getSize();
    }

}
