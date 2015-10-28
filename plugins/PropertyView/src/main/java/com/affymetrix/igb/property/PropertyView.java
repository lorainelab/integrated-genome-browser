package com.affymetrix.igb.property;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.*;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.PropertyViewHelper;
import static com.affymetrix.genometry.util.SelectionInfoUtils.*;
import com.affymetrix.genoviz.swing.JTextButtonCellRendererImpl;
import com.affymetrix.igb.swing.jide.JRPStyledTable;
import com.lorainelab.igb.services.IgbService;
import static com.lorainelab.igb.services.ServiceComponentNameReference.SELECTION_INFO_TAB;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel;
import com.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

@Component(name = SELECTION_INFO_TAB, provide = IgbTabPanelI.class, immediate = true)
public final class PropertyView extends IgbTabPanel implements SymSelectionListener, PropertyHandler, GroupSelectionListener {

    private static final long serialVersionUID = 1L;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("property");
    private static final int TAB_POSITION = 4;
    private static final int MAX_SYM_PROPERTIES = 20;

    // the table showing name-value pairs
    private static final JRPStyledTable table = new JRPStyledTable("PropertyView_table");
    private final JScrollPane scroll_pane = new JScrollPane();
    private TableRowSorter<TableModel> sorter;
    private static final String PROPERTY = "property";
    private Set<PropertyListener> propertyListeners = new HashSet<>();
    private IgbService igbService;

    public PropertyView() {
        super(BUNDLE.getString("propertyViewTab"), BUNDLE.getString("propertyViewTab"), BUNDLE.getString("selectionInfoTooltip"), false, TAB_POSITION);
        JViewport jvp = new JViewport();
        scroll_pane.setColumnHeaderView(jvp);
        //TODO cleanup this code... why instantiate a class here?
        new JTableCutPasteAdapter(table, true);
        //TODO cleanup this code... why instantiate a class here?
        new PropertyViewHelper(table);
        this.setPreferredSize(new java.awt.Dimension(100, 250));
        this.setMinimumSize(new java.awt.Dimension(100, 250));
        GenometryModel.getInstance().addSymSelectionListener(this);
        GenometryModel.getInstance().addGroupSelectionListener(this);
    }

    @Activate
    public void activate() {
        propertyListeners.add((PropertyListener) igbService.getSeqMapView().getMouseListener());
        igbService.getSeqMapView().setPropertyHandler(this);
    }

