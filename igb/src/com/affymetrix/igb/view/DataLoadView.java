/**
*   Copyright (c) 2005-2007 Affymetrix, Inc.
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.CompositeNegSeq;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.swing.DisplayUtils;

public class DataLoadView extends JComponent  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  static boolean USE_QUICKLOAD = false;
  static boolean USE_DAS2_VIEW = false;
  static boolean USE_DAS2_VIEW3 = true;
  static boolean USE_DAS1_VIEW = false;

  Das2LoadView das2_view;
  Das2LoadView3 das2_view3;
  DasLoadView das1_view;
  QuickLoadView2 quick_view;
  SeqGroupView group_view;

  public DataLoadView() {
    group_view = new SeqGroupView();
    
    //gmodel.addModelChangeListener(group_view);

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    
    JPanel main_panel = new JPanel();
    this.add(main_panel);
    this.setBorder(BorderFactory.createEtchedBorder());
    
    main_panel.setLayout(new BorderLayout());
    main_panel.add("West", group_view);
    
    JTabbedPane tpane = new JTabbedPane();
    main_panel.add("Center", tpane);
    if (USE_QUICKLOAD)  { 
      quick_view = new QuickLoadView2();
      quick_view.setBorder(BorderFactory.createEtchedBorder());
      tpane.addTab("QuickLoad", quick_view); 
    }
    if (USE_DAS2_VIEW) {
      das2_view = new Das2LoadView();
      tpane.addTab("DAS/2", das2_view);
    }
    if (USE_DAS2_VIEW3) {
      das2_view3 = new Das2LoadView3();
      tpane.addTab("Annotation Types", das2_view3);
    }
    if (USE_DAS1_VIEW) {
      das1_view = new DasLoadView();
      tpane.addTab("DAS/1", das1_view);
    }
  }

  public void initialize() {
    if (USE_QUICKLOAD)  { quick_view.initialize(); }
  }
}

class SeqGroupView extends JComponent
  implements ListSelectionListener, GroupSelectionListener, SeqSelectionListener,
  ItemListener /*, GenometryModelChangeListener */ {

  static boolean DEBUG_EVENTS = false;
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static final String NO_GENOME = "No Genome Selected";

  JTable seqtable;
  AnnotatedBioSeq selected_seq = null;
  ListSelectionModel lsm;
  //JLabel genomeL;
  JComboBox genomeCB;

  public SeqGroupView() {
    seqtable = new JTable();
    //genomeL = new JLabel(NO_GENOME);
    //genomeL.setFont(genomeL.getFont().deriveFont(Font.BOLD));
    seqtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    genomeCB = new JComboBox() {
      public Dimension getMaximumSize() {
        return new Dimension(
          super.getMaximumSize().width,
          getPreferredSize().height);
      }
    };

    JScrollPane scroller = new JScrollPane(seqtable);
    scroller.setBorder(BorderFactory.createCompoundBorder(
      scroller.getBorder(),
      BorderFactory.createEmptyBorder(0,2,0,2)));

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.add(genomeCB);
    this.add(Box.createRigidArea(new Dimension(0, 5)));
    this.add(scroller);
    
    this.setBorder(BorderFactory.createTitledBorder("Current Genome"));
    gmodel.addGroupSelectionListener(this);
    gmodel.addSeqSelectionListener(this);
    lsm = seqtable.getSelectionModel();
    lsm.addListSelectionListener(this);
    genomeCB.addItemListener(this);
  }

  String most_recent_seq_id = null;

  public void groupSelectionChanged(GroupSelectionEvent evt) {
    //    AnnotatedSeqGroup group = (AnnotatedSeqGroup)evt.getSelectedGroups().get(0);
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    if (this.DEBUG_EVENTS)  {
      System.out.println("SeqGroupView received groupSelectionChanged() event");
      if (group == null)  { System.out.println("  group is null"); }
      else  {
        System.out.println("  group: " + group.getID());
        System.out.println("  seq count: " + group.getSeqCount());
      }
    }

    if (group == null) {
      //genomeL.setText(NO_GENOME);
      genomeCB.setSelectedIndex(-1);
    } else {
      String group_id = group.getID();
      //genomeL.setText(group_id);
      addItemToComboBox(genomeCB, group_id);
      genomeCB.setSelectedItem(group_id);
    }
    SeqGroupTableModel mod = new SeqGroupTableModel(group);
    selected_seq = null;
    seqtable.setModel(mod);

    // Uncomment this to allow the user to re-sort the table.
    // It turns out to not work very well since it sorts by String sort order
    // when something more complex is needed.
    //
    //TableSorter2 sort_model = new TableSorter2(mod);
    //sort_model.setTableHeader(seqtable.getTableHeader());
    //seqtable.setModel(sort_model);
    
    seqtable.validate();
    seqtable.repaint();
    
    if (group != null) {
      // When changing genomes, try to keep the same chromosome selected when possible
      MutableAnnotatedBioSeq aseq = group.getSeq(most_recent_seq_id);
      if (aseq != null) {
        gmodel.setSelectedSeq(aseq);
      }
    }
  }
  
  // add an item to a combo box iff it isn't already included
  void addItemToComboBox(JComboBox cb, Object item) {
    for (int i=0; i<cb.getItemCount(); i++) {
      Object o = cb.getItemAt(i);
      if (o.equals(item)) {
        return;
      }
    }
    cb.addItem(item);
  }

  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (this.DEBUG_EVENTS)  { System.out.println("SeqGroupView received seqSelectionChanged() event"); }
    synchronized (seqtable) {  // or should synchronize on lsm?
     // if (selected_seq != evt.getSelectedSeq()) {
	lsm.removeListSelectionListener(this);
	//selected_seq = gmodel.getSelectedSeq();
        selected_seq = evt.getSelectedSeq();
	if (selected_seq == null) {
          seqtable.clearSelection(); 
        }
        else  {
          most_recent_seq_id = selected_seq.getID();
          
          for (int i=0; i<seqtable.getRowCount(); i++) {
            // should be able to use == here instead of equals(), because table's model really returns seq.getID()
            if (most_recent_seq_id ==  seqtable.getValueAt(i, 0)) {
              if (seqtable.getSelectedRow() != i) {
                seqtable.setRowSelectionInterval(i, i);
                scrollTableLater(seqtable, i);
              }
              break;
            }
          }
        }
	lsm.addListSelectionListener(this);
     // }
    }
  }

  // Scroll the table such that the selected row is visible
  void scrollTableLater(final JTable table, final int i) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        // Check the row count first since this is multi-threaded
        if (table.getRowCount() >= i) {
          DisplayUtils.scrollToVisible(table, i, 0);
        }
      }
    });
  }
  
  public void valueChanged(ListSelectionEvent evt) {
    Object src = evt.getSource();
    if ((src == lsm) && (! evt.getValueIsAdjusting())) { // ignore extra messages
      if (this.DEBUG_EVENTS)  { System.out.println("SeqGroupView received valueChanged() ListSelectionEvent"); }
      int srow = seqtable.getSelectedRow();
      if (srow >= 0)  {
        String seq_name = (String) seqtable.getModel().getValueAt(srow, 0);
        selected_seq = gmodel.getSelectedSeqGroup().getSeq(seq_name);
        if (selected_seq != gmodel.getSelectedSeq()) {
          gmodel.setSelectedSeq( (MutableAnnotatedBioSeq) selected_seq);
        }
      }
    }
  }

  public Dimension getMinimumSize() { return new Dimension(200, 50); }
  public Dimension getPreferredSize() { return new Dimension(200, 50); }

  public void itemStateChanged(ItemEvent e) {
    if (e.getSource() == genomeCB && e.getStateChange() == ItemEvent.SELECTED) {
      String genome_id = (String) e.getItem();
      if (genome_id != null) {
        AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_id);
        gmodel.setSelectedSeqGroup(group);
      }
    }
  }

