package com.affymetrix.igb.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.event.*;

public class SeqComboBoxView extends JComponent
  implements ItemListener, ActionListener,
	     GroupSelectionListener, SeqSelectionListener {

  static boolean DEBUG_EVENTS = false;
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  AnnotatedBioSeq selected_seq = null;
  JLabel seqL;
  JComboBox seqCB;
  JButton genomeB;
  static final String SELECT_A_SEQUENC = "Select a seq";
  static final String NO_SEQUENCES = "No seqs to select";
  static final String NO_SEQ_SELECTED = "No seq selected";

  public SeqComboBoxView() {
    // need to set maximum x size of seqL and seqCB (or entire SeqComboBoxView), so doesn't grab space needed by other parts of GUI
    JLabel genomeL = new JLabel("Genome: ");
    JComboBox genomeCB = new JComboBox();
    genomeB = new JButton("Pick Genome");
    genomeCB.addItem("unknown");
    seqL = new JLabel("Sequence: ", SwingConstants.RIGHT);
    seqCB = new JComboBox();
    seqCB.addItem(NO_SEQUENCES);
    this.setLayout(new GridLayout(1, 3));
    //    this.add(genomeL);
    //    this.add(genomeCB);
    this.add(genomeB);
    this.add(seqL);
    this.add(seqCB);
      // this.setLayout(new BorderLayout());
    //    this.add("West", seqL);
    //    this.add("Center", seqCB);
    seqCB.addItemListener(this);
    gmodel.addGroupSelectionListener(this);
    gmodel.addSeqSelectionListener(this);
    genomeB.addActionListener(this);
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == genomeB) {
      IGB.errorPanel("Genome Chooser is not yet implemented");
      /*
      JTree genome_tree = new JTree();
      JButton okayB = new JButton("OK");
      JButton cancelB = new JButton("Cancel");
      JPanel pan1 = new JPanel(new GridLayout(1, 2));
      pan1.add(okayB);
      pan1.add(cancelB);
      JPanel ok_cancel_panel = new JPanel();
      ok_cancel_panel.add(pan1);

      final JOptionPane opt_pane = new JOptionPane(
						   test_tree,
						   JOptionPane.PLAIN_MESSAGE,
						   JOptionPane.OK_CANCEL_OPTION
						   );
      final JDialog dialog = new JDialog(IGB.getSingleton().getFrame(), "Genome Chooser", true);

      //      dialog.setContentPane(opt_pane);
      dialog.getContentPane().add("Center", test_tree);
      dialog.getContentPane().add("South", ok_cancel_panel);
      
      dialog.setSize(new Dimension(300, 600));
      //      dialog.pack();
      System.out.println("***** in showDasDialog(), showing dialog");
      dialog.show();
      //   dialog.setVisible(true);
      */
    }
  }


  public void groupSelectionChanged(GroupSelectionEvent evt) {
    //    AnnotatedSeqGroup group = (AnnotatedSeqGroup)evt.getSelectedGroups().get(0);
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    if (this.DEBUG_EVENTS)  {
      System.out.println("SeqComboBoxView received groupSelectionChanged() event");
      if (group == null) {
        System.out.println("  group: " + null);
      } else {
        System.out.println("  group: " + group.getID());
        System.out.println("  seq count: " + group.getSeqCount());
      }
    }
    selected_seq = null;
    seqCB.removeAllItems();
    if (group == null || group.getSeqCount() == 0) {
      seqCB.addItem(NO_SEQUENCES);
    }
    else {
      seqCB.addItem(NO_SEQ_SELECTED);
      Iterator iter = group.getSeqList().iterator();
      while (iter.hasNext()) {
        AnnotatedBioSeq aseq = (AnnotatedBioSeq)iter.next();
        String seqid = aseq.getID();
        seqCB.addItem(seqid);
      }
    }
  }

  public void seqSelectionChanged(SeqSelectionEvent evt) {
    AnnotatedBioSeq aseq = evt.getSelectedSeq();

    if (this.DEBUG_EVENTS)  {
      System.out.println("SeqComboBoxView received seqSelectionChanged() event: " + (aseq == null ? "null" : aseq.getID()));
    }

    if (aseq == null)  {
      if (seqCB.getSelectedItem() != NO_SEQ_SELECTED) {
        seqCB.setSelectedItem(NO_SEQ_SELECTED);
        // It is possible that the NO_SEQ_SELECTED item doesn't exist, because
        // the NO_SEQUENCES item is there instead.
        // This takes care of that case, though it isn't important:
        if (seqCB.getSelectedItem() != NO_SEQ_SELECTED) {
          seqCB.setSelectedItem(NO_SEQUENCES);
        }
      }
    }
    else  {
      String seqid = aseq.getID();
      seqCB.setSelectedItem(seqid);
    }
  }


  public void itemStateChanged(ItemEvent evt) {
    Object src = evt.getSource();
    if (DEBUG_EVENTS)  { System.out.println("SeqComboBoxView received itemStateChanged event: " + evt); }
    if ((src == seqCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      String seqid = (String)evt.getItem();
      if (seqid == NO_SEQ_SELECTED)   {
	if (gmodel.getSelectedSeq() != null) {
	  gmodel.setSelectedSeq(null);
	}
      }
      else {
	AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
	MutableAnnotatedBioSeq aseq = null;
        if (group != null) { aseq = group.getSeq(seqid); }
	if (gmodel.getSelectedSeq() != aseq) {  // to catch bounces from seqSelectionChanged() setting of selected item
	  gmodel.setSelectedSeq(aseq);
	}
      }
    }
    //    else if (src == genomeB) {
    //      System.out.println("&&&&&&&& SeqComboBoxView genome selection button pressed");
    //    }
  }

  

}
