package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import java.util.*;
import java.text.NumberFormat;
import javax.swing.JPanel;
import javax.swing.table.*;
import com.affymetrix.igb.util.JTableCutPasteAdapter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;

public final class PropertyView extends JPanel implements SymSelectionListener {

	// the table showing name-value pairs
	private static final JTable table = new JTable();
	private JScrollPane scroll_pane;
	private JTableCutPasteAdapter cutPaster;
	private TableRowSorter<TableModel> sorter;
	private static final boolean by_rows = false;
	private static final boolean sortable = true;
	public static final String PROPERTY = "property";
	public static final String DEFAULT_TITLE = "Property Sheet";

	public PropertyView() {
		super();
		scroll_pane = new JScrollPane();
		JViewport jvp = new JViewport();
		scroll_pane.setColumnHeaderView(jvp);
		//table = new JTable();
		cutPaster = new JTableCutPasteAdapter(table, true);
		setPreferredSize(new java.awt.Dimension(100, 250));
		setMinimumSize(new java.awt.Dimension(100, 250));
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
	}

	public void symSelectionChanged(SymSelectionEvent evt) {
		Object src = evt.getSource();
		// if selection event originally came from here, then ignore it...
		if (src == this) {
			return;
		}
		SeqMapView mapView = null;
		if (src instanceof SeqMapView) {
			mapView = (SeqMapView) src;
		}
		showSyms(evt.getSelectedSyms(), mapView);
	}

	private void showSyms(List<SeqSymmetry> selected_syms, SeqMapView seqMap) {
		List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();
		for (SeqSymmetry sym : selected_syms) {
			Map<String, Object> props = determineProps(sym, seqMap);
			propList.add(props);
		}
		Map[] prop_array = propList.toArray(new Map[propList.size()]);

		List<String> prop_order = determineOrder();
		this.showProperties(prop_array, prop_order, "");
	}

	private static Map<String, Object> determineProps(SeqSymmetry sym, SeqMapView seqMap) {
		Map<String, Object> props = null;
		if (sym instanceof SymWithProps) {
			// using Propertied.cloneProperties() here instead of Propertied.getProperties()
			//   because adding start, end, id, and length as additional key-val pairs to props Map
			//   and don't want these to bloat up sym's properties
			props = ((SymWithProps) sym).cloneProperties();
		}
		if (props == null && sym instanceof DerivedSeqSymmetry) {
			SeqSymmetry original_sym = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
			if (original_sym instanceof SymWithProps) {
				props = ((SymWithProps) original_sym).cloneProperties();
			}
		}
		if (props == null) {
			// make an empty hashtable if sym has no properties...
			props = new Hashtable<String, Object>();
		}
		String symid = sym.getID();
		if (symid != null) {
			props.put("id", symid);
		}
		if (seqMap != null) {
			SeqSpan span = seqMap.getViewSeqSpan(sym);
			if (span != null) {
				String chromID = span.getBioSeq().getID();
				props.put("chromosome", chromID);
				props.put("start",
						NumberFormat.getIntegerInstance().format(span.getStart()));
				props.put("end",
						NumberFormat.getIntegerInstance().format(span.getEnd()));
				props.put("length",
						NumberFormat.getIntegerInstance().format(span.getLength()));
				props.remove("seq id"); // this is redundant if "chromosome" property is set
				if (props.containsKey("method") && !props.containsKey("type")) {
					props.put("type", props.get("method"));
					props.remove("method");
				}
			}
		}
		if (sym instanceof GraphSym) {
			float[] range = ((GraphSym) sym).getVisibleYRange();
			props.put("min score", range[0]);
			props.put("max score", range[1]);
		}
		return props;
	}

	// The general order these fields should show up in.
	private static List<String> determineOrder() {
		List<String> prop_order;

		prop_order = new ArrayList<String>(20);
		prop_order.add("gene name");
		prop_order.add("name");
		prop_order.add("id");
		prop_order.add("chromosome");
		prop_order.add("start");
		prop_order.add("end");
		prop_order.add("length");
		prop_order.add("min score");
		prop_order.add("max score");
		prop_order.add("type");
		prop_order.add("same orientation");
		prop_order.add("query length");
		prop_order.add("# matches");
		prop_order.add("# target inserts");
		prop_order.add("# target bases inserted");
		prop_order.add("# query bases inserted");
		prop_order.add("# query inserts");
		prop_order.add("seq id");
		prop_order.add("cds min");
		prop_order.add("cds max");

		return prop_order;
	}

