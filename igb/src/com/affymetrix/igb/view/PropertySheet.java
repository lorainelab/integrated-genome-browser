/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.view;

import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import com.affymetrix.igb.util.JTableCutPasteAdapter;
import com.affymetrix.swing.BlockingTableCellEditor;
import java.awt.BorderLayout;
import java.awt.Dimension;

class PropertySheet extends JPanel {
    
  // the table showing name-value pairs
  JTable table;
  JScrollPane scroll_pane;
  JTableCutPasteAdapter cutPaster;

  private TableRowSorter<TableModel> sorter;
  
  Dimension size = new Dimension ( 1000, 1000 );
  int columnwidth;

  boolean by_rows = false;
  boolean sortable = true;
  boolean useDefaultKeystrokes = true;
  
  public static final String PROPERTY = "property";
  public static final String DEFAULT_TITLE = "Property Sheet";

  /**
   * Create a new PropertySheet containing no data.
   */
  public PropertySheet(boolean useDefaultKeystrokes) {
    super();
    scroll_pane = new JScrollPane();
    JViewport jvp = new JViewport();
    scroll_pane.setColumnHeaderView(jvp);
    table = new JTable();
    this.useDefaultKeystrokes = useDefaultKeystrokes;
    cutPaster = new JTableCutPasteAdapter(table, useDefaultKeystrokes);
  }

  
  /**
   * Return headings for columns.  If we're laying out
   * values in a row, then column headings will be the
   * names associated with each value.  If we're laying
   * out values in a column, then column headings will
   * be PROPERTY and then labels for the item whose
   * values are being presented.
   * @param name_values - a Vector containing name-values for a
   *   one or more Properties
   * @param props - the list of Properties
   */
  private String[] getColumnHeadings(Vector<String[]> name_values,
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
      for (int i = 0 ; i < num_values ; i++) {
        col_headings[i] = PropertyKeys.getName(name_values,i);
      }
    }
    else {  // columns represent set of properties for a particular entity
      col_headings = new String[num_items+1];
      col_headings[0] = PROPERTY;
      for (int i = 0 ; i < num_items ; i++) {
        Object id_obj = props[i].get("id");
        String id;
        if (id_obj == null) { id = "no ID"; }
        else {id = id_obj.toString(); } // in most cases the id already is a String
        col_headings[i+1] = id;
      }
    }
    return col_headings;
  }


  /**
   * Build and return rows for the table to be shown in
   * this PropertySheet.
   * If there are no Properties to be shown, then returns
   * default rows.
   * @param name_values - a Vector containing name-values for a
   *   one or more Properties
   * @param props  the list of Properties
   */
  private String[][] buildRows(Vector<String[]> name_values, Map[] props) {
    int num_props = props.length;
    int num_vals = name_values.size();
    String[][] rows = null;
    if (by_rows) {
      rows = new String[num_props][num_vals];
      for (int j = 0 ; j < num_props ; j++) {
        for (int i = 0 ; i < num_vals ; i++) {
          String[] vals = name_values.get(i);
          try {
            rows[j][i]=vals[j+1];
          }
          catch (ArrayIndexOutOfBoundsException array_ex) {
            System.out.println("error allocating rows for property sheet.");
          }
        }
      }
    }
    else {
      rows = new String[num_vals][num_props + 1];
      for (int i = 0 ; i < num_vals ; i++) {
        String[] vals = name_values.get(i);
        rows[i][0] = vals[0];
        for (int j = 1 ; j < vals.length ; j++) {
          rows[i][j] = vals[j];
        }
      }
    }
    return rows;
  }


  /**
   * take name_values and return a new Vector that
   *  starts with the names found in preferred_ordering in the specified order,
   *  and then adds entries in name_values that were not found in preferred_ordering.
   *
   *  WARNING! this destroys integrity of original name_values Vector!
   *  also assumes that there are no null entries in name_values Vector
   *
   * @param name_values   a Vector of String[]'s
   * @param preferred_ordering a Vector of Strings with the preferred order of column names
   * @param useOnlySpecifiedColumns If true, then the property sheet will display ONLY the properties
   * whose names are listed. 
   */
  private Vector<String[]> reorderNames(Vector<String[]> name_values, Vector<String> preferred_ordering) {
	  Vector<String[]> reordered = new Vector<String[]>(name_values.size());
		for (String request : preferred_ordering) {
			for (int k = 0; k < name_values.size(); k++) {
				String[] vals = name_values.get(k);
				if (vals != null && vals.length > 0) {
					String name = vals[0];
					if (name.equals(request)) {
						reordered.add(vals);
						name_values.setElementAt(null, k);
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
   * @see #buildRows(Vector, Map[])
   */
  public void showProperties(Map[] props, Vector<String> preferred_prop_order, String noData) {
    PropertyKeys propkeys = new PropertyKeys();

    Vector<String[]> name_values = propkeys.getNameValues(props, noData);
    if (preferred_prop_order != null) {
      name_values = reorderNames(name_values, preferred_prop_order);
    }
    String[][] rows = buildRows(name_values,props);
    String[] col_headings = getColumnHeadings(name_values,props);

    TableModel model = new DefaultTableModel(rows,col_headings);
    table.setModel(model);
    if (sortable) {
		sorter = new TableRowSorter<TableModel>(model);
		table.setRowSorter(sorter);
    }

    table.setEnabled( true );  // to allow selection, etc.
    this.size.height = table.getSize().height;
    //table.setSize ( this.size );
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    this.removeAll();
    this.setLayout(new BorderLayout() );
    scroll_pane = new JScrollPane(table);
    this.add(scroll_pane, BorderLayout.CENTER);
    table.setCellSelectionEnabled(true);
    
    TableCellEditor tce = new BlockingTableCellEditor();    
    table.setDefaultEditor(Object.class, tce);
    table.setCellEditor(tce);

    validate();
    for (int i=0; i<table.getColumnCount(); i++) {
      table.getColumnModel().getColumn(i).setMinWidth(100);
      table.getColumnModel().getColumn(i).setPreferredWidth(150);
    }
  }
}
