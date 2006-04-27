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
import java.awt.event.*;
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
import com.affymetrix.igb.prefs.IPlugin;
import com.affymetrix.swing.IntegerTableCellRenderer;

/**
 *  A panel that shows the hashtable of symmetry items from
 *  {@link AnnotatedSeqGroup#getSymmetryIDs()}.  When the user selects an item,
 *  the {@link SeqMapView} will zoom to it.
 */
public class AnnotBrowserView extends JPanel
implements SymMapChangeListener, GroupSelectionListener, IPlugin  {

  private final JTable table = new JTable();
  
  // The second column in the table contains an object of type SeqSymmetry
  // but we use a special TableCellRenderer so that what is actually displayed
  // is a String representing the Tier
  private final static String[] col_headings = {"ID", "Tier", "Start", "End", "Sequence"};
  private final static Class[] col_classes = {String.class, SeqSymmetry.class, Integer.class, Integer.class, String.class};
  private final static Vector col_headings_vector = new Vector(Arrays.asList(col_headings));
  static final int NUM_COLUMNS = 5;

  private final DefaultTableModel model;
  private final ListSelectionModel lsm;

  JTextField from_tf = new JTextField(8) {
      public Dimension getMaximumSize() {
        return getPreferredSize();
      }
  };
  JTextField to_tf = new JTextField(8) {
      public Dimension getMaximumSize() {
        return getPreferredSize();
      }
  };

  public AnnotBrowserView() {
    super();
    this.setLayout(new BorderLayout());

    Box top_row = Box.createHorizontalBox();
    this.add(top_row, BorderLayout.NORTH);
    
    top_row.add(Box.createRigidArea(new Dimension(6, 30)));
    top_row.add(new JLabel("Find ids from:"));
    top_row.add(Box.createRigidArea(new Dimension(10, 30)));
    top_row.add(from_tf);
    top_row.add(Box.createRigidArea(new Dimension(10, 30)));
    top_row.add(new JLabel("to:"));
    top_row.add(Box.createRigidArea(new Dimension(10, 30)));
    top_row.add(to_tf);
    top_row.add(Box.createHorizontalGlue());
        
    JScrollPane scroll_pane = new JScrollPane(table);
    this.add(scroll_pane, BorderLayout.CENTER);

    model = new DefaultTableModel() {
      public boolean isCellEditable(int row, int column) {return false;}
      public Class getColumnClass(int column) {
        return col_classes[column];
      }
      
      public void fireTableStructureChanged() {
        // The columns never change, so suppress tableStructureChanged events
        // converting to normal table-rows-changed-type events.
        // This allows the column-based sorting settings to be preserved when
        // the data changes.
        fireTableChanged(new javax.swing.event.TableModelEvent(this));
      }
    };
    model.setDataVector(new Vector(0), col_headings_vector);

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(list_selection_listener);
    lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    TableSorter2 sort_model = new TableSorter2(model);
    //sort_model.addMouseListenerToHeaderInTable(table); // for TableSorter version 1
    sort_model.setTableHeader(table.getTableHeader()); // for TableSorter2

    table.setModel(sort_model);
    table.setRowSelectionAllowed(true);
    table.setEnabled( true );
    table.setDefaultRenderer(Integer.class, new IntegerTableCellRenderer());
    table.setDefaultRenderer(SeqSymmetry.class, new SeqSymmetryTableCellRenderer());

    //    table.setCellSelectionEnabled(true);
    //    JTableCutPasteAdapter cut_paster = new JTableCutPasteAdapter(table);

    validate();
    AnnotatedSeqGroup.addSymMapChangeListener(this);
    SingletonGenometryModel.getGenometryModel().addGroupSelectionListener(this);
    
    from_tf.addActionListener(text_action_listener);
    to_tf.addActionListener(text_action_listener);
    from_tf.addFocusListener(text_focus_listener);
    to_tf.addFocusListener(text_focus_listener);
  }

  protected Vector buildRows(AnnotatedSeqGroup seq_group, String start, String end) {
    if (seq_group == null) {
      return new Vector(0);
    }

    Set sym_ids;
    // if end<start, then switch the order of the search,
    // but don't do that if start or end is blank, because 
    // "a" to "" is different from "" to "a" and both searches are valid
    if (start.length() > 0 && end.length() > 0 && start.compareTo(end) > 0) {
      sym_ids = seq_group.getSymmetryIDs(end, start);
    } else {
      sym_ids = seq_group.getSymmetryIDs(start, end);
    }
    java.util.List seq_list = seq_group.getSeqList();
    
    java.util.List entries = new ArrayList(sym_ids);
    int num_rows = entries.size();
    Vector rows = new Vector(num_rows, num_rows/10);
    for (int j = 0 ; j < num_rows ; j++) {
      String key = (String) entries.get(j);
      java.util.List the_list = seq_group.findSyms(key);
            
      for (int k=0; k<the_list.size(); k++) {
        Vector a_row = new Vector(NUM_COLUMNS);
        a_row.add(key);
        SeqSymmetry sym = (SeqSymmetry) the_list.get(k);
        a_row.add(sym);

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
          a_row.add(new Integer(first_span_in_group.getStart()));
          a_row.add(new Integer(first_span_in_group.getEnd()));
          String s = first_span_in_group.getBioSeq().getID() + (first_span_in_group.isForward() ? "+" : "-");
          a_row.add(s);
          rows.add(a_row);
        }
      }
    }
    
    return rows;
  }

  
  /** 
   * Re-populates the table with the given AnnotatedSeqGroup.
   */
  public void showSymHash(AnnotatedSeqGroup seq_group) {
    final AnnotatedSeqGroup final_seq_group = seq_group;
    final String start = from_tf.getText().trim().toLowerCase();
    final String end = to_tf.getText().trim().toLowerCase();
    Thread thread = new Thread() {
      public void run() {    
        final Vector rows = buildRows(final_seq_group, start, end);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            model.setDataVector(rows, col_headings_vector);
          }
        });
      }
    };

    thread.start();
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
  
  // Redraws the table in response to events in the text fields and buttons.
  ActionListener text_action_listener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      showSymHash(SingletonGenometryModel.getGenometryModel().getSelectedSeqGroup());
    }
  };
  
  // Redraws the table in response to events in the text fields and buttons.
  FocusListener text_focus_listener = new FocusAdapter() {
    public void focusLost(FocusEvent e) {
      showSymHash(SingletonGenometryModel.getGenometryModel().getSelectedSeqGroup());
    }          
  };
  
  /** This is called when the user selects a row of the table;
   *  It calls {@link AnnotatedSeqGroup#findSyms(String)}.
   */
  ListSelectionListener list_selection_listener = new ListSelectionListener() {
    public void valueChanged(ListSelectionEvent evt) {
      boolean old_way = true;
      if (evt.getSource()==lsm && ! evt.getValueIsAdjusting() && model.getRowCount() > 0) {
        int srow = table.getSelectedRow();
        if (srow >= 0) {
          Object o = table.getModel().getValueAt(srow, 0);
          SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
          java.util.List syms = new ArrayList(1);
          syms.add((SeqSymmetry) table.getModel().getValueAt(srow, 1));
          gmodel.setSelectedSymmetriesAndSeq(syms, this);
        }
      }
    }
  };

  public void destroy() {
    removeAll();
    AnnotatedSeqGroup.removeSymMapChangeListener(this);
    if (lsm != null) {lsm.removeListSelectionListener(list_selection_listener);}
  }
  
  /** Main method for testing visual layout. */
  public static void main(String[] args) {
    AnnotBrowserView testview = new AnnotBrowserView();
    JFrame frm = new JFrame();
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", testview);
    frm.setSize(new Dimension(400, 400));
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) { System.exit(0);}
    });
    frm.show();
  }

  // implementation of IPlugin
  public void putPluginProperty(Object key, Object value) {
  }

  // implementation of IPlugin
  public Object getPluginProperty(Object o) {
    if (IPlugin.TEXT_KEY_ICON.equals(o)) {
      return com.affymetrix.igb.menuitem.MenuUtil.getIcon("toolbarButtonGraphics/general/Find16.gif");
    }
    return null;
  }
  
  public static class SeqSymmetryTableCellRenderer extends DefaultTableCellRenderer {
    public SeqSymmetryTableCellRenderer() {
      super();
    }
    
    protected void setValue(Object value) {
      SeqSymmetry sym = (SeqSymmetry) value;
      super.setValue(SeqMapView.determineMethod(sym));
    }
  }
}
