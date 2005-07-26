package com.affymetrix.igb.view;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.genometry.*;

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
    tpane.addTab("DAS/2", das2_view);
    tpane.addTab("QuickLoad", quick_view);
    tpane.addTab("DAS/1", das1_view);
    this.add("West", group_view);
  }

}

class SeqGroupView extends JComponent
    implements GroupSelectionListener, ListSelectionListener  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  JTable seqtable;
  AnnotatedBioSeq selected_seq = null;
  ListSelectionModel lsm;
  JLabel genomeL;

  public SeqGroupView() {
    seqtable = new JTable();
    genomeL = new JLabel("No Genome Selected");
    genomeL.setFont(genomeL.getFont().deriveFont(Font.BOLD));
    seqtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.setLayout(new BorderLayout());
    this.add("Center", new JScrollPane(seqtable));
    this.add("North", genomeL);
    this.setBorder(new TitledBorder("Current Genome"));
    gmodel.addGroupSelectionListener(this);
    lsm = seqtable.getSelectionModel();
    lsm.addListSelectionListener(this);
  }

  public void groupSelectionChanged(GroupSelectionEvent evt) {
    //    AnnotatedSeqGroup group = (AnnotatedSeqGroup)evt.getSelectedGroups().get(0);
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    genomeL.setText(group.getID());
    SeqGroupTableModel mod = new SeqGroupTableModel(group);
    selected_seq = null;
    seqtable.setModel(mod);
    seqtable.validate();
    seqtable.repaint();
  }

  public void valueChanged(ListSelectionEvent evt) {
    Object src = evt.getSource();
    if ((src == lsm) && (! evt.getValueIsAdjusting())) { // ignore extra messages
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
  public int getRowCount() { return group.getSeqs().size(); }
  public int getColumnCount() { return 2; }
  public Object getValueAt(int row, int col) {
    if (col == 0) {
      return group.getSeq(row).getID();
    }
    else if (col == 1) {
      return Integer.toString(group.getSeq(row).getLength());
    }
    return null;
  }

  public String getColumnName(int col) {
    if (col == 0) { return "Sequence"; }
    else if (col == 1) { return "Length"; }
    else { return null; }
  }


}
