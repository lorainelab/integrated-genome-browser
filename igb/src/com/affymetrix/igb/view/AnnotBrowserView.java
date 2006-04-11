/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.event.GroupSelectionEvent;
import com.affymetrix.igb.event.GroupSelectionListener;
import com.affymetrix.igb.event.SymMapChangeEvent;
import com.affymetrix.igb.event.SymMapChangeListener;
import com.affymetrix.igb.util.TableSorter2;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;

/**
 *  A panel that shows the hashtable of symmetry items from
 *  {@link AnnotatedSeqGroup#getSymmetryIDs()}.  When the user selects an item,
 *  the {@link SeqMapView} will zoom to it.
 */
public class AnnotBrowserView extends JPanel
implements ListSelectionListener, SymMapChangeListener, GroupSelectionListener  {

  private final JTable table = new JTable();
  private final static String[] col_headings = {"ID", "Start", "End", "Sequence"};
  private final DefaultTableModel model;
  private final ListSelectionModel lsm;

  public AnnotBrowserView() {
    super();
    this.setLayout(new BorderLayout());

    JScrollPane scroll_pane = new JScrollPane(table);
    this.add(scroll_pane, BorderLayout.CENTER);
    this.add(scroll_pane);

    model = new DefaultTableModel() {
      public boolean isCellEditable(int row, int column) {return false;}
      public Class getColumnClass(int column) {
        if (column==0 || column==3) return String.class;
        else return Integer.class;
      }
    };
    model.setDataVector(new Object[0][0], col_headings);

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(this);
    lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    TableSorter2 sort_model = new TableSorter2(model);
    //sort_model.addMouseListenerToHeaderInTable(table); // for TableSorter version 1
    sort_model.setTableHeader(table.getTableHeader()); // for TableSorter2

    table.setModel(sort_model);
    table.setRowSelectionAllowed(true);
    table.setEnabled( true );

    //    table.setCellSelectionEnabled(true);
    //    JTableCutPasteAdapter cut_paster = new JTableCutPasteAdapter(table);

    validate();
    AnnotatedSeqGroup.addSymMapChangeListener(this);
    SingletonGenometryModel.getGenometryModel().addGroupSelectionListener(this);
  }

  protected Object[][] buildRows(AnnotatedSeqGroup seq_group) {
    if (seq_group == null) {
      return new Object[0][4];
    }
    
    Set sym_ids = seq_group.getSymmetryIDs();
    java.util.List seq_list = seq_group.getSeqList();
    
    ArrayList entries = new ArrayList(sym_ids);
    int num_rows = entries.size();
    int num_cols = 4;
    Object[][] rows = new Object[num_rows][num_cols];
    for (int j = 0 ; j < num_rows ; j++) {
      String key = (String) entries.get(j);
      rows[j][0]= key;
      Object o = seq_group.findSyms(key);
      
      SeqSymmetry sym;
      int num_matches = 1;
      if (o instanceof java.util.List) {
        // For now, only display the first match of the list
        java.util.List sym_list = (java.util.List) o;
        sym = (SeqSymmetry) sym_list.get(0);
        num_matches = sym_list.size();
      } else {
        sym = (SeqSymmetry) o;
        num_matches = 1;
      }
      
      if (num_matches != 1) {
        rows[j][1] = new Integer(0);
        rows[j][2] = new Integer(0);
        rows[j][3] = "" + num_matches + " matches";
        
      } else {
        
        int span_count = sym.getSpanCount();
        SeqSpan first_span_in_group = null; // first span with a BioSeq in this SeqGroup
        for (int i=0; i<span_count; i++) {
          SeqSpan span = sym.getSpan(i);
          if (span == null) continue;
          
          BioSeq seq = span.getBioSeq();
          if (seq_list.contains(seq)) {
            first_span_in_group = span;
            break;
          }
        }
        
        if (first_span_in_group != null) {
          rows[j][1]= new Integer(first_span_in_group.getStart());
          rows[j][2]= new Integer(first_span_in_group.getEnd());
          rows[j][3]= first_span_in_group.getBioSeq().getID() + (first_span_in_group.isForward() ? "+" : "-");
        } else {
          rows[j][1] = new Integer(0);
          rows[j][2] = new Integer(0);
          rows[j][3] = "?";
        }
      }
    }
    
    return rows;
  }

  /** 
   * Re-populates the table with the given AnnotatedSeqGroup.
   */
  public void showSymHash(AnnotatedSeqGroup seq_group) {
    Object[][] rows = buildRows(seq_group);
    model.setDataVector(rows, col_headings);
  }

  /** Causes a call to {@link #showSymHash(AnnotatedSeqGroup)}.
   *  Normally, this occurs as a result of a call to
   *  {@link AnnotatedSeqGroup#symHashChanged(Object)}.
   */
  public void symMapModified(SymMapChangeEvent evt) {
    showSymHash(evt.getSeqGroup());
  }
  
  public void groupSelectionChanged(GroupSelectionEvent evt) {
    showSymHash(evt.getSelectedGroup());
  }
  
  /** This is called when the user selects a row of the table;
   *  It calls {@link AnnotatedSeqGroup#findSyms(String)}.
   */
  public void valueChanged(ListSelectionEvent evt) {
    boolean old_way = true;
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
      int srow = table.getSelectedRow();
      if (srow >= 0) {
        String id = (String) table.getModel().getValueAt(srow, 0);
        SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
        AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
        java.util.List syms = group.findSyms(id);
        gmodel.setSelectedSymmetriesAndSeq(syms, this);
      }
    }
  }

  public void destroy() {
    removeAll();
    AnnotatedSeqGroup.removeSymMapChangeListener(this);
    if (lsm != null) {lsm.removeListSelectionListener(this);}
  }
}
