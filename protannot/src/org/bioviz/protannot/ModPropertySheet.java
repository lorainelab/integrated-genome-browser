package org.bioviz.protannot;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Hold and show property of the selected Glyph
 */
final class ModPropertySheet extends JPanel {

    // the table showing name-value pairs
    private JTable table;
    private JLabel title;
    private JScrollPane scroll_pane;
    private JViewport jvp;
    private Dimension size = new Dimension(1000, 1000);
    private static final String DEFAULT_TITLE = " ";
    private Properties[] props;

    /**
     * Create a new PropertySheet containing no data.
     */
    ModPropertySheet() {
        super();
        scroll_pane = new JScrollPane();
        title = new JLabel(DEFAULT_TITLE);
        jvp = new JViewport();
        jvp.setView(title);
        scroll_pane.setColumnHeaderView(jvp);
    }

    /**
     * Set the title, a Jlabel attached to a JViewPort.
     * @param   ttl     Name of the title
     */
    void setTitle(String ttl) {
        this.title = new JLabel(ttl);
        jvp.setView(title);
    }

    /**
     * Gets column heading from properties.
     * @param   props   Properties from which header names are to be retrived.
     * @return          Returns array of string containing header names.
     */
    private String[] getColumnHeadings(
            Properties[] props) {
        // will contain number of Properties + 1
        String[] col_headings = null;
        // the number of items being described
        int num_items = props.length;

        col_headings = new String[num_items + 1];
        col_headings[0] = "";
        for (int i = 1; i < col_headings.length; i++) {
            Properties properties = props[i - 1];
            Object value = properties.getProperty("name");
            if (value == null) {
                value = properties.getProperty("id");
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
     * Build and return rows for the table to be shown in
     * this PropertySheet.
     * If there are no Properties to be shown, then returns
     * default rows.
     * @param   name_values - a Vector containing name-values for a
     * one or more Properties
     * @param   props       - the list of Properties
     * @return  String[]
     */
    private String[][] buildRows(Vector<String[]> name_values, Properties[] props) {
        int num_props = props.length;
        Vector<String[]> nv = new Vector<String[]>();
        for (String[] vals : name_values) {
            String content = vals[0];
            if (!content.equals("id") && !content.equals("name")) {
                nv.add(vals);
            }
        }
        String[][] rows = null;
        rows = new String[nv.size()][num_props + 1];
        for (int i = 0; i < nv.size(); i++) {
            String[] vals = nv.elementAt(i);
            rows[i][0] = vals[0];
            for (int j = 1; j < vals.length; j++) {
                rows[i][j] = vals[j];
            }
        }
        return rows;
    }

    /**
     * Show data associated with the given properties.
     * Uses buildRows() to retrieve ordered name-value pairs.
     * @param   props - the given Properties
     * @see     java.util.Properties
     * @see     #buildRows(Vector, Properties[])
     */
    void showProperties(Properties[] props) {
        this.props = props;
        ModPropertyKeys propkeys = new ModPropertyKeys();
        Vector<String[]> name_values = propkeys.getNameValues(props);
        String[][] rows = buildRows(name_values, props);
        String[] col_headings = getColumnHeadings(props);
        this.table = new JTable();
        TableModel model = new DefaultTableModel(rows, col_headings) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
        table.setRowSelectionAllowed(false);
        // measure column headings so we can make size decisions
        int champion = 0;
        int candidate = 0;
        int num_cols = col_headings.length;
        FontMetrics metrix = table.getFontMetrics(table.getFont());
        for (int i = 0; i < num_cols; i++) {
            candidate = metrix.stringWidth(col_headings[i]);
            champion = (candidate > champion ? candidate : champion);
        }
        table.setEnabled(false);
        this.size.height = table.getSize().height;
        //    updateColumnSize();
        table.setSize(this.size);
        this.removeAll();
        this.setLayout(new BorderLayout());
        scroll_pane = new JScrollPane(table);
        this.add(title, BorderLayout.NORTH);
        this.add(scroll_pane, BorderLayout.CENTER);
        validate();
    }

    /**
     * Returns properties of selected glyph.
     * @return  Return properties of selected glyph.
     */
    Properties[] getProperties() {
        return this.props;
    }
}
