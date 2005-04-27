/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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
import com.affymetrix.igb.util.TableSorter;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.view.SeqMapView;

/**
 *  A panel that shows the hashtable of symmetry items from
 *  {@link IGB#getSymHash()}.  When the user selects an item,
 *  the {@link SeqMapView} will zoom to it.
 */
public class AnnotBrowserView extends JPanel
implements ListSelectionListener, SymMapChangeListener  {

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

    TableSorter sort_model = new TableSorter(model);
    sort_model.addMouseListenerToHeaderInTable(table);

    table.setModel(sort_model);
    table.setRowSelectionAllowed(true);
    table.setEnabled( true );

    //    table.setCellSelectionEnabled(true);
    //    JTableCutPasteAdapter cut_paster = new JTableCutPasteAdapter(table);

    validate();
    IGB.addSymMapChangeListener(this);
  }

  protected Object[][] buildRows(Map props) {
    ArrayList entries = new ArrayList(props.entrySet());
    int num_rows = entries.size();
    int num_cols = 4;
    Object[][] rows = new Object[num_rows][num_cols];
    for (int j = 0 ; j < num_rows ; j++) {
      Map.Entry entry = (Map.Entry) entries.get(j);
      rows[j][0]= entry.getKey().toString();
      SeqSymmetry sym = (SeqSymmetry) entry.getValue();
      SeqSpan span = sym.getSpan(0); // Is this correct?
      if (span!= null) {
        rows[j][1]= new Integer(span.getStart());
        rows[j][2]= new Integer(span.getEnd());
        rows[j][3]= span.getBioSeq().getID() + (span.isForward() ? "+" : "-");
      } else {
        rows[j][1] = new Integer(0);
        rows[j][2] = new Integer(0);
        rows[j][3] = "?";
      }
    }
    return rows;
  }

  /** Re-populates the table with the given Map, which should contain
   *  SeqSymmetry values.
   *  @param props  A Map of String's to SeqSymmetry's.
   */
  public void showSymHash(Map props) {
    Object[][] rows = buildRows(props);
    model.setDataVector(rows, col_headings);
  }

  /** Causes a call to {@link #showSymHash(Map)}.
   *  Normally, this occurs as a result of a call to
   *  {@link IGB#symHashChanged(Object)}.
   */
  public void symMapModified(SymMapChangeEvent evt) {
    showSymHash(evt.getMap());
  }
  
  /** This is called when the user selects a row of the table;
   *  It calls {@link #findSym(SeqSymmetry)}.
   */
  public void valueChanged(ListSelectionEvent evt) {
    boolean old_way = true;
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
      int srow = table.getSelectedRow();
      if (srow >= 0) {
        String id = (String) table.getModel().getValueAt(srow, 0);
        SeqSymmetry sym = (SeqSymmetry)IGB.getSymHash().get(id);
        findSym(sym);
      }
    }
  }

  public final boolean findSym(SeqSymmetry hitsym) {
    boolean found = false;
    if (hitsym != null) {
      SingletonGenometryModel gmodel = IGB.getGenometryModel();
      MutableAnnotatedBioSeq seq = gmodel.getSelectedSeqGroup().getSeq(hitsym);
      if (seq != null) {
        ArrayList symlist = new ArrayList();
        symlist.add(hitsym);
        gmodel.setSelectedSeq(seq);  // event propagation will trigger gviewer to focus on sequence
        gmodel.setSelectedSymmetries(symlist, this);
        found = true;
      }
    }
    return found;
  }

  public void destroy() {
    removeAll();
    IGB.removeSymMapChangeListener(this);
    if (lsm != null) {lsm.removeListSelectionListener(this);}
  }
}
