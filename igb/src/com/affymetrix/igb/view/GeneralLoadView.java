package com.affymetrix.igb.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;

import com.affymetrix.genoviz.util.ErrorHandler;

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.SeqSpan;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.SeqResiduesLoader;
import com.affymetrix.igb.view.GeneralLoadUtils.genericVersion;
import java.util.HashMap;
import java.util.Map;

public final class GeneralLoadView extends JComponent
        implements ItemListener, ActionListener, GroupSelectionListener, SeqSelectionListener {

    GeneralLoadUtils glu;
    static boolean DEBUG_EVENTS = true;
    static public boolean build_virtual_genome = true;
    static public boolean build_virtual_encode = true;
    // hardwiring names for genome and encode virtual seqs, need to generalize this soon
    static public String GENOME_SEQ_ID = "genome";
    static public String ENCODE_REGIONS_ID = "encode_regions";
    static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    static final String SELECT = "Select";
    JComboBox versionCB;
    JComboBox genomeCB;
    JPanel types_panel;
    //QuickLoadServerModel current_server;
    AnnotatedSeqGroup current_group;
    String current_genome_version_name;
    AnnotatedBioSeq current_seq;
    Map cb2filename = new HashMap();
    SeqMapView gviewer;
    JButton all_residuesB;
    JButton partial_residuesB;
    //SeqGroupView group_view;
    //boolean auto_select_first_seq_in_group = true;

    public GeneralLoadView() {
        this.glu = new GeneralLoadUtils(gmodel);

        if (Application.getSingleton() != null) {
            gviewer = Application.getSingleton().getMapView();
        }
        this.setLayout(new BorderLayout());
        types_panel = new JPanel();
        types_panel.setLayout(new BoxLayout(types_panel, BoxLayout.Y_AXIS));

        JPanel choice_panel = new JPanel();
        choice_panel.setLayout(new BoxLayout(choice_panel, BoxLayout.X_AXIS));
        choice_panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

        genomeCB = new JComboBox();
        genomeCB.setEnabled(false);
        choice_panel.add(new JLabel("Genome:"));
        choice_panel.add(Box.createHorizontalStrut(5));
        choice_panel.add(genomeCB);
        choice_panel.add(Box.createHorizontalGlue());

        versionCB = new JComboBox();
        versionCB.setEnabled(false);
        choice_panel.add(new JLabel("Version:"));
        choice_panel.add(Box.createHorizontalStrut(5));
        choice_panel.add(versionCB);
        choice_panel.add(Box.createHorizontalStrut(20));


        JPanel buttonP = new JPanel();
        buttonP.setLayout(new GridLayout(1, 3));
        if (IGB.isSequenceAccessible()) {
            all_residuesB = new JButton("Load All Sequence");
            all_residuesB.addActionListener(this);
            buttonP.add(all_residuesB);
            partial_residuesB = new JButton("Load Sequence in View");
            if (IGB.ALLOW_PARTIAL_SEQ_LOADING) {
                partial_residuesB.addActionListener(this);
                buttonP.add(partial_residuesB);
            }
        } else {
            buttonP.add(Box.createRigidArea(new Dimension(5, 0)));
            buttonP.add(new JLabel("No sequence available", JLabel.CENTER));
        }

        this.add("North", choice_panel);
        this.add("Center", new JScrollPane(types_panel));
        this.add("South", buttonP);

        this.setBorder(BorderFactory.createEtchedBorder());

        initializeGenomeCB();

        gmodel.addGroupSelectionListener(this);
        gmodel.addSeqSelectionListener(this);

        versionCB.addItemListener(this);
        genomeCB.addItemListener(this);
    }

    private void initializeGenomeCB() {
        genomeCB.removeAllItems();
        genomeCB.addItem(SELECT);
        this.glu.discoverServersAndGenomesAndVersions();

        if (this.glu.genome_names.size() == 0) {
            // Disable the genome_name selectedGenome.
            genomeCB.setEnabled(false);
            return;
        }

        for (String genome_name : this.glu.genome_names) {
            genomeCB.addItem(genome_name);
        }

        genomeCB.addItemListener(this);
        genomeCB.setEnabled(true);
        genomeCB.setSelectedIndex(0);
    }

    private void refreshVersionCB(String genomeName) {
        versionCB.removeAllItems();
        versionCB.addItem(SELECT);

        if (genomeName == null || genomeName.length() == 0 || genomeName.equals(SELECT)) {
            // Disable the version selectedGenome.
            versionCB.setEnabled(false);
            return;
        }

        for (genericVersion gVersion : this.glu.genome2genericVersionList.get(genomeName)) {
            versionCB.addItem(gVersion.versionName);
        }

        versionCB.addItemListener(this);
        versionCB.setEnabled(true);
        versionCB.setSelectedIndex(0);
    }

    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        /* handles residues loading based on partial or full sequence load buttons */
        if (src == partial_residuesB) {
            SeqSpan viewspan = gviewer.getVisibleSpan();
            if (current_group == null) {
                ErrorHandler.errorPanel("Error", "No sequence group selected.", gviewer);
            } else if (current_seq == null) {
                ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer);
            } else if (viewspan.getBioSeq() != current_seq) {
                System.err.println("Error in GeneralLoadView: " +
                        "SeqMapView seq and GeneralLoadView current_seq not the same!");
            } else {
                SeqResiduesLoader.loadPartialResidues(viewspan, current_group);
            }
        } else if (src == all_residuesB) {
            if (current_group == null) {
                ErrorHandler.errorPanel("Error", "No sequence group selected.", gviewer);
            } else if (current_seq == null) {
                ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer);
            }
            if (!(current_seq instanceof SmartAnnotBioSeq)) {
                ErrorHandler.errorPanel("Error", "Can't do optimized full residues retrieval for this sequence.", gviewer);
            } else {
                SeqResiduesLoader.loadAllResidues((SmartAnnotBioSeq) current_seq);
            }
        } else if (src instanceof JCheckBox) {  // must put this after cache modification branch, since some of those are JCheckBoxes
            if (DEBUG_EVENTS) {
                System.out.println("GeneralLoadView received annotation load action");
            }
            JCheckBox cbox = (JCheckBox) src;
            String filename = (String) cb2filename.get(cbox);
            boolean selected = cbox.isSelected();
            // probably need to make this threaded (see QuickLoaderView)
            if (selected) {
                System.out.println("Selected : " + filename);
                this.glu.loadAnnotations(gviewer, current_group, filename);
                gviewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true, false);
                cbox.setText(filename);
            } else {
                // never happens.  We don't allow the checkbox to be un-selected
            }
        }
    }

    public void itemStateChanged(ItemEvent evt) {
        Object src = evt.getSource();
        if (DEBUG_EVENTS) {
            System.out.println("####### GeneralLoadView received itemStateChanged event: " + evt);
        }

        try {

            if ((src == genomeCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
                String selectedGenome = (String) genomeCB.getSelectedItem();

                refreshVersionCB(selectedGenome);

                //auto_select_first_seq_in_group = false;
                // Don't let the group selectedGenome trigger an automatic seq selectedGenome
                // because that could happen during start-up and would always force the brief display
                // of chr1 (or whatever chr is first in the group) before going to the old_group_id.
                gmodel.setSelectedSeqGroup(null); // causes a GroupSelectionEvent

                types_panel.invalidate(); // make sure display gets updated
                //auto_select_first_seq_in_group = true;
            } else if ((src == versionCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
                String version_name = (String) versionCB.getSelectedItem();
                if (DEBUG_EVENTS) {
                    System.out.println("Selected version: " + version_name);
                }

                if (version_name.equals(SELECT)) {
                    current_genome_version_name = version_name;
                    gmodel.setSelectedSeq(null);
                    gmodel.setSelectedSeqGroup(null);
                } else {
                    AnnotatedSeqGroup group = gmodel.getSeqGroup(version_name);
                    if (group == null) {
                        group = gmodel.getSeqGroup(this.glu.version2genome.get(version_name));
                    }
                    if (gmodel.getSelectedSeqGroup() != group) {
                        // need to initialize genome before setting it as selected seq group, in
                        //    case it hasn't been seen before
                        current_genome_version_name = version_name;

                        this.glu.initVersion(version_name);
                        // calling gmodel.setSelectedSeqGroup() should also bounce event back to this.groupSelectionChanged()
                        gmodel.setSelectedSeq(null);
                        gmodel.setSelectedSeqGroup(group);
                    }
                }
            }

        } catch (Throwable t) {
            // some out-of-memory errors could happen during this code, so
            // this catch block will report that to the user.
            ErrorHandler.errorPanel("Error ", t);
        }
    }

    public void groupSelectionChanged(GroupSelectionEvent evt) {
        // Implementation of GroupSelectionListener
        //  This gets called when something external, such as a bookmark, causes
        //  the genome to change.  Internally, when the genome combo box is changed,
        //  that causes a call to SingletonGenomeModel.setSelectedSeqGroup(), and that
        //  causes a call to here.

        AnnotatedSeqGroup group = evt.getSelectedGroup();
        if (DEBUG_EVENTS) {
            System.out.println("GeneralLoadView.groupSelectionChanged() called, group: " + (group == null ? null : group.getID()));
        }
        if (current_group != group) {
            cb2filename.clear();

            types_panel.removeAll();

            current_group = group;

            if (current_group == null || this.glu == null) {
                current_genome_version_name = null;
            } else {
                current_genome_version_name = this.glu.getGenomeName(group);
            }

            if (current_genome_version_name == null) {
                // if no genome in quickload server matches selected AnnotatedSeqGroup,
                // then clear the types_panel and un-select the item in the genomeCB
                types_panel.add(new JLabel("The selected genome is not included in this QuickLoad server."));
                genomeCB.setSelectedIndex(-1);
            } else {
                this.glu.initVersion(current_genome_version_name);
                genomeCB.setSelectedItem(current_genome_version_name);

                List<String> featureNames = this.glu.getFeatures(current_genome_version_name);
                for (String featureName : featureNames) {
                    if (featureName == null || featureName.length() == 0) {
                        continue;
                    }

                    JCheckBox cb = new JCheckBox(featureName);
                    cb2filename.put(cb, featureName);
                    cb.addActionListener(this);
                    types_panel.add(cb);
                }
            }
        }
        types_panel.invalidate(); // make sure display gets updated (even if this is the same group as before.)
        types_panel.repaint();
    }

    public void seqSelectionChanged(SeqSelectionEvent evt) {
        if (DEBUG_EVENTS) {
            System.out.println("GeneralLoadView.seqSelectionChanged() called");
        }
        current_seq = evt.getSelectedSeq();

        if (current_seq != null && !ENCODE_REGIONS_ID.equals(current_seq.getID()) && !GENOME_SEQ_ID.equals(current_seq.getID())) {
            all_residuesB.setEnabled(true);
            partial_residuesB.setEnabled(true);
        } else {
            all_residuesB.setEnabled(false);
            partial_residuesB.setEnabled(false);
        }
    }
    /*void refreshGenomeChoices() {
    genomeCB.removeItemListener(this);
    genomeCB.removeAllItems();
    genomeCB.addItem(SELECT);

    if (this.glu != null) {
    Iterator genome_names = this.glu.getGenomeNames().iterator();
    while (genome_names.hasNext()) {
    String genome_name = (String) genome_names.next();
    //AnnotatedSeqGroup group = current_server.getSeqGroup(genome_name);
    genomeCB.addItem(genome_name);
    }
    }
    genomeCB.setSelectedIndex(-1); // deselect everything, so later selectedGenome will send event
    genomeCB.addItemListener(this);
    genomeCB.setSelectedItem(SELECT);
    }*/
}

