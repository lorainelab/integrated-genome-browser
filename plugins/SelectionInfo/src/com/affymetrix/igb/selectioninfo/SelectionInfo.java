package com.affymetrix.igb.selectioninfo;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.PropertyHandler;
import com.affymetrix.igb.osgi.service.PropertyListener;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.util.JTableCutPasteAdapter;
import com.affymetrix.igb.view.SeqMapView;

import java.text.NumberFormat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public final class SelectionInfo extends IGBTabPanel implements SymSelectionListener, PropertyHandler {
	private static final long serialVersionUID = 1L;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("selectioninfo");
	private static final List<String> GRAPH_TOOL_TIP_ORDER = new ArrayList<String>();
	static {
		GRAPH_TOOL_TIP_ORDER.add("id");
		GRAPH_TOOL_TIP_ORDER.add("x coord");
		GRAPH_TOOL_TIP_ORDER.add("y coord");
		GRAPH_TOOL_TIP_ORDER.add("min score");
		GRAPH_TOOL_TIP_ORDER.add("max score");
		GRAPH_TOOL_TIP_ORDER.add("strand");
		GRAPH_TOOL_TIP_ORDER.add("A");
		GRAPH_TOOL_TIP_ORDER.add("T");
		GRAPH_TOOL_TIP_ORDER.add("G");
		GRAPH_TOOL_TIP_ORDER.add("C");
		GRAPH_TOOL_TIP_ORDER.add("N");
	}

	private static final List<String> TOOL_TIP_ORDER = new ArrayList<String>();
	static {
		TOOL_TIP_ORDER.add("id");
		TOOL_TIP_ORDER.add("start");
		TOOL_TIP_ORDER.add("end");
		TOOL_TIP_ORDER.add("length");
		TOOL_TIP_ORDER.add("strand");
		TOOL_TIP_ORDER.add("residues");
	}

	// The general order these fields should show up in.
	private static final List<String> PROP_ORDER = new ArrayList<String>(20);
	static {
		PROP_ORDER.add("gene name");
		PROP_ORDER.add("name");
		PROP_ORDER.add("id");
		PROP_ORDER.add("chromosome");
		PROP_ORDER.add("start");
		PROP_ORDER.add("end");
		PROP_ORDER.add("length");
		PROP_ORDER.add("strand");
		PROP_ORDER.add("min score");
		PROP_ORDER.add("max score");
		PROP_ORDER.add("type");
		PROP_ORDER.add("same orientation");
		PROP_ORDER.add("query length");
		PROP_ORDER.add("# matches");
		PROP_ORDER.add("# target inserts");
		PROP_ORDER.add("# target bases inserted");
		PROP_ORDER.add("# query bases inserted");
		PROP_ORDER.add("# query inserts");
		PROP_ORDER.add("seq id");
		PROP_ORDER.add("cds min");
		PROP_ORDER.add("cds max");
		PROP_ORDER.add("description");
		PROP_ORDER.add("loadmode");
		PROP_ORDER.add("feature url");
	}

	/**
	 *
	 * @author hiralv
	 */
	public static class PropertyViewHelper extends DefaultTableCellRenderer implements
				MouseListener, MouseMotionListener {
			private static final long serialVersionUID = 1L;
			private static final Border selectedBorder = BorderFactory.createMatteBorder(2,2,2,2,
                    Color.black);

			private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
			private final Cursor defaultCursor = null;
			private final JTable table;

			public PropertyViewHelper(JTable table){
				this.table = table;
				table.addMouseListener(this);
				table.addMouseMotionListener(this);
			}
			
			@Override
			public Component getTableCellRendererComponent (JTable table,
					Object obj, boolean isSelected, boolean hasFocus, int row, int column){
				JLabel tableCellRendererComponent;
				if(isURLField(row,column)){

					String url = "<html> <a href='" + (String)obj + "'>" +
							(String)obj + "</a> </html>)";

					tableCellRendererComponent = (JLabel)super.getTableCellRendererComponent (table, url,
							isSelected, hasFocus, row, column);
				}

				tableCellRendererComponent = (JLabel)super.getTableCellRendererComponent (table, obj, isSelected,
						hasFocus, row, column);
				if (row % 2 == 0) {
					tableCellRendererComponent.setBackground(Color.LIGHT_GRAY);
				}
				else {
					tableCellRendererComponent.setBackground(Color.WHITE);
//					tableCellRendererComponent.setBorder(BorderFactory.createCompoundBorder(
//						tableCellRendererComponent.getBorder(), // outside border, your Label Border
//						BorderFactory.createEmptyBorder(5, 5, 5, 5)));
				}
				tableCellRendererComponent.setHorizontalAlignment(SwingConstants.CENTER);
				tableCellRendererComponent.setVerticalAlignment(SwingConstants.TOP);
				setBorder(selectedBorder);
				return tableCellRendererComponent;
			}

			@Override
			public void mouseClicked(MouseEvent e){

				Point p = e.getPoint();
				int row = table.rowAtPoint(p);
				int column = table.columnAtPoint(p);

				if (isURLField(row,column)) {
					GeneralUtils.browse((String) table.getValueAt(row, column));
				}

			}
			public void mouseMoved(MouseEvent e) {
				Point p = e.getPoint();
				int row = table.rowAtPoint(p);
				int column = table.columnAtPoint(p);

				if(isURLField(row,column)){
					table.setCursor(handCursor);
				}else if(table.getCursor() != defaultCursor) {
					table.setCursor(defaultCursor);
				}
			}

			public void mouseDragged(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}

			private boolean isURLField(int row, int column){

				if(row > table.getRowCount() || column > table.getColumnCount() ||
						row < 0 || column < 0)
					return false;
				
				String value = (String) table.getValueAt(row, column);

				if(value.length() <= 0)
					return false;

				if(value.startsWith("http://") || value.startsWith("https://"))
					return true;

				return false;
			};
	}
	// the table showing name-value pairs
	private final JTable table;
	private final JScrollPane scroll_pane;
	public static final String PROPERTY = "property";
	public static final String DEFAULT_TITLE = "Property Sheet";
	private final PropertyViewHelper helper;
	Set<PropertyListener> propertyListeners = new HashSet<PropertyListener>();

	public SelectionInfo(IGBService igbService) {
		super(igbService, BUNDLE.getString("selectionInfoTab"), BUNDLE.getString("selectionInfoTab"), false, 2);
		table = new JTable();
		scroll_pane = new JScrollPane();
		JViewport jvp = new JViewport();
		scroll_pane.setColumnHeaderView(jvp);
		helper = new PropertyViewHelper(table);
		new JTableCutPasteAdapter(table, true);
		this.setPreferredSize(new java.awt.Dimension(100, 250));
		this.setMinimumSize(new java.awt.Dimension(100, 250));
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
		propertyListeners.add(((SeqMapView)igbService.getMapView()).getMouseListener());
	}

	public void symSelectionChanged(SymSelectionEvent evt) {
		Object src = evt.getSource();
		// if selection event originally came from here, then ignore it...
		if (src == this) {
			return;
		}
		SeqMapView mapView = null;
		TierLabelManager tlm = null;

		if (src instanceof SeqMapView) {
			mapView = (SeqMapView) src;
			tlm = mapView.getTierManager();
		} else if (src instanceof TierLabelManager){
			tlm = (TierLabelManager)src;
		}

		showSyms(evt.getSelectedSyms(), mapView, tlm);
	}

	@SuppressWarnings("unchecked")
	private void showSyms(List<SeqSymmetry> selected_syms, SeqMapView seqMap, TierLabelManager tlm) {
		List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();
		for (SeqSymmetry sym : selected_syms) {
			Map<String, Object> props = determineProps(sym, seqMap);
			if (props != null) {
				propList.add(props);
			}

		}

		if(seqMap != null)
			addGlyphInfo(propList, seqMap.getSeqMap().getSelected(), selected_syms);

		if(tlm != null)
			addTierInfo(propList, tlm);

		Map<String, Object>[] prop_array = propList.toArray(new Map[propList.size()]);

		this.showProperties(prop_array, PROP_ORDER, "");
	}

	public static void addTierInfo(List<Map<String, Object>> propList, TierLabelManager handler){
		List<Map<String, Object>> tierProp = handler.getTierProperties();

		if(!tierProp.isEmpty()){
			propList.addAll(tierProp);
		}
	}


	@SuppressWarnings("unchecked")
	private static void addGlyphInfo(List<Map<String, Object>> propList, List<GlyphI> glyphs, List<SeqSymmetry> selected_syms){
		for (GlyphI glyph : glyphs) {

			if (glyph.getInfo() instanceof SeqSymmetry
					&& selected_syms.contains((SeqSymmetry) glyph.getInfo())) {
				continue;
			}

			Map<String, Object> props = null;
			if(glyph.getInfo() instanceof Map){
				 props = (Map<String, Object>) glyph.getInfo();
			} else {
				props = new HashMap<String, Object>();
			}

			boolean direction = true;
			if(props.containsKey("direction")){
				if(((String)props.get("direction")).equals("reverse"))
					direction = false;
			}

			Rectangle2D.Double boundary = glyph.getSelectedRegion();
			int start = (int) boundary.getX();
			int length = (int) boundary.getWidth();
			int end = start + length;
			if(!direction){
				int temp = start;
				start = end;
				end = temp;
			}
			props.put("start", start);
			props.put("end", end);
			props.put("length", length);

			propList.add(props);
		}
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
			props = new HashMap<String, Object>();
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
					props.put("strand",
							span.isForward() ? "+" : "-");
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
		// for debugging only - if that wasn't obvious
		props.put("property", "something goes in here and the text wraps to fit the available space so that users don't have to change the width of the side panel");
		//
		return props;
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

	private static String[][] getPropertiesRow(Map<String, Object>[] props, final List<String> preferred_prop_order, String noData){
		ArrayList<String> rowList = new ArrayList<String>();
		for (int row = 0; row < props.length; row++) {
			Map<String, Object> thisProps = props[row];
			Object id = thisProps.get("id");
			if(id != null){
				rowList.add("<html><span width=\"100%\" align=\"center\" style=\"text-align:center;\">" + id.toString() + "</span></html>");
			}
			List<String> listProps = new ArrayList<String>(thisProps.keySet());
			Collections.sort(listProps, new Comparator<String>() {
			    public int compare(String o1, String o2) {
			    	int i1 = preferred_prop_order.indexOf(o1);
			    	if (i1 == -1) {
			    		i1 = Integer.MAX_VALUE;
			    	}
			    	int i2 = preferred_prop_order.indexOf(o2);
			    	if (i2 == -1) {
			    		i2 = Integer.MAX_VALUE;
			    	}
			    	return i1 - i2;
			    }
			}
			);
			rowList.add(convertPropsToString(props[row], preferred_prop_order, false));
//			for (String prop : listProps) {
//				rowList.add("<html><span style=\"word-wrap: break-word;\"><b>" + prop + " : </b>" + ("link".equals(prop) ? "<a href=\"" + thisProps.get(prop) + "\">" : "") + thisProps.get(prop) + ("link".equals(prop) ? "<\\a>" : "") + "</code></span></html>");
//			}
		}
		String[][] propertiesRow = new String[rowList.size()][1];
		for (int i = 0; i < rowList.size(); i++) {
			propertiesRow[i][0] = rowList.get(i);
		}
		return propertiesRow;
	}

	/**
	 * Show data associated with the given properties.
	 * Uses buildRows() to retrieve ordered
	 * name-value pairs.
	 * @param props  the given Properties
	 * @param preferred_prop_order the preferred order of columns
	 * @param noData the value to use when a property value is null
	 */
	private void showProperties(Map<String, Object>[] props, List<String> preferred_prop_order, String noData) {

		String[][] table_data = getPropertiesRow(props,preferred_prop_order, noData);
		String[] col_headings = new String[]{BUNDLE.getString("selectionInfoTab") + " (" + props.length + ")"};
		propertyChanged(props.length + 1);

		TableModel model = new DefaultTableModel(table_data, col_headings) {
			public static final long serialVersionUID = 1l;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		table.setModel(model);
		table.setDefaultRenderer(Object.class, helper);

		table.setEnabled(true);  // to allow selection, etc.
		table.setFillsViewportHeight(true);
		table.setMinimumSize(new Dimension(80000, 500));	//added by Max
		this.removeAll();
		this.setLayout(new BorderLayout());
		scroll_pane.setViewportView(table);
		scroll_pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.add(scroll_pane, BorderLayout.CENTER);
		table.setCellSelectionEnabled(false);

		validate();
		table.getColumnModel().getColumn(0).setMinWidth(100);
		table.getColumnModel().getColumn(0).setPreferredWidth(150);
		table.getColumnModel().getColumn(0).setMaxWidth(200);
	    int height = table.getRowHeight();
		for (int rowIndex = 1; rowIndex < table.getRowCount(); rowIndex += 2) {
			TableCellRenderer renderer = table.getCellRenderer(rowIndex, 0);
        	Component comp = table.prepareRenderer(renderer, rowIndex, 0);
        	int h = comp.getMaximumSize().height;
//        	System.out.println("row = " + rowIndex + ", h = " + h + ", height = " + height);
        	table.setRowHeight(rowIndex, Math.max(height, h));
        }
	}

	@SuppressWarnings("unchecked")
	public String[][] getPropertiesRow(SeqSymmetry sym, JComponent seqMap){
		List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();
		Map<String, Object> props = determineProps(sym, (SeqMapView)seqMap);
		propList.add(props);

		return getPropertiesRow(propList.toArray(new Map[propList.size()]),TOOL_TIP_ORDER,"", true);
	}

	@SuppressWarnings("unchecked")
	public String[][] getGraphPropertiesRowColumn(GraphSym sym, int x, JComponent seqMap){
		List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();
		Map<String, Object> props = determineProps(sym, (SeqMapView)seqMap);
		props.putAll(sym.getLocationProperties(x, ((SeqMapView)seqMap).getVisibleSpan()));
		propList.add(props);
		return getPropertiesRow(propList.toArray(new Map[propList.size()]),GRAPH_TOOL_TIP_ORDER,"",true);
	}

	private static String[][] getPropertiesRow(Map<String, Object>[] props, List<String> preferred_prop_order, String noData, boolean limited){
		List<String[]> name_values = getNameValues(props, noData);
		if (preferred_prop_order != null) {
			name_values = reorderNames(name_values, preferred_prop_order, limited);
		}
		return buildRows(name_values, props);
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
	private static List<String[]> reorderNames(List<String[]> name_values, List<String> preferred_ordering, boolean limited) {
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
   * Fills up a Vector with arrays containing names and values
   * for each of the given Properties.
   * e.g., {name,value0,value1,value2,...,valueN} for
   * N different Properties Objects.
   * @param props  the list of Properties derived from SeqFeatures.
   * @param noData  the String value to use to represent cases where
   *   there is no value of the property for a given key
   */
    private static List<String[]> getNameValues(Map<String, Object>[] props, String noData) {
		List<String[]> result = new ArrayList<String[]>();
		// collect all possible names from the given Properties
		int num_props = props.length;
		Map<String, String[]> rows_thus_far = new HashMap<String, String[]>();
		for (int i = 0; i < num_props; i++) {
			for (String name : props[i].keySet()) {
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

    private static final String LINK = "link";
	private static String convertPropsToString(Map<String, Object> properties, final List<String> preferred_prop_order, boolean shorten){
		if(properties == null)
			return null;

		StringBuilder props = new StringBuilder();
		String value = null;
		props.append("<html>");
		List<String> keys = new ArrayList<String>(properties.keySet());
		Collections.sort(keys,
			new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					int index1 = preferred_prop_order.indexOf(o1);
					if (index1 == -1) {
						index1 = Integer.MAX_VALUE;
					}
					int index2 = preferred_prop_order.indexOf(o2);
					if (index2 == -1) {
						index2 = Integer.MAX_VALUE;
					}
					return index1 - index2;
				}
			}
		);
		for (String key : keys){
			props.append("<b>");
			props.append(key);
			props.append(" : </b>");
			Object obj = properties.get(key);
			if (obj != null){
				value = obj.toString();
				if (shorten) {
					int vallen = value.length();
					props.append(value.substring(0, Math.min(40, vallen)));
					if(vallen > 40) {
						props.append(" ...");
					}
				}
				else {
					if (LINK.equals(key)) {
						props.append("<a href=\"" + value + "\">");
					}
					props.append(value);
					if (LINK.equals(key)) {
						props.append("</a>");
					}
				}
			}
			props.append("<br>");
			if (!shorten) {
				props.append("<hr size=\"1\" style=\"height:1px\" />");
			}
		}
		props.append("</html>");

		return props.toString();
	}

	private void propertyChanged(int prop_displayed){
		for(PropertyListener pl : propertyListeners){
			pl.propertyDisplayed(prop_displayed);
		}
	}
}


