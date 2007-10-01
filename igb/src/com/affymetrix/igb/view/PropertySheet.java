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

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import com.affymetrix.igb.util.JTableCutPasteAdapter;
import com.affymetrix.igb.util.TableSorter2;
import com.affymetrix.swing.BlockingTableCellEditor;

public class PropertySheet extends JPanel {
    
  // the table showing name-value pairs
  JTable table;
  JScrollPane scroll_pane;
  JTableCutPasteAdapter cutPaster;
  
  Dimension size = new Dimension ( 1000, 1000 );
  int columnwidth;

  boolean by_rows = false;
  boolean sortable = false;
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
   * Tell this PropertySheet whether values for SeqFeature
   * properties should be presented in a row or a column.
   * The default is to present them in a column.
   * @param by_rows - if true, then present them in a row
   */
  public void byRows(boolean by_rows) {
    this.by_rows = by_rows;
  }
  
  /**
   * Whether values for SeqFeature
   * properties should be presented in a row or a column.
   * The default is to present them in a column.
   */
  public boolean isByRows() {
    return this.by_rows;
  }
  
  /**
   *  Specifies whether the user should be able to sort the columns by clicking on the headers.
   *  Note that the sort-order will be lost each time the data is updated because there
   *  is no guarantee that the same type of data, with the same column names, will
   *  be used each time showProperties() is called.
   */
  public void setSortable(boolean b) {
    this.sortable = b;
  }
  
  /** Converts a row index of the view into a row index of the model.
   *  If {@link setSortable(boolean)} was false, then the two indices are
   *  equivalent.
   */
  public int getModelIndex(int i) {
    if (sortable) {
      return ((TableSorter2) table.getModel()).modelIndex(i);
    } else {
      return i;
    }
  }

  public void setUseDefaultKeystrokes(boolean b) {
    this.useDefaultKeystrokes = b;
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
  public String[] getColumnHeadings(Vector name_values,
                                    Map[] props) {
    // will contain number of Properties + 1 if by_rows is false
    // will contain number of values if by_rows is true
    String[] col_headings = null;
    // the number of different name-value groups
    int num_values = name_values.size();
    // the nmber of items being described
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
  public String[][] buildRows(Vector name_values, Map[] props) {
    int num_props = props.length;
    int num_vals = name_values.size();
    String[][] rows = null;
    if (by_rows) {
      rows = new String[num_props][num_vals];
      for (int j = 0 ; j < num_props ; j++) {
        for (int i = 0 ; i < num_vals ; i++) {
          String[] vals = (String[]) name_values.elementAt(i);
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
        String[] vals = (String[]) name_values.elementAt(i);
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
  @SuppressWarnings("unchecked")
  Vector reorderNames(Vector name_values, Vector preferred_ordering, boolean useOnlySpecifiedColumns) {
    Vector reordered = new Vector(name_values.size());
    for (int i=0; i<preferred_ordering.size(); i++) {
      String request = (String)preferred_ordering.elementAt(i);
      for (int k=0; k<name_values.size(); k++) {
        String[] vals = (String[])name_values.elementAt(k);
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
    if (! useOnlySpecifiedColumns) {
      for (int i = 0; i < name_values.size(); i++) {
        if (name_values.elementAt(i) != null) {
          reordered.add(name_values.elementAt(i));
        }
      }
    }
    return reordered;
  }

  /**
   * Show data associated with the given properties.
   * Uses buildRows() to retrieve ordered
   * name-value pairs.
   * @param props  the given Properties
   * @see #buildRows(Vector, Map[])
   */
  public void showProperties(Map[] props) {
    showProperties(props, null);
  }

  /**
   * Show data associated with the given properties.
   * Uses buildRows() to retrieve ordered
   * name-value pairs.
   * @param props  the given Properties
   * @param preferred_prop_order the preferred order of columns
   * @see #buildRows(Vector, Map[])
   */
  public void showProperties(Map[] props, Vector preferred_prop_order) {
    this.showProperties(props, preferred_prop_order, "ND");
  }

  /** If true, then the property sheet will display ONLY the properties
   * whose names are listed. 
   */
  boolean useOnlyDefaultProperties = false;
  
  /** If true, then the property sheet will display ONLY the properties
   * whose names are listed. 
   */
  public void setUseOnlyDefaultProperties(boolean b) {
    useOnlyDefaultProperties = b;
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
  public void showProperties(Map[] props, Vector preferred_prop_order, String noData) {
    PropertyKeys propkeys = new PropertyKeys();

    Vector name_values = propkeys.getNameValues(props, noData);
    if (preferred_prop_order != null) {
      name_values = reorderNames(name_values, preferred_prop_order, useOnlyDefaultProperties);
    }
    String[][] rows = buildRows(name_values,props);
    String[] col_headings = getColumnHeadings(name_values,props);

    TableModel unsorted_model = new DefaultTableModel(rows,col_headings);
    
    if (sortable) {
      TableSorter2 sort_model = new TableSorter2(unsorted_model);
      sort_model.setTableHeader(table.getTableHeader());
      table.setModel(sort_model);
    } else {
      table.setModel(unsorted_model);
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

  public JTable getTable() {
    return table;
  }
  
  /** Returns the current JTableCutPasteAdapter.  May be null. */
  public JTableCutPasteAdapter getCutPasteAdapter() {
    return cutPaster;
  }

  public void destroy() {
    removeAll();
  }

}
