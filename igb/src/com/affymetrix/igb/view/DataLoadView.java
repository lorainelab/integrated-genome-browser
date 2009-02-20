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
import javax.swing.*;
import javax.swing.event.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.swing.DisplayUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;

public class DataLoadView extends JComponent  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  GeneralLoadView general_load_view;
  SeqGroupView group_view;

  public DataLoadView() {
    // some of the options in DataLoadPrefsView are specific to QuickLoad, but still want to be able to see 
    //     DataLoadPrefsView even when not using QuickLoad
    PreferencesPanel pp = PreferencesPanel.getSingleton();
    pp.addPrefEditorComponent(new DataLoadPrefsView());

    this.setLayout(new BorderLayout());

     JPanel main_panel = new JPanel();
    this.add(main_panel);
    this.setBorder(BorderFactory.createEtchedBorder());

    main_panel.setLayout(new BorderLayout());

    general_load_view = new GeneralLoadView();
    main_panel.add("Center", general_load_view);

    group_view = new SeqGroupView();
    main_panel.add("West",group_view);
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
  //JLabel genomeL;
  //JComboBox genomeCB;

  public SeqGroupView() {
    seqtable = new JTable();
    seqtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
   
    JScrollPane scroller = new JScrollPane(seqtable);
    scroller.setBorder(BorderFactory.createCompoundBorder(
      scroller.getBorder(),
      BorderFactory.createEmptyBorder(0,2,0,2)));

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    //this.add(genomeCB);
    this.add(Box.createRigidArea(new Dimension(0, 5)));
    this.add(scroller);

    this.setBorder(BorderFactory.createTitledBorder("Current Sequence"));
    gmodel.addGroupSelectionListener(this);
    gmodel.addSeqSelectionListener(this);
    lsm = seqtable.getSelectionModel();
    lsm.addListSelectionListener(this);
  }

  String most_recent_seq_id = null;

  public void groupSelectionChanged(GroupSelectionEvent evt) {
    //    AnnotatedSeqGroup group = (AnnotatedSeqGroup)evt.getSelectedGroups().get(0);
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    if (SeqGroupView.DEBUG_EVENTS)  {
      System.out.println("SeqGroupView received groupSelectionChanged() event");
      if (group == null)  { System.out.println("  group is null"); }
      else  {
        System.out.println("  group: " + group.getID());
        System.out.println("  seq count: " + group.getSeqCount());
      }
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

  public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (SeqGroupView.DEBUG_EVENTS) {
			System.out.println("SeqGroupView received seqSelectionChanged() event");
		}
		synchronized (seqtable) {  // or should synchronize on lsm?
			// if (selected_seq != evt.getSelectedSeq()) {
			lsm.removeListSelectionListener(this);
			//selected_seq = gmodel.getSelectedSeq();
			selected_seq = evt.getSelectedSeq();
			if (selected_seq == null) {
				seqtable.clearSelection();
			} else {
				most_recent_seq_id = selected_seq.getID();

				for (int i = 0; i < seqtable.getRowCount(); i++) {
					// should be able to use == here instead of equals(), because table's model really returns seq.getID()
					if (most_recent_seq_id == seqtable.getValueAt(i, 0)) {
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
      if (SeqGroupView.DEBUG_EVENTS)  { System.out.println("SeqGroupView received valueChanged() ListSelectionEvent"); }
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

  @Override
  public Dimension getMinimumSize() { return new Dimension(200, 50); }
    @Override
  public Dimension getPreferredSize() { return new Dimension(200, 50); }

}