    @Deactivate
    public void stop() {
        igbService.getSeqMapView().setPropertyHandler(null);
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Override
    // implement SymSelectionListener
    public void symSelectionChanged(SymSelectionEvent evt) {
        Object src = evt.getSource();
        // if selection event originally came from here, then ignore it...
        if (src == this) {
            return;
        }

        List<SeqSymmetry> selected_syms = evt.getSelectedGraphSyms();
        int size = Math.min(selected_syms.size(), MAX_SYM_PROPERTIES);
        boolean skip_glyph_properties = false;
        if (selected_syms.size() > MAX_SYM_PROPERTIES) {
            skip_glyph_properties = true;
            Logger.getLogger(PropertyView.class.getName()).log(Level.INFO, "Skipping collecting properties; too many syms selected");
        }
        List<Map<String, Object>> propList = new ArrayList<>();
        if (src instanceof PropertyHolder) {
            PropertyHolder propertyHolder = (PropertyHolder) src;
            for (int i = 0; i < size; i++) {
                Map<String, Object> props = propertyHolder.determineProps(selected_syms.get(i));
                if (selected_syms.get(i) instanceof SymWithProps) {
                    props = orderProperties(props, (SymWithProps) selected_syms.get(i));
                }
                if (props != null) {
                    propList.add(props);
                }
            }

            if (!skip_glyph_properties) {
                propList.addAll(propertyHolder.getProperties());
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object>[] prop_array = propList.toArray(new Map[propList.size()]);

        this.showProperties(prop_array, null, "", false);
    }

    @Override
    // implement GroupSelectionListener
    public void groupSelectionChanged(GenomeVersionSelectionEvent evt) {
        if (evt.getSelectedGroup() == null) {
            table.setModel(new DefaultTableModel());
            propertyChanged(0);
        }
    }

    /**
     * Return headings for columns. If we're laying out values in a row, then
     * column headings will be the names associated with each value. If we're
     * laying out values in a column, then column headings will be PROPERTY and
     * then labels for the item whose values are being presented.
     *
     * @param name_values - a List containing name-values for a one or more
     * Properties
     * @param props - the list of Properties
     */
    private static String[] getColumnHeadings(Map<String, Object>[] props) {
        // will contain number of Properties + 1 if by_rows is false
        // will contain number of values if by_rows is true
        String[] col_headings = null;

        // the number of items being described
        int num_items = props.length;
        // columns represent set of properties for a particular entity
        col_headings = new String[num_items + 1];
        col_headings[0] = PROPERTY;
        for (int i = 0; i < num_items; i++) {
            Object id_obj = props[i].get("id");
            String id;
            if (id_obj == null) {
                id = BUNDLE.getString("noID");
            } else {
                id = id_obj.toString();
            } // in most cases the id already is a String
            col_headings[i + 1] = id;
        }

        return col_headings;
    }

    /**
     * Build and return rows for the table to be shown in this PropertySheet. If
     * there are no Properties to be shown, then returns default rows.
     *
     * @param name_values - a List containing name-values for a one or more
     * Properties
     * @param props the list of Properties
     */
    private static String[][] buildRows(List<String[]> name_values, Map<String, Object>[] props) {
        int num_vals = name_values.size();
        String[][] rows = new String[num_vals][props.length + 1];
        for (int i = 0; i < num_vals; i++) {
            String[] vals = name_values.get(i);
            rows[i][0] = vals[0];
            System.arraycopy(vals, 1, rows[i], 1, vals.length - 1);
        }
        return rows;
    }

    /**
     * Show data associated with the given properties. Uses buildRows() to
     * retrieve ordered name-value pairs.
     *
     * @param props the given Properties
     * @param preferred_prop_order the preferred order of columns
     * @param noData the value to use when a property value is null
     */
    private void showProperties(Map<String, Object>[] props,
            String[] preferred_prop_order, String noData, boolean limited) {

        String[][] rows = getPropertiesRow(props, preferred_prop_order, noData, limited);
        String[] col_headings = getColumnHeadings(props);

        //start
        //System.out.println("#############rows length: " + rows.length);
        //System.out.println("#############col_headings length: " + col_headings.length);
        //System.out.println("#############propertiesInColumns: " + propertiesInColumns);
        //System.out.println("#############rows length: " + rows.length);
        //System.out.println("#############col_headings length: " + col_headings.length);
        //end
        propertyChanged(col_headings.length);

        TableModel model = new PropertyTableModel(rows, col_headings);
        table.setModel(model);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setMinimumSize(new Dimension(80000, 500));	//added by Max
        //table.setPreferredScrollableViewportSize(new Dimension(80000, 500));  //added by Max
        this.removeAll();
        this.setLayout(new BorderLayout());
        scroll_pane.setViewportView(table);
        scroll_pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        //scroll_pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        this.add(scroll_pane, BorderLayout.CENTER);

        final JTextButtonCellRendererImpl ren = new JTextButtonCellRendererImpl(igbService.getApplicationFrame());
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(ren);
            table.getColumnModel().getColumn(i).setCellEditor(ren);
        }

        validate();
        table.getColumnModel().getColumn(0).setMinWidth(100);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(0).setMaxWidth(200);
    }

    @SuppressWarnings("unchecked")
    @Override
    // implement PropertyHandler
    public Map<String, Object> getPropertiesRow(SeqSymmetry sym, PropertyHolder propertyHolder) {
        List<Map<String, Object>> propList = new ArrayList<>();
        Map<String, Object> props = propertyHolder.determineProps(sym);
        propList.add(props);

        //return getPropertiesRow(propList.toArray(new Map[propList.size()]), prop_order, "", false);
        return props;
    }

    @SuppressWarnings("unchecked")
    @Override
    // implement PropertyHandler
    public Map<String, Object> getGraphPropertiesRowColumn(GraphSym sym, int x, PropertyHolder propertyHolder) {
        List<Map<String, Object>> propList = new ArrayList<>();
        Map<String, Object> props = propertyHolder.determineProps(sym);
        props.putAll(sym.getLocationProperties(x, igbService.getSeqMapView().getVisibleSpan()));
        propList.add(props);

        //return getPropertiesRow(propList.toArray(new Map[propList.size()]), graph_tooltip_order, "", false);
        return props;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void showGraphProperties(GraphSym sym, int x, PropertyHolder propertyHolder) {
        List<Map<String, Object>> propList = new ArrayList<>();
        Map<String, Object> props = propertyHolder.determineProps(sym);
        props.putAll(sym.getLocationProperties(x, igbService.getSeqMapView().getVisibleSpan()));
        propList.add(props);

        Map<String, Object>[] prop_array = propList.toArray(new Map[propList.size()]);

        this.showProperties(prop_array, graph_tooltip_order, "", true);
    }

    private static String[][] getPropertiesRow(Map<String, Object>[] props, String[] preferred_prop_order, String noData, boolean limited) {
        List<String[]> name_values = getNameValues(props, noData);
        if (preferred_prop_order != null) {
            name_values = reorderNames(name_values, preferred_prop_order, limited);
        }
        return buildRows(name_values, props);
    }

    /**
     * take name_values and return a new ArrayList that starts with the names
     * found in preferred_ordering in the specified order, and then adds entries
     * in name_values that were not found in preferred_ordering.
     *
     * WARNING! this destroys integrity of original name_values! also assumes
     * that there are no null entries in name_values
     *
     * @param name_values a List of String[]s
     * @param preferred_ordering a List of Strings with the preferred order of
     * column names
     * @return String array of re-ordered names
     */
    private static List<String[]> reorderNames(List<String[]> name_values, String[] preferred_ordering, boolean limited) {
        List<String[]> reordered = new ArrayList<>(name_values.size());
        for (String request : preferred_ordering) {
            for (int k = 0; k < name_values.size(); k++) {
                String[] vals = name_values.get(k);
                if (vals != null && vals.length > 0) {
                    String name = vals[0];
                    if (name.equals(request)) {
                        reordered.add(vals);
                        name_values.set(k, null);
                        break;
                    }
                }
            }
        }

        if (!limited) {
            for (String[] name_value : name_values) {
                if (name_value != null) {
                    reordered.add(name_value);
                }
            }
        }

        return reordered;
    }

    /**
     * Fills up a Vector with arrays containing names and values for each of the
     * given Properties. e.g., {name,value0,value1,value2,...,valueN} for N
     * different Properties Objects.
     *
     * @param props the list of Properties derived from SeqFeatures.
     * @param noData the String value to use to represent cases where there is
     * no value of the property for a given key
     */
    private static List<String[]> getNameValues(Map<String, Object>[] props, String noData) {
        List<String[]> result = new ArrayList<>();
        // collect all possible names from the given Properties
        int num_props = props.length;
        Map<String, String[]> rows_thus_far = new LinkedHashMap<>();
        for (Map<String, Object> prop : props) {
            for (String name : prop.keySet()) {
                if (name != null && rows_thus_far.containsKey(name)) {
                    continue;
                }

                String name_value[] = new String[num_props + 1];
                name_value[0] = name;
                for (int j = 0; j < props.length; j++) {
                    Object val = props[j].get(name);
                    val = (val == null ? noData : val);
                    // if val is a List for multivalued property, rely on toString() to convert to [item1, item2, etc.]
                    //   string representation
                    name_value[j + 1] = val.toString();
                }
                rows_thus_far.put(name, name_value);
            }
        }

        result.addAll(rows_thus_far.values());

        return result;
    }

    private void propertyChanged(int prop_displayed) {
        for (PropertyListener pl : propertyListeners) {
            pl.propertyDisplayed(prop_displayed);
        }
    }

    @Override
    // override IGBTabPanel
    public boolean isEmbedded() {
        return true;
    }

    @Override
    // override IGBTabPanel
    public boolean isCheckMinimumWindowSize() {
        return true;
    }
}