//  public void genometryModelChanged(GenometryModelChangeEvent evt) {
//    AnnotatedSeqGroup group = evt.getSeqGroup();
//    if (evt.getType().equals(GenometryModelChangeEvent.SEQ_GROUP_ADDED)) {
//      genomeCB.addItem(group.getID());
//    } else if (evt.getType().equals(GenometryModelChangeEvent.SEQ_GROUP_REMOVED)) {
//      genomeCB.removeItem(group.getID()); 
//    }
//  }

}


class SeqGroupTableModel extends AbstractTableModel  {
  AnnotatedSeqGroup group;

  public SeqGroupTableModel(AnnotatedSeqGroup seq_group) {
    group = seq_group;
  }

  public int getRowCount() { return (group == null ? 0 : group.getSeqCount()); }

  public int getColumnCount() { return 2; }
  
  public Object getValueAt(int row, int col) {
    if (group != null) {
      MutableAnnotatedBioSeq seq = group.getSeq(row);
      if (col == 0) {
        return seq.getID();
      }
      else if (col == 1) {
	if (seq instanceof CompositeNegSeq) {
	  return Long.toString((long)((CompositeNegSeq)seq).getLengthDouble()); 
	}
	else {
	  return Integer.toString(seq.getLength());
	}
      }
    }
    return null;
  }

  public String getColumnName(int col) {
    if (col == 0) { return "Sequence"; }
    else if (col == 1) { return "Length"; }
    else { return null; }
  }
}
