/**
*   Copyright (c) 2005 Affymetrix, Inc.
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
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.event.SymMapChangeEvent;
import com.affymetrix.igb.event.SymMapChangeListener;
import com.affymetrix.igb.util.TableSorter2;
import com.affymetrix.igb.tiers.TierPreferenceState;
import com.affymetrix.swing.*;

/**
 *  A panel for choosing tier properties for the {@link SeqMapView}.
 */
public class TierPrefsView extends JPanel implements ListSelectionListener  {

  private final JTable table = new JTable();
  private final static String[] col_headings = {"Tier", "Color", "Show", "Separate", "Collapsed", "Order", "Max Depth"};
  private final int COL_TIER_NAME = 0;
  private final int COL_COLOR = 1;
  private final int COL_SHOW = 2;
  private final int COL_SEPARATE = 3;
  private final int COL_COLLAPSED = 4;
  private final int COL_ORDER = 5;
  private final int COL_MAX_DEPTH = 6;
  
  private final TierPrefsTableModel model;
  private final ListSelectionModel lsm;

  public TierPrefsView() {
    super();
    this.setLayout(new BorderLayout());

    JScrollPane scroll_pane = new JScrollPane(table);
    this.add(scroll_pane, BorderLayout.CENTER);
    this.add(scroll_pane);

    model = new TierPrefsTableModel();

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(this);
    lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    TableSorter2 sort_model = new TableSorter2(model);
    sort_model.setTableHeader(table.getTableHeader());
    table.setModel(sort_model);

    table.setRowSelectionAllowed(true);
    table.setEnabled( true );
    
    table.setDefaultRenderer(Color.class, new ColorTableCellRenderer(true));
    table.setDefaultEditor(Color.class, new ColorTableCellEditor());
    table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());

    validate();
  }

  public void setStateList(java.util.List states) {
    model.setStates(states);
  }
  
  /** Called when the user selects a row of the table.
   */
  public void valueChanged(ListSelectionEvent evt) {
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
      int srow = table.getSelectedRow();
    }
  }

  public void destroy() {
    removeAll();
    if (lsm != null) {lsm.removeListSelectionListener(this);}
  }

  
  class TierPrefsTableModel extends AbstractTableModel {
    
    java.util.List tier_states;
    
    TierPrefsTableModel() {
      this.tier_states = Collections.EMPTY_LIST;
    }
    
    public void setStates(java.util.List tier_states) {
      this.tier_states = tier_states;
    }
    
    public java.util.List getStates() {
      return this.tier_states;
    }
    
    public boolean isCellEditable(int row, int column) {
      return (column != COL_TIER_NAME);
    }
    
    public Class getColumnClass(int c) {
      return getValueAt(0, c).getClass();
    }
    
    public int getColumnCount() {
      return col_headings.length;
    }
    
    public String getColumnName(int columnIndex) {
      return col_headings[columnIndex];
    }
    
    public int getRowCount() {
      return tier_states.size();
    }
    
    public Object getValueAt(int row, int column) {
      TierPreferenceState state = (TierPreferenceState) tier_states.get(row);
      switch (column) {
        case COL_COLOR: 
          return state.getColor();
        case COL_SEPARATE: 
          return state.getSeparate();
        case COL_COLLAPSED: 
          return state.getCollapsed();
        case COL_SHOW:  
          return state.getShow();
        case COL_TIER_NAME: 
          return state.getUniqueName();
        case COL_ORDER: 
          return String.valueOf(state.getSortOrder());
        case COL_MAX_DEPTH: 
          int md = state.getMaxDepth();
          if (md == 0) { return "none"; }
          else { return String.valueOf(state.getMaxDepth()); }
        default:
          return null;
      }
    }
    
    public void setValueAt(Object value, int row, int col) {
      TierPreferenceState state = (TierPreferenceState) tier_states.get(row);
      switch (col) {
        case COL_COLOR:
          state.setColor((Color) value);
          break;
        case COL_SEPARATE:
          state.setSeparate(((Boolean) value).booleanValue());
          break;
        case COL_COLLAPSED:
          state.setCollapsed(((Boolean) value).booleanValue());
          break;
        case COL_SHOW:
          state.setShow(((Boolean) value).booleanValue());
          break;
        case COL_TIER_NAME:
          System.out.println("Tier name is not changeable!");
          break;
        case COL_ORDER:
        {
          int i = parseInteger(value, state.getSortOrder());
          if (i != Integer.MIN_VALUE) {
            state.setSortOrder(i);
          }
        }
          break;
        case COL_MAX_DEPTH:
        {
          int i = parseInteger(value, state.getMaxDepth());
          if (i != Integer.MIN_VALUE) {
            state.setMaxDepth(i);
          }
        }
          break;
        default:
          System.out.println("Unknown column selected: " + col);;
      }
      fireTableCellUpdated(row, col);
    }
    
    /** Parse an integer, using the given fallback if any exception occurrs.*/
    int parseInteger(Object o, int fallback) {
      int i = fallback;
      try {
        i = Integer.parseInt((String) o);
      }
      catch (Exception e) {
        // don't report the error, use the fallback value
      }
      return i;
    }
    
  };

  /** Used for testing.  Opens a window with the TierPrefsView in it. */
  public static void main(String[] args) {

    TierPrefsView t = new TierPrefsView();

    Vector v = new Vector();
    v.add(new TierPreferenceState("RefSeq"));
    v.add(new TierPreferenceState("EnsGene"));
    v.add(new TierPreferenceState("Contig"));
    v.add(new TierPreferenceState("KnownGene"));
    v.add(new TierPreferenceState("TwinScan"));
    t.setStateList(v);

    
    JFrame f = new JFrame("TierPrefsView");
    f.getContentPane().add(t);
    
    f.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);
      }
    });
    f.pack();
    f.show();
  }
}


