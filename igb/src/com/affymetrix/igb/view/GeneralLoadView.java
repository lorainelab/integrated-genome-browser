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

import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.general.GenericFeature;
import com.affymetrix.igb.general.GenericVersion;
import com.affymetrix.igb.util.SeqResiduesLoader;
import com.affymetrix.igb.view.GeneralLoadUtils.LoadStatus;
import com.affymetrix.igb.view.GeneralLoadUtils.LoadStrategy;
import java.util.HashSet;
import java.util.Set;

final class GeneralLoadView extends JComponent
				implements ItemListener, ActionListener, GroupSelectionListener, SeqSelectionListener {

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
	private JButton load_visible_dataB;
	private AnnotatedSeqGroup current_group;
	private AnnotatedBioSeq current_seq;
	private SeqMapView gviewer;
	private JTable feature_table;
	private FeaturesTableModel feature_model;
	private boolean first_time_called = true;

	public GeneralLoadView() {
		if (Application.getSingleton() != null) {
			gviewer = Application.getSingleton().getMapView();
		}

		this.glu = new GeneralLoadUtils(gmodel, gviewer);

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

		load_visible_dataB = new JButton("Load Visible Data");
		load_visible_dataB.setEnabled(false);
		load_visible_dataB.addActionListener(this);
		buttonP.add(load_visible_dataB);

		if (IGB.isSequenceAccessible()) {
			all_residuesB = new JButton("Load All Sequence");
			all_residuesB.setEnabled(false);
			all_residuesB.addActionListener(this);
			buttonP.add(all_residuesB);
			partial_residuesB = new JButton("Load Sequence in View");
			partial_residuesB.setEnabled(false);
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
			// Disable the genome_name selectedSpecies.
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
		} else if (src == load_visible_dataB) {
			loadVisibleData();
		}
	}

	/**
	 * Load any data that's marked for visible range.
	 */
	private void loadVisibleData() {
		// Full genome sequence not allowed right now.
		//if (IsGenomeSequence()) {
		//     }

		SeqSpan request_span = gviewer.getVisibleSpan();

		if (DEBUG_EVENTS) {
			System.out.println("Visible load request span: " + request_span.getStart() + " " + request_span.getEnd());
		}
		// Load any features that have a visible strategy and haven't already been loaded.
		String genomeVersionName = (String) versionCB.getSelectedItem();
		for (GenericFeature gFeature : this.glu.getFeatures(genomeVersionName)) {
			if (gFeature.loadStrategy != LoadStrategy.VISIBLE) {
				continue;
			}
			if (!gFeature.LoadStatusMap.containsKey(current_seq)) {
				// Should never get here.
				System.out.println("ERROR!  " + current_seq.getID() + " does not contain feature status");
			}
			/*if (gFeature.LoadStatusMap.get(current_seq) != LoadStatus.UNLOADED) {
			continue;
			}*/
			// Even if it's already loaded, we may want to reload... for example, if the viewsize changes.

			if (DEBUG_EVENTS) {
				System.out.println("Selected : " + gFeature.featureName);
			}
			this.glu.loadAndDisplayAnnotations(gFeature, current_seq, feature_model);
		}
		Application.getSingleton().setStatus("", false);

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
		String selectedSpecies = (String) speciesCB.getSelectedItem();
		refreshVersionCB(selectedSpecies);
	}

	/**
	 * Refresh the genome versions, now that the species has changed.
	 * @param speciesName
	 */
	private void refreshVersionCB(String speciesName) {
		versionCB.removeAllItems();
		versionCB.removeItemListener(this);
		versionCB.addItem(SELECT);
		versionCB.setSelectedIndex(0);

		if (speciesName == null || speciesName.length() == 0 || speciesName.equals(SELECT)) {
			// Disable the version selectedSpecies.
			versionCB.setEnabled(false);
			// refresh the display.
			gmodel.setSelectedSeq(null);
			gmodel.setSelectedSeqGroup(null);
			return;
		}

		// Add version names to combo boxes.
		// Since the same version name may occur on multiple servers, we use sets
		// to eliminate the redundant elements.
		Set<String> versionNames = new HashSet<String>();
		for (GenericVersion gVersion : this.glu.genome2genericVersionList.get(speciesName)) {
			versionNames.add(gVersion.versionName);
		}
		for (String versionName : versionNames) {
			versionCB.addItem(versionName);
		}

		versionCB.setEnabled(true);
		versionCB.addItemListener(this);
	}

	private void versionCBChanged() {
		String version_name = (String) versionCB.getSelectedItem();
		if (DEBUG_EVENTS) {
			System.out.println("Selected version: " + version_name);
		}
		if (version_name.equals(SELECT)) {
			gmodel.setSelectedSeq(null);
			gmodel.setSelectedSeqGroup(null);
		} else {
			AnnotatedSeqGroup group = gmodel.getSeqGroup(version_name);
			if (group == null) {
				System.out.println("Group was null");
				group = gmodel.getSeqGroup(this.glu.versionName2genome.get(version_name));
			}
			if (gmodel.getSelectedSeqGroup() != group) {
				// need to initialize genome before setting it as selected seq group, in
				// case it hasn't been seen before
				// calling gmodel.setSelectedSeqGroup() should also bounce event back to this.groupSelectionChanged()
				gmodel.setSelectedSeq(group == null ? null : group.getSeq(0));	// select first chromosome if possible.
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
		current_group = group;
		String genomeVersionName = null;
		if (current_group == null || this.glu == null || !this.glu.group2version.containsKey(group)) {
			genomeVersionName = null;
		} else {
			genomeVersionName = this.glu.group2version.get(group).versionName;
		}
		if (genomeVersionName == null) {
			if (!SELECT.equals((String) versionCB.getSelectedItem())) {
				System.out.println("ERROR: invalid version selected: " + (String) versionCB.getSelectedItem());
				return;
			}
			// if no genome version in server matches selected AnnotatedSeqGroup,
			// then clear the types_panel and un-select the item
			types_panel.removeAll();
		} else {
			if (this.first_time_called) {
				firstTimeGroupSelectionChanged(genomeVersionName); // No longer the first time called.
			}
			this.glu.initVersion(genomeVersionName);
			versionCB.setSelectedItem(genomeVersionName);
			//createFeaturesTable(genomeVersionName);
		}
		if (DEBUG_EVENTS) {
			System.out.println("groupSelectionChangedInternal to " + genomeVersionName);
		}
	}

	/**
	 * GroupSelectionChanged method is being called for the first time.
	 * This may be a call from the bookmark code, in which case we need to be sure that various boxes are set properly.
	 * @param genomeVersionName
	 */
	private void firstTimeGroupSelectionChanged(String genomeVersionName) {
		String selectedSpecies = (String) speciesCB.getSelectedItem();
		if (selectedSpecies.equals(SELECT)) {
			// Fix combo boxes.  Turn off their listeners temporarily so there are no side effects.
			speciesCB.removeItemListener(this);
			speciesCB.setSelectedItem(this.glu.versionName2genome.get(genomeVersionName));
			if (SELECT.equals((String) versionCB.getSelectedItem())) {
				versionCB.removeItemListener(this);
				versionCB.setSelectedItem(genomeVersionName);
				versionCB.setEnabled(true);
				versionCB.addItemListener(this);
			}
			speciesCB.addItemListener(this);

		// refresh the seq box, which otherwise won't contain the full genome.
		}
		this.first_time_called = false; // No longer the first time called.
	}

	/**
	 * Create the table with the list of features and their status.
	 */
	private void createFeaturesTable(String genomeVersionName) {
		if (DEBUG_EVENTS) {
			System.out.println("Creating new table with chrom " + (current_seq == null ? null : current_seq.getID()));
		}

		List<GenericFeature> features = this.glu.getFeatures(genomeVersionName);
		feature_model = new FeaturesTableModel(this, features, current_seq);

		this.feature_table = new JTable(feature_model);
		this.feature_table.setRowHeight(20);    // TODO: better than the default value of 16, but still not perfect.
		this.feature_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);   // Allow columns to be resized

		// Don't enable combo box for full genome sequence
		TableWithVisibleComboBox.setComboBoxEditor(this.feature_table, 0, FeaturesTableModel.loadChoices, !this.IsGenomeSequence());

		types_panel.removeAll();
		types_panel.add(this.feature_table);
		feature_model.fireTableDataChanged();
		types_panel.invalidate();

		changeVisibleDataButtonIfNecessary(features);	// might have been disabled when switching to another chromosome or genome.
	}

	/**
	 * Changed the selected chromosome.
	 * @param evt
	 */
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		current_seq = evt.getSelectedSeq();

		disableButtonsIfNullOrGenomeSequence();

		String genomeVersionName = (String) this.versionCB.getSelectedItem();
		if (genomeVersionName == null || genomeVersionName.equals(SELECT)) {
			return;
		}

		createFeaturesTable(genomeVersionName);

		// Are there any "whole range" sequences that haven't been loaded?
		// Load any features that have a visible strategy and haven't already been loaded.
		for (GenericFeature gFeature : this.glu.getFeatures(genomeVersionName)) {
			if (gFeature.loadStrategy != LoadStrategy.WHOLE) {
				continue;
			}
			if (!gFeature.LoadStatusMap.containsKey(current_seq)) {
				System.out.println("ERROR!  " + current_seq.getID() + " does not contain feature status");
			}
			if (gFeature.LoadStatusMap.get(current_seq) != LoadStatus.UNLOADED) {
				continue;
			}

			if (DEBUG_EVENTS) {
				System.out.println("Selected : " + gFeature.featureName);
			}
			this.glu.loadAndDisplayAnnotations(gFeature, current_seq, feature_model);
		}

	}

	/**
	 * Don't allow buttons to be used if we're viewing the entire sequence.
	 * (We do this because it's not clear if the user REALLY would want to load all of the specified data.)
	 * (In fact, for the full sequence this would currently be too much memory for the app.)
	 * @param seqID
	 */
	private void disableButtonsIfNullOrGenomeSequence() {
		boolean isGenomeSequence = IsGenomeSequence();
		all_residuesB.setEnabled(!isGenomeSequence);
		partial_residuesB.setEnabled(!isGenomeSequence);
		load_visible_dataB.setEnabled(!isGenomeSequence);
	}

	/**
	 * Accessor method.
	 * See if we need to enable/disable the load_visible_dataB button
	 * by looking at the features' load strategies.
	 */
	void changeVisibleDataButtonIfNecessary(List<GenericFeature> features) {
		if (IsGenomeSequence()) {
			return;
		// Currently not enabling this button for the full sequence.
		}
		boolean enabled = false;
		for (GenericFeature gFeature : features) {
			if (gFeature.loadStrategy == LoadStrategy.VISIBLE) {
				enabled = true;
				break;
			}
		}

		if (load_visible_dataB.isEnabled() != enabled) {
			load_visible_dataB.setEnabled(enabled);
		}
	}

	boolean IsGenomeSequence() {
		// hardwiring names for genome and encode virtual seqs, need to generalize this soon
		final String seqID = current_seq == null ? null : current_seq.getID();
		final String GENOME_SEQ_ID = "genome";
		final String ENCODE_REGIONS_ID = "encode_regions";
		return (seqID == null || ENCODE_REGIONS_ID.equals(seqID) || GENOME_SEQ_ID.equals(seqID));
	}
}

