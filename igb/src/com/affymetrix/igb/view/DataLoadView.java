/**
*   Copyright (c) 2005-2006 Affymetrix, Inc.
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
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.tree.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.swing.DisplayUtils;

public class DataLoadView extends JComponent  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  Das2LoadView das2_view;
  DasLoadView das1_view;
  QuickLoadView2 quick_view;
  SeqGroupView group_view;

  public DataLoadView() {
    das2_view = new Das2LoadView();
    das1_view = new DasLoadView();
    quick_view = new QuickLoadView2();
    group_view = new SeqGroupView();

    this.setLayout(new BorderLayout());
    JTabbedPane tpane = new JTabbedPane();
    this.add("Center", tpane);
    tpane.addTab("QuickLoad", quick_view);
    tpane.addTab("DAS/2", das2_view);
    //    tpane.addTab("DAS/1", das1_view);
    this.add("West", group_view);
  }

  public void initialize() {
    quick_view.initialize();
  }
}

class SeqGroupView extends JComponent
  implements ListSelectionListener, GroupSelectionListener, SeqSelectionListener {

  static boolean DEBUG_EVENTS = false;
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static final String NO_GENOME = "No Genome Selected";
  
  JTable seqtable;
  AnnotatedBioSeq selected_seq = null;
  ListSelectionModel lsm;
  JLabel genomeL;

  public SeqGroupView() {
    seqtable = new JTable();
    genomeL = new JLabel(NO_GENOME);
    genomeL.setFont(genomeL.getFont().deriveFont(Font.BOLD));
    seqtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.setLayout(new BorderLayout());
    this.add("Center", new JScrollPane(seqtable));
    this.add("North", genomeL);
    this.setBorder(new TitledBorder("Current Genome"));
    gmodel.addGroupSelectionListener(this);
    gmodel.addSeqSelectionListener(this);
    lsm = seqtable.getSelectionModel();
    lsm.addListSelectionListener(this);
  }


  public void groupSelectionChanged(GroupSelectionEvent evt) {
    //    AnnotatedSeqGroup group = (AnnotatedSeqGroup)evt.getSelectedGroups().get(0);
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    if (this.DEBUG_EVENTS)  {
      System.out.println("SeqGroupView received groupSelectionChanged() event");
      System.out.println("  group: " + group.getID());
      System.out.println("  seq count: " + group.getSeqs().size());
    }

    if (group == null) {
      genomeL.setText(NO_GENOME);
    } else {
      genomeL.setText(group.getID());
    }
    SeqGroupTableModel mod = new SeqGroupTableModel(group);
    selected_seq = null;
    seqtable.setModel(mod);
    seqtable.validate();
    seqtable.repaint();
  }

  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (this.DEBUG_EVENTS)  { System.out.println("SeqGroupView received seqSelectionChanged() event"); }
    synchronized (seqtable) {  // or should synchronize on lsm?
      // could also get selected seq from SeqSelectionEvent, but should be the same
      if (selected_seq != gmodel.getSelectedSeq()) {
	lsm.removeListSelectionListener(this);
	selected_seq = gmodel.getSelectedSeq();
	if (selected_seq == null) { seqtable.clearSelection(); }
	else  {
	  for (int i=0; i<seqtable.getRowCount(); i++) {
	    // should be able to use == here instead of equals(), because table's model really returns seq.getID()
	    if (selected_seq.getID() ==  seqtable.getValueAt(i, 0)) {
	      //	    lsm.setSelectionInterval(i, i); // equivalent to seqtable.setRowSelectionInterval()?
	      seqtable.setRowSelectionInterval(i, i);
              DisplayUtils.scrollToVisible(seqtable, i, 0);
	      break;
	    }
	  }
	}
	lsm.addListSelectionListener(this);
      }
    }
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
        return Integer.toString(seq.getLength());
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


/**
 *   Stubs out MutableTreeNode methods that aren't used for Das2*Node objects
 */
abstract class DataSourcesAbstractNode implements MutableTreeNode {
  TreeNode parent;
  public void insert(MutableTreeNode child, int index)  {}
  public void remove(int index) {}
  public void remove(MutableTreeNode node) {}
  public void removeFromParent() {}
  public void setParent(MutableTreeNode newParent) {
    this.parent = parent;
  }
  public TreeNode getParent() {
    return parent;
  }
  public void setUserObject(Object object) {}
}