	/**
	 * Return headings for columns.  If we're laying out
	 * values in a row, then column headings will be the
	 * names associated with each value.  If we're laying
	 * out values in a column, then column headings will
	 * be PROPERTY and then labels for the item whose
	 * values are being presented.
	 * @param name_values - a List containing name-values for a
	 *   one or more Properties
	 * @param props - the list of Properties
	 */
	private static String[] getColumnHeadings(List<String[]> name_values,
			Map[] props) {
		// will contain number of Properties + 1 if by_rows is false
		// will contain number of values if by_rows is true
		String[] col_headings = null;
		// the number of different name-value groups
		int num_values = name_values.size();
		// the number of items being described
		int num_items = props.length;
		if (by_rows) {  // columns represent individual property names
			col_headings = new String[num_values];
			for (int i = 0; i < num_values; i++) {
				col_headings[i] = PropertyKeys.getName(name_values, i);
			}
		} else {  // columns represent set of properties for a particular entity
			col_headings = new String[num_items + 1];
			col_headings[0] = PROPERTY;
			for (int i = 0; i < num_items; i++) {
				Object id_obj = props[i].get("id");
				String id;
				if (id_obj == null) {
					id = "no ID";
				} else {
					id = id_obj.toString();
				} // in most cases the id already is a String
				col_headings[i + 1] = id;
			}
		}
		return col_headings;
	}

	/**
	 * Build and return rows for the table to be shown in
	 * this PropertySheet.
	 * If there are no Properties to be shown, then returns
	 * default rows.
	 * @param name_values - a List containing name-values for a
	 *   one or more Properties
	 * @param props  the list of Properties
	 */
	private static String[][] buildRows(List<String[]> name_values, Map[] props) {
		int num_props = props.length;
		int num_vals = name_values.size();
		String[][] rows = null;
		if (by_rows) {
			rows = new String[num_props][num_vals];
			for (int j = 0; j < num_props; j++) {
				for (int i = 0; i < num_vals; i++) {
					String[] vals = name_values.get(i);
					try {
						rows[j][i] = vals[j + 1];
					} catch (ArrayIndexOutOfBoundsException array_ex) {
						System.out.println("error allocating rows for property sheet.");
					}
				}
			}
		} else {
			rows = new String[num_vals][num_props + 1];
			for (int i = 0; i < num_vals; i++) {
				String[] vals = name_values.get(i);
				rows[i][0] = vals[0];
				for (int j = 1; j < vals.length; j++) {
					rows[i][j] = vals[j];
				}
			}
		}
		return rows;
	}

	/**
	 * take name_values and return a new ArrayList that
	 *  starts with the names found in preferred_ordering in the specified order,
	 *  and then adds entries in name_values that were not found in preferred_ordering.
	 *
	 *  WARNING! this destroys integrity of original name_values!
	 *  also assumes that there are no null entries in name_values
	 *
	 * @param name_values   a List of String[]s
	 * @param preferred_ordering a List of Strings with the preferred order of column names
	 * @return String array of re-ordered names
	 */
	private static List<String[]> reorderNames(List<String[]> name_values, List<String> preferred_ordering) {
		List<String[]> reordered = new ArrayList<String[]>(name_values.size());
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
		for (String[] name_value : name_values) {
			if (name_value != null) {
				reordered.add(name_value);
			}
		}

		return reordered;
	}

	/**
	 * Show data associated with the given properties.
	 * Uses buildRows() to retrieve ordered
	 * name-value pairs.
	 * @param props  the given Properties
	 * @param preferred_prop_order the preferred order of columns
	 * @param noData the value to use when a property value is null
	 * @see #buildRows(List, Map[])
	 */
	private void showProperties(Map[] props, List<String> preferred_prop_order, String noData) {
		PropertyKeys propkeys = new PropertyKeys();

		List<String[]> name_values = propkeys.getNameValues(props, noData);
		if (preferred_prop_order != null) {
			name_values = reorderNames(name_values, preferred_prop_order);
		}
		String[][] rows = buildRows(name_values, props);
		String[] col_headings = getColumnHeadings(name_values, props);

		TableModel model = new DefaultTableModel(rows, col_headings) {

			public static final long serialVersionUID = 1l;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		table.setModel(model);
		if (sortable) {
			sorter = new TableRowSorter<TableModel>(model);
			table.setRowSorter(sorter);
		}

		table.setEnabled(true);  // to allow selection, etc.
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.removeAll();
		this.setLayout(new BorderLayout());
		scroll_pane = new JScrollPane(table);
		this.add(scroll_pane, BorderLayout.CENTER);
		table.setCellSelectionEnabled(true);

		validate();
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setMinWidth(100);
			table.getColumnModel().getColumn(i).setPreferredWidth(150);
		}
	}
}


