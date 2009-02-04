package com.affymetrix.igb.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import javax.swing.*;

import com.affymetrix.genoviz.util.ErrorHandler;

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.SeqSpan;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.event.DataRequestEvent;
import com.affymetrix.genometryImpl.event.DataRequestListener;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.SeqResiduesLoader;
import com.affymetrix.igb.view.GeneralLoadUtils.genericVersion;

public final class GeneralLoadView extends JComponent
        implements ItemListener, ActionListener, GroupSelectionListener, SeqSelectionListener, DataRequestListener {

    GeneralLoadUtils glu;
    private static boolean DEBUG_EVENTS = true;
    static boolean BUILD_VIRTUAL_GENOME = true;
    static boolean BUILD_VIRTUAL_ENCODE = true;
    private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    private static final String SELECT = "Select";
    private JComboBox versionCB;
    private JComboBox speciesCB;
    private JPanel types_panel;
    private JButton all_residuesB;
    private JButton partial_residuesB;

    //QuickLoadServerModel current_server;
    private AnnotatedSeqGroup current_group;
    private String current_genome_version_name;
    private AnnotatedBioSeq current_seq;
    private final Map cb2filename = new HashMap();
    private SeqMapView gviewer;

    //SeqGroupView group_view;
    //boolean auto_select_first_seq_in_group = true;
    public GeneralLoadView() {
        this.glu = new GeneralLoadUtils(gmodel);

        if (Application.getSingleton() != null) {
            gviewer = Application.getSingleton().getMapView();
            gviewer.addDataRequestListener(this);
        }
        this.setLayout(new BorderLayout());
        types_panel = new JPanel();
        types_panel.setLayout(new BoxLayout(types_panel, BoxLayout.Y_AXIS));

        JPanel choice_panel = new JPanel();
        choice_panel.setLayout(new BoxLayout(choice_panel, BoxLayout.X_AXIS));
        choice_panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

        speciesCB = new JComboBox();
        speciesCB.setEnabled(false);
        choice_panel.add(new JLabel("Species:"));
        choice_panel.add(Box.createHorizontalStrut(5));
        choice_panel.add(speciesCB);
        choice_panel.add(Box.createHorizontalGlue());

        versionCB = new JComboBox();
        versionCB.setEnabled(false);
        choice_panel.add(new JLabel("Genome Version:"));
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

        initializeSpeciesCB();

        gmodel.addGroupSelectionListener(this);
        gmodel.addSeqSelectionListener(this);

        versionCB.addItemListener(this);
        speciesCB.addItemListener(this);
    }

    private void initializeSpeciesCB() {
        speciesCB.removeAllItems();
        speciesCB.addItem(SELECT);
        this.glu.discoverServersAndGenomesAndVersions();

        if (this.glu.genome_names.size() == 0) {
            // Disable the genome_name selectedGenome.
            speciesCB.setEnabled(false);
            return;
        }

        for (String genome_name : this.glu.genome_names) {
            speciesCB.addItem(genome_name);
        }

        speciesCB.addItemListener(this);
        speciesCB.setEnabled(true);
        speciesCB.setSelectedIndex(0);
    }

    /**
     * Refresh the genome versions, now that the species has changed.
     * @param speciesName
     */
    private void refreshVersionCB(String speciesName) {
        versionCB.removeAllItems();
        versionCB.addItem(SELECT);

        if (speciesName == null || speciesName.length() == 0 || speciesName.equals(SELECT)) {
            // Disable the version selectedGenome.
            versionCB.setEnabled(false);
            return;
        }

        for (genericVersion gVersion : this.glu.genome2genericVersionList.get(speciesName)) {
            versionCB.addItem(gVersion.versionName);
        }

        versionCB.addItemListener(this);
        versionCB.setEnabled(true);
        versionCB.setSelectedIndex(0);
    }

    /**
     * "Refresh Data" was pressed.
     * @param evt
     * @return
     */
    public boolean dataRequested(DataRequestEvent evt) {
		System.out.println("GeneralLoadView received DataRequestEvent: " + evt);
		//loadFeaturesInView();
		return false;
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
                System.out.println("Selected : " + filename + " with annotated seq : " + gmodel.getSelectedSeq() + "with group " + current_group);
                this.glu.loadAndDisplayAnnotations(gviewer, current_group, filename);
               
				Application.getSingleton().setStatus("", false);
                //cbox.setText(filename);
            } else {
                // never happens.  We don't allow the checkbox to be un-selected
            }
        }
    }

    /**
     * One of the combo boxes changed state.
     * @param evt
     */
    public void itemStateChanged(ItemEvent evt) {
        Object src = evt.getSource();
        if (DEBUG_EVENTS) {
            System.out.println("####### GeneralLoadView received itemStateChanged event: " + evt);
        }

        try {
            if ((src == speciesCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
                speciesCBChanged(); // make sure display gets updated
            //auto_select_first_seq_in_group = true;
            } else if ((src == versionCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
                versionCBChanged();
            }
        } catch (Throwable t) {
            // some out-of-memory errors could happen during this code, so
            // this catch block will report that to the user.
            ErrorHandler.errorPanel("Error ", t);
        }
    }

    private void speciesCBChanged() {
        String selectedGenome = (String) speciesCB.getSelectedItem();
        refreshVersionCB(selectedGenome);
        //auto_select_first_seq_in_group = false;
        // Don't let the group selectedGenome trigger an automatic seq selectedGenome
        // because that could happen during start-up and would always force the brief display
        // of chr1 (or whatever chr is first in the group) before going to the old_group_id.
        gmodel.setSelectedSeqGroup(null); // causes a GroupSelectionEvent
        types_panel.invalidate(); // make sure display gets updated
    //auto_select_first_seq_in_group = true;
    }

    private void versionCBChanged() {
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
                group = gmodel.getSeqGroup(this.glu.versionName2genome.get(version_name));
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

    /**
     * This gets called when something external, such as a bookmark, causes
     * the genome version to change.
     * Also, when the genome version combo box is changed,
     * that calls SingletonGenomeModel.setSelectedSeqGroup(), and calls here.
     * @param evt
     */
    public void groupSelectionChanged(GroupSelectionEvent evt) {
        AnnotatedSeqGroup group = evt.getSelectedGroup();
        if (DEBUG_EVENTS) {
            System.out.println("GeneralLoadView.groupSelectionChanged() called, group: " + (group == null ? null : group.getID()));
        }
        if (current_group != group) {
            groupSelectionChangedInternal(group);
        }
        types_panel.invalidate(); // make sure display gets updated (even if this is the same group as before.)
        types_panel.repaint();
    }

    /**
     * The genome version actually changed, rather than just someone choosing the same version.
     * @param group
     */
    private void groupSelectionChangedInternal(AnnotatedSeqGroup group) {
        if (DEBUG_EVENTS) {
            System.out.println("groupSelectionChangedInternal");
        }
        cb2filename.clear();
        types_panel.removeAll();
        current_group = group;
        if (current_group == null || this.glu == null || !this.glu.group2version.containsKey(group)) {
            current_genome_version_name = null;
        } else {
            current_genome_version_name = this.glu.group2version.get(group).versionName;
        }
        if (current_genome_version_name == null) {
            // if no genome in quickload server matches selected AnnotatedSeqGroup,
            // then clear the types_panel and un-select the item in the speciesCB
            types_panel.add(new JLabel("The selected genome is not included in this QuickLoad server."));
            speciesCB.setSelectedIndex(-1);
        } else {
            this.glu.initVersion(current_genome_version_name);
            speciesCB.setSelectedItem(current_genome_version_name);
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

    /**
     * Changed the selected chromosome.
     * @param evt
     */
    public void seqSelectionChanged(SeqSelectionEvent evt) {
        current_seq = evt.getSelectedSeq();
        String seqID = current_seq == null ? null : current_seq.getID();
        if (DEBUG_EVENTS) {
            System.out.println("GeneralLoadView.seqSelectionChanged() called with " + seqID);
        }

        // hardwiring names for genome and encode virtual seqs, need to generalize this soon
        final String GENOME_SEQ_ID = "genome";
        final String ENCODE_REGIONS_ID = "encode_regions";

        boolean disableResidues = seqID == null || ENCODE_REGIONS_ID.equals(seqID) || GENOME_SEQ_ID.equals(seqID);
        all_residuesB.setEnabled(!disableResidues);
        partial_residuesB.setEnabled(!disableResidues);
    }
}

