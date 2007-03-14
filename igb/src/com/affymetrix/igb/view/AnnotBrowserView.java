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
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.event.GroupSelectionEvent;
import com.affymetrix.igb.event.GroupSelectionListener;
import com.affymetrix.igb.event.SymMapChangeEvent;
import com.affymetrix.igb.event.SymMapChangeListener;
import com.affymetrix.igb.util.TableSorter2;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.menuitem.MenuUtil;
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

  public Action search_action = new AbstractAction("Find Annotations...") {
    public void actionPerformed(ActionEvent e) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            performSearch();
          } catch (InterruptedException ex) {
            setStatus("Search interrupted");
          }
        }
      });
    }
  };
  JButton go_b = new JButton(search_action);

  JLabel status_bar = new JLabel("0 results");
  
  // Helps to figure out when the selected group has changed
  int current_group_hash_number = 0;

  FindAnnotationsPanel finder;
  
  public AnnotBrowserView() {
    super();
    super.setName("Annotation Browser");

    finder = new FindAnnotationsPanel();
    finder.initialize();
    
    this.setLayout(new BorderLayout());
    this.setBorder(BorderFactory.createEtchedBorder());

    Box top_row = Box.createHorizontalBox();
    this.add(top_row, BorderLayout.NORTH);
    
    top_row.add(Box.createRigidArea(new Dimension(6, 30)));
    top_row.add(go_b);
    top_row.add(Box.createRigidArea(new Dimension(6, 30)));
        
    JScrollPane scroll_pane = new JScrollPane(table);
    this.add(scroll_pane, BorderLayout.CENTER);
    
    Box bottom_row = Box.createHorizontalBox();
    this.add(bottom_row, BorderLayout.SOUTH);

    bottom_row.add(status_bar);
    
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
    sort_model.setColumnComparator(SeqSymmetry.class, new SeqSymmetryMethodComparator());

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
    
    MenuUtil.addToMenu("View", new JMenuItem(search_action));
  }

  public static final int THE_LIMIT = Integer.MAX_VALUE;
  
  protected Vector buildRows(AnnotatedSeqGroup seq_group, String start, String end) {
    if (seq_group == null) {
      return new Vector(0);
    }

    Set sym_ids = finder.searchForID(seq_group);

    //java.util.List seq_list = seq_group.getSeqList();
    
    java.util.List entries = new ArrayList(sym_ids);
    int num_rows = entries.size();
    
    Vector rows = new Vector(num_rows, num_rows/10);
    for (int j = 0 ; j < num_rows && rows.size() < THE_LIMIT ; j++) {
      String key = (String) entries.get(j);
      java.util.List the_list = seq_group.findSyms(key);
      
      for (int k=0; k<the_list.size(); k++) {
        SeqSymmetry sym = (SeqSymmetry) the_list.get(k);

        int span_count = sym.getSpanCount();
        for (int i=0; i<span_count; i++) {
          SeqSpan span = sym.getSpan(i);
          if (span == null) continue;

          BioSeq seq = span.getBioSeq();
          if (finder.filterBySequence(seq)) /* (seq_list.contains(seq)*/ {
        
//        if (finder.filterBySpan(span)) {
              Vector a_row = new Vector(NUM_COLUMNS);
              a_row.add(key);
              a_row.add(sym);
              a_row.add(new Integer(span.getStart()));
              a_row.add(new Integer(span.getEnd()));
              String s = span.getBioSeq().getID() + (span.isForward() ? "+" : "-");
              a_row.add(s);
              rows.add(a_row);
//            }
          }
        }
      }
    }
    
    return rows;
  }

  // Clear the table (using invokeLater)
  void clearTable(final String text) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        model.setDataVector(new Vector(0), col_headings_vector);
        status_bar.setText(text);
      }
    });
  }
    
  /** 
   * Creates a Thread that will perform the search based on the settings in
   * the search criteria panel.
   */
  Thread doSearch(AnnotatedSeqGroup seq_group) {
    final AnnotatedSeqGroup final_seq_group = seq_group;
    current_group_hash_number = (seq_group == null ? 0 : seq_group.hashCode());
    
    final String start = "";
    final String end = "";
    Thread thread = new Thread() {
      public void run() {
        search_action.setEnabled(false);
        clearTable("Working...");        
        
        final Vector rows = buildRows(final_seq_group, start, end);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            model.setDataVector(rows, col_headings_vector);
            int num_results = rows.size();
            if (rows.size() >= THE_LIMIT) {
              setStatus("More than " + THE_LIMIT + " results");
            } else {
              setStatus("" + rows.size() + " results");
            }
            search_action.setEnabled(true);
            
            IGB.ensureComponentIsShowing(AnnotBrowserView.this);
          }
        });
      }
    };

    return thread;
  }

  /** Set the text in the status bar in a thread-safe way. */
  void setStatus(final String text) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        status_bar.setText(text);
      }
    });
  }

  /** Causes a call to {@link #showSymHash(AnnotatedSeqGroup)}.
   * }
   *  Normally, this occurs as a result of a call to
   *  {@link AnnotatedSeqGroup#symHashChanged(Object)}.
   */
  public void symMapModified(SymMapChangeEvent evt) {
    //showSymHash(evt.getSeqGroup());
    setStatus("Data modified, search again");
  }
  
  public void groupSelectionChanged(GroupSelectionEvent evt) {
    //showSymHash(evt.getSelectedGroup());
    
    int hash_number = (evt.getSelectedGroup() == null ? 0 : evt.getSelectedGroup().hashCode());
    if (model.getDataVector().size() > 0) {
      if (hash_number != current_group_hash_number) {
        clearTable("Data modified, search again");
      } else {
        setStatus("Data modified, search again");
      }
    }
    current_group_hash_number = hash_number;
  }
  
  /** Brings-up a dialog to specify search parameters and then performs the search.
   *  This method will not return until the search is finished.
   */
  public void performSearch() throws InterruptedException {
    finder.reinitialize(SingletonGenometryModel.getGenometryModel());

    String[] options = new String[] {"OK", "Cancel"};
    int result = JOptionPane.showOptionDialog(AnnotBrowserView.this, finder, "Search",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, (Icon) null,
        options, options[0]);

    if (result == 0) {
      Thread thread = doSearch(SingletonGenometryModel.getGenometryModel().getSelectedSeqGroup());
      thread.start();
    }
  }
    
  /** This is called when the user selects a row of the table. */
  ListSelectionListener list_selection_listener = new ListSelectionListener() {
    public void valueChanged(ListSelectionEvent evt) {
      if (evt.getSource()==lsm && ! evt.getValueIsAdjusting() && model.getRowCount() > 0) {
        int srow = table.getSelectedRow();
        if (srow >= 0) {
          //Object o = table.getModel().getValueAt(srow, 0);
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
    frm.setVisible(true);
  }

  // implementation of IPlugin
  public void putPluginProperty(Object key, Object value) {
  }

  // implementation of IPlugin
  public Object getPluginProperty(Object o) {
    if (IPlugin.TEXT_KEY_ICON.equals(o)) {
      //return com.affymetrix.igb.menuitem.MenuUtil.getIcon("toolbarButtonGraphics/general/Find16.gif");
      return null; // suppress the icon until more of the plugins are using icons
    }
    return null;
  }
  
  /** A renderer that displays the value of {@link SeqMapView#determineMethod(SeqSymmetry)}. */
  public static class SeqSymmetryTableCellRenderer extends DefaultTableCellRenderer {
    public SeqSymmetryTableCellRenderer() {
      super();
    }
    
    protected void setValue(Object value) {
      SeqSymmetry sym = (SeqSymmetry) value;
      super.setValue(SeqMapView.determineMethod(sym));
    }
  }

  /** A Comparator that compares based on {@link SeqMapView#determineMethod(SeqSymmetry)}. */
  public static class SeqSymmetryMethodComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      SeqSymmetry s1 = (SeqSymmetry) o1;
      SeqSymmetry s2 = (SeqSymmetry) o2;
      return SeqMapView.determineMethod(s1).compareTo(SeqMapView.determineMethod(s2));
    }
  }
}
