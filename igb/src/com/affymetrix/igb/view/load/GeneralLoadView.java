package com.affymetrix.igb.view.load;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometry.SeqSpan;

import com.affymetrix.genometry.util.LoadUtils.LoadStatus;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.general.Persistence;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.SeqMapView;

import javax.swing.JSplitPane;
import javax.swing.table.TableColumn;
import org.jdesktop.swingworker.SwingWorker;

public final class GeneralLoadView extends JComponent
				implements ItemListener, ActionListener, GroupSelectionListener, SeqSelectionListener {

	GeneralLoadUtils glu;
	private static final boolean DEBUG_EVENTS = false;
	private static final SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private static final String SELECT_SPECIES = "Species";
	private static final String SELECT_GENOME = "Genome Version";
	private static final String GENOME_SEQ_ID = "genome";
	private static final String ENCODE_REGIONS_ID = "encode_regions";
	private AnnotatedSeqGroup curGroup = null;
	private SmartAnnotBioSeq curSeq = null;
	private JComboBox kingdomCB;
	private final JComboBox versionCB;
	private final JComboBox speciesCB;
	//private JPanel feature_panel;
	private JButton all_residuesB;
	private JButton partial_residuesB;
	private final JButton refresh_dataB;
	private SeqMapView gviewer;
	private JTableX feature_table;
	private FeaturesTableModel feature_model;
	JScrollPane featuresTableScrollPane;
	private FeatureTreeView feature_tree_view;

	public GeneralLoadView() {
		if (Application.getSingleton() != null) {
			gviewer = Application.getSingleton().getMapView();
		}

		this.glu = new GeneralLoadUtils();
	
		this.setLayout(new BorderLayout());

		JPanel choicePanel = new JPanel();
		choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.X_AXIS));
		choicePanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

		/*kingdomCB = new JComboBox();
		kingdomCB.setEnabled(false);
		kingdomCB.setEditable(false);
		choicePanel.add(new JLabel("Kingdom:"));
		choicePanel.add(Box.createHorizontalStrut(5));
		choicePanel.add(kingdomCB);
		choicePanel.add(Box.createHorizontalGlue());*/

		speciesCB = new JComboBox();
						speciesCB.addItem(SELECT_SPECIES);
		speciesCB.setMaximumSize(new Dimension(speciesCB.getPreferredSize().width*4,speciesCB.getPreferredSize().height));

		speciesCB.setEnabled(false);
		speciesCB.setEditable(false);
		choicePanel.add(new JLabel("Choose:"));
		choicePanel.add(Box.createHorizontalStrut(5));
		choicePanel.add(speciesCB);
		//choicePanel.add(Box.createHorizontalGlue());

		choicePanel.add(Box.createHorizontalStrut(50));
		versionCB = new JComboBox();
		versionCB.addItem(SELECT_GENOME);
		versionCB.setMaximumSize(new Dimension(versionCB.getPreferredSize().width*4, versionCB.getPreferredSize().height));
		versionCB.setEnabled(false);
		versionCB.setEditable(false);
		//choicePanel.add(new JLabel("Genome Version:"));
		//choicePanel.add(Box.createHorizontalStrut(5));
		choicePanel.add(versionCB);
		//choicePanel.add(Box.createHorizontalStrut(20));


		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 3));

		all_residuesB = new JButton("Load All Sequence");
		all_residuesB.setMaximumSize(all_residuesB.getPreferredSize());
		all_residuesB.setEnabled(false);
		all_residuesB.addActionListener(this);
		buttonPanel.add(all_residuesB);
		partial_residuesB = new JButton("Load Sequence in View");
		partial_residuesB.setMaximumSize(partial_residuesB.getPreferredSize());
		partial_residuesB.setEnabled(false);
		//if (IGB.ALLOW_PARTIAL_SEQ_LOADING) {
		
		partial_residuesB.addActionListener(this);
		buttonPanel.add(partial_residuesB);
		//}
		refresh_dataB = new JButton("Refresh Data");
		refresh_dataB.setMaximumSize(refresh_dataB.getPreferredSize());
		refresh_dataB.setEnabled(false);
		refresh_dataB.addActionListener(this);
		buttonPanel.add(refresh_dataB);

		this.feature_model = new FeaturesTableModel(this, null, null);
		this.feature_table = new JTableX(this.feature_model);
		this.feature_table.setModel(this.feature_model);

		this.feature_tree_view = new FeatureTreeView(this);
		featuresTableScrollPane = new JScrollPane(this.feature_table);

		JPanel featuresPanel = new JPanel();
		featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
		featuresPanel.add(new JLabel("Choose Load Mode for Data Sets:"));
		featuresPanel.add(featuresTableScrollPane);

		this.add("North", choicePanel);

		JSplitPane jPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.feature_tree_view, featuresPanel);
		jPane.setResizeWeight(0.5);		
		
		this.add("Center", jPane);
		this.add("South", buttonPanel);

		this.setBorder(BorderFactory.createEtchedBorder());

		
		populateSpeciesData();

	}

	/**
	 * Discover servers, species, etc., asynchronously.
	 */
	private void populateSpeciesData() {
		Application.getSingleton().setNotLockedUpStatus("Loading servers...");

		Executor vexec = Executors.newSingleThreadExecutor();

		SwingWorker worker = new SwingWorker() {

			protected Object doInBackground() throws Exception {
				discoverServersAndSpeciesAndVersions();
				return null;
			}

			@Override
			public void done() {
				initializeKingdomCB();
				initializeSpeciesCB();
				Application.getSingleton().setNotLockedUpStatus("Loading previous genome...");
				RestorePersistentGenome();
				Application.getSingleton().setStatus("",false);
				addListeners();
			}
		};

		vexec.execute(worker);
	}

	private void discoverServersAndSpeciesAndVersions() {
		this.glu.discoverServersAndSpeciesAndVersions();
	}

	/**
	 * Add and verify another server.  Called from DataLoadPrefsView.
	 * @param serverName
	 * @param serverURL
	 * @param serverType
	 */
	public boolean addServer(String serverName, String serverURL, String serverType) {
		boolean successful = false;
		if (serverType.equals("QuickLoad")) {
			successful = this.glu.addServer(serverName, serverURL, ServerType.QuickLoad);
		} else if (serverType.equals("DAS")) {
			successful = this.glu.addServer(serverName, serverURL, ServerType.DAS);
		} else if (serverType.equals("DAS2")) {
			successful = this.glu.addServer(serverName, serverURL, ServerType.DAS2);
		}
		if (!successful) {
			return false;
		}

		// server has been added.  Refresh necessary boxes, tables, etc.
		initializeSpeciesCB();
		clearFeaturesTable();
		speciesCB.addItemListener(this);
		speciesCB.setSelectedItem(SELECT_SPECIES);
		gmodel.setSelectedSeqGroup(null);

		return true;
	}


	private void addListeners() {
		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);

		speciesCB.setEnabled(true);
		versionCB.setEnabled(true);
		speciesCB.addItemListener(this);
		versionCB.addItemListener(this);

	}

	private void initializeKingdomCB() {
		// TODO
	}

	/**
	 * Initialize Species combo box.  It is assumed that we have the species data at this point.
	 * If a species was already selected, leave it as the selected species.
	 */
	private void initializeSpeciesCB() {
		String oldSpecies = (String)speciesCB.getSelectedItem();
		speciesCB.removeItemListener(this);
		speciesCB.removeAllItems();
		speciesCB.addItem(SELECT_SPECIES);

		int speciesListLength = this.glu.species2genericVersionList.keySet().size();
		if (speciesListLength == 0) {
			// Disable the speciesName selectedSpecies.
			speciesCB.setEnabled(false);
			return;
		}

		// Sort the species before presenting them
		SortedSet<String> speciesList = new TreeSet<String>();
		speciesList.addAll(this.glu.species2genericVersionList.keySet());
		for (String speciesName : speciesList) {
			speciesCB.addItem(speciesName);
		}

		if (oldSpecies != null && speciesList.contains(oldSpecies)) {
			speciesCB.setSelectedItem(oldSpecies);
		}
	}

	/**
	 * bootstrap bookmark from Preferences for last species/versionName/genome / sequence / region
	 * @param gviewer
	 * @return
	 */
	private void RestorePersistentGenome() {
		// Get group and seq info from persistent preferences.
		// (Recovering as much data as possible before activating listeners.)
		AnnotatedSeqGroup group = Persistence.restoreGroupSelection();
		if (group == null) {
			return;
		}

		List<GenericVersion> gVersions = group.getVersions();
		if (gVersions.isEmpty()) {
			return;
		}
		String versionName = gVersions.get(0).versionName;
		if (versionName == null || this.glu.versionName2species.get(versionName) == null) {
			return;
		}

		gmodel.addGroupSelectionListener(this);
		if (group != gmodel.getSelectedSeqGroup()) {
			gmodel.setSelectedSeqGroup(group);
		}

		initVersion(versionName);

		// Select the persistent chromosome, and restore the span.
		SmartAnnotBioSeq seq = Persistence.restoreSeqSelection(group);
		if (seq == null) {
			seq = group.getSeq(0);
		}
		gmodel.addSeqSelectionListener(this);
		if (gmodel.getSelectedSeq() != seq) {
			gmodel.setSelectedSeq(seq);
		}

		// Try/catch may not be needed.
		try {
			Persistence.restoreSeqVisibleSpan(gviewer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initVersion(String versionName) {
		Application.getSingleton().setNotLockedUpStatus("Loading chromosomes...");
		this.glu.initVersion(versionName); // Make sure this genome versionName's feature names are initialized.
		this.glu.initSeq(versionName); // Make sure the chromosome sequences are initialized.
		Application.getSingleton().setStatus("",false);
	}
	
	/**
	 * Handles clicking of partial residue, all residue, and refresh data buttons.
	 * @param evt
	 */
	public void actionPerformed(ActionEvent evt) {
		final Object src = evt.getSource();
		if (src == refresh_dataB) {
			loadVisibleData();
			return;
		}
		if (src != partial_residuesB && src != all_residuesB) {
			return;
		}

		Application.getSingleton().setNotLockedUpStatus("Loading residues");

		final String genomeVersionName = (String) versionCB.getSelectedItem();


		final SmartAnnotBioSeq curSeq = (SmartAnnotBioSeq)gmodel.getSelectedSeq();
		// Use a SwingWorker to avoid locking up the GUI.
		Executor vexec = ThreadUtils.getPrimaryExecutor(src);

		SwingWorker worker = new SwingWorker() {

			public Object doInBackground() {
				if (src == partial_residuesB) {
					SeqSpan viewspan = gviewer.getVisibleSpan();
					if (!glu.loadResidues(genomeVersionName, curSeq, viewspan.getMin(), viewspan.getMax(), viewspan)) {
						// Load the full sequence if the partial one couldn't be loaded.
						if (!glu.loadResidues(genomeVersionName, curSeq, 0, curSeq.getLength(), null)) {
							ErrorHandler.errorPanel("Couldn't load sequence",
											"Was not able to locate the sequence.");
						}
					}
				} else {
					if (!glu.loadResidues(genomeVersionName, curSeq, 0, curSeq.getLength(), null)) {
						ErrorHandler.errorPanel("Couldn't load sequence",
										"Was not able to locate the sequence.");
					}
				}
				return null;
			}
			@Override
			public void done() {
				Application.getSingleton().setStatus("",false);
			}
		};

		vexec.execute(worker);
	}

	/**
	 * Load any data that's marked for visible range.
	 */
	private void loadVisibleData() {
		SeqSpan request_span = gviewer.getVisibleSpan();

		if (DEBUG_EVENTS) {
			System.out.println("Visible load request span: " + request_span.getStart() + " " + request_span.getEnd());
		}

		SmartAnnotBioSeq curSeq = (SmartAnnotBioSeq)gmodel.getSelectedSeq();

		// Load any features that have a visible strategy and haven't already been loaded.
		String genomeVersionName = (String) versionCB.getSelectedItem();
		for (GenericFeature gFeature : this.glu.getFeatures(genomeVersionName)) {
			if (gFeature.loadStrategy != LoadStrategy.VISIBLE) {
				continue;
			}
			// Even if it's already loaded, we may want to reload... for example, if the viewsize changes.

			if (!gFeature.LoadStatusMap.containsKey(curSeq)) {
				// Should never get here.
				System.out.println("ERROR!  " + curSeq.getID() + " does not contain feature status");
			}

			if (DEBUG_EVENTS) {
				System.out.println("Selected : " + gFeature.featureName);
			}
			this.glu.loadAndDisplayAnnotations(gFeature, curSeq, feature_model);
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

	/**
	 * The species combo box changed.
	 * If the species changes to SELECT, the SelectedSeqGroup is set to null.
	 * If the species changes to a specific organism and there's only one choice for the genome versionName, the SelectedSeqGroup is set to that versionName.
	 * Otherwise, the SelectedSetGroup is set to null.
	 */
	private void speciesCBChanged() {
		String speciesName = (String) speciesCB.getSelectedItem();

		// Populate the versionName CB
		refreshVersionCB(speciesName);

		// Set the selected seq group if there's only one possible choice for the versionName.
		if (!speciesName.equals(SELECT_SPECIES) && this.glu.species2genericVersionList != null) {
			if (versionCB.getItemCount() == 2) {
				// There is precisely one genome versionName, and the versionCB has precisely one genome versionName (and the SELECT option)
				List<GenericVersion> versionList = this.glu.species2genericVersionList.get(speciesName);
				String versionName = versionList.get(0).versionName;
				AnnotatedSeqGroup genome = gmodel.getSeqGroup(versionName);
				if (genome != null && genome != gmodel.getSelectedSeqGroup() && versionCB.getItemAt(1).equals(versionName)) {
					initVersion(versionName);
					gmodel.setSelectedSeqGroup(genome);

					// TODO: Need to be certain that the group is selected at this point!
					gmodel.setSelectedSeq(genome.getSeq(0));
					return;
				}
			}
		}

		if (gmodel.getSelectedSeqGroup() != null) {
			gmodel.setSelectedSeqGroup(null);
		}
	}

	/**
	 * The versionName combo box changed.
	 * This changes the selected group (either to null, or to a valid group).
	 * It is assumed that at this point, the species is valid.
	 */
	private void versionCBChanged() {
		String versionName = (String) versionCB.getSelectedItem();
		if (DEBUG_EVENTS) {
			System.out.println("Selected version: " + versionName);
		}

		if (versionName.equals(SELECT_GENOME)) {
					// Select the null group (and the null seq), if it's not already selected.
			gmodel.setSelectedSeqGroup(null);
			gmodel.setSelectedSeq(null);
			return;
		}

		AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
		if (group == null) {
			System.out.println("Group was null -- trying species instead");
			group = gmodel.getSeqGroup(this.glu.versionName2species.get(versionName));
		}

		initVersion(versionName);
		gmodel.setSelectedSeqGroup(group);

		// TODO: Need to be certain that the group is selected at this point!
		gmodel.setSelectedSeq(group.getSeq(0));
	
	}



	/**
	 * Refresh the genome versions, now that the species has changed.
	 * If there's precisely one versionName, just select it.
	 * @param speciesName
	 */
	private void refreshVersionCB(String speciesName) {
		versionCB.removeItemListener(this);
		versionCB.removeAllItems();
		versionCB.addItem(SELECT_GENOME);
		versionCB.setSelectedIndex(0);

		if (speciesName.equals(SELECT_SPECIES)) {
			// Disable the versionName.
			versionCB.setEnabled(false);
			return;
		}

		// Add versionName names to combo boxes.
		// Since the same versionName name may occur on multiple servers, we use sets
		// to eliminate the redundant elements.
		SortedSet<String> versionNames = new TreeSet<String>();
		for (GenericVersion gVersion : this.glu.species2genericVersionList.get(speciesName)) {
			versionNames.add(gVersion.versionName);
		}

		for (String versionName : versionNames) {
			versionCB.addItem(versionName);
		}
		versionCB.setEnabled(true);
		versionCB.addItemListener(this);
	}

	/**
	 * This gets called when the genome versionName is changed.
	 * This occurs via the combo boxes, or by an external event like bookmarks, or LoadFileAction
	 * @param evt
	 */
	public void groupSelectionChanged(GroupSelectionEvent evt) {
		AnnotatedSeqGroup group = evt.getSelectedGroup();

		if (DEBUG_EVENTS) {
			System.out.println("GeneralLoadView.groupSelectionChanged() called, group: " + (group == null ? null : group.getID()));
		}
		if (group == null) {
			/*if (speciesCB.getSelectedItem() != SELECT_SPECIES) {
				speciesCB.removeItemListener(this);
				speciesCB.setSelectedItem(SELECT_SPECIES);
				speciesCB.addItemListener(this);
			}*/
			if (versionCB.getSelectedItem() != SELECT_GENOME) {
				versionCB.removeItemListener(this);
				versionCB.setSelectedItem(SELECT_GENOME);
				versionCB.setEnabled(false);
				versionCB.addItemListener(this);
			}
			curGroup = null;
			return;
		}
		if (curGroup == group) {
			if (DEBUG_EVENTS) {
				System.out.println("GeneralLoadView.groupSelectionChanged(): group was same as previous.");
			}
			return;
		}
		curGroup = group;

		List<GenericVersion> gVersions = group.getVersions();
		if (gVersions.isEmpty()) {
			createUnknownVersion(group);
			return;
		}
		String versionName = gVersions.get(0).versionName;
		if (versionName == null) {
			System.out.println("ERROR -- couldn't find version");
			return;
		}
		String speciesName = this.glu.versionName2species.get(versionName);
		if (speciesName == null) {
			// Couldn't find species matching this versionName -- we have problems.
			System.out.println("ERROR - Couldn't find species for version " + versionName);
			return;
		}

		if (!speciesName.equals(speciesCB.getSelectedItem())) {
			// Set the selected species (the combo box is already populated)
			speciesCB.removeItemListener(this);
			speciesCB.setSelectedItem(speciesName);
			speciesCB.addItemListener(this);
		}
		if (!versionName.equals(versionCB.getSelectedItem())) {
			refreshVersionCB(speciesName);			// Populate the versionName CB
			versionCB.removeItemListener(this);
			versionCB.setSelectedItem(versionName);
			versionCB.addItemListener(this);
		}

		clearFeaturesTable();

		disableAllButtons();

		//this.glu.initSeq(versionName);	// Make sure the chromosome sequences are initialized.
	}

	/**
	 * Changed the selected chromosome.
	 * @param evt
	 */
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		SmartAnnotBioSeq aseq = (SmartAnnotBioSeq) evt.getSelectedSeq();

		if (DEBUG_EVENTS) {
			System.out.println("GeneralLoadView.seqSelectionChanged() called, aseq: " + (aseq == null ? null : aseq.getID()));
		}

		if (aseq == null) {
			clearFeaturesTable();
			disableAllButtons();
			curSeq = null;
			return;
		}
		if (curSeq == aseq) {
			if (DEBUG_EVENTS) {
				System.out.println("GeneralLoadView.seqSelectionChanged(): group was same as previous.");
			}
			return;
		}
		curSeq = aseq;

		// validate that this sequence is in our group.
		AnnotatedSeqGroup group = aseq.getSeqGroup();
		if (group == null) {
			if (DEBUG_EVENTS) {
				System.out.println("sequence was null");
			}
			return;
		}
		List<GenericVersion> gVersions = group.getVersions();
		if (gVersions.isEmpty()) {
			createUnknownVersion(group);
			return;
		}

		String speciesName = (String) this.speciesCB.getSelectedItem();
		String versionName = (String) this.versionCB.getSelectedItem();
		if (speciesName.equals(SELECT_SPECIES) || versionName.equals(SELECT_GENOME)) {
			return;
		}

		if (!(gVersions.get(0).versionName.equals(versionName))) {
			System.out.println("ERROR - version doesn't match");
			return;
		}

		Application.getSingleton().setNotLockedUpStatus("Loading features");

		createFeaturesTable();
		loadWholeRangeFeatures(versionName);
		Application.getSingleton().setStatus("",false);
	}


	/**
	 * group has been created independently of the discovery process (probably by loading a file).
	 * create new "unknown" species/versionName.
	 */
	private void createUnknownVersion(AnnotatedSeqGroup group) {
		gmodel.removeGroupSelectionListener(this);
		gmodel.removeSeqSelectionListener(this);

		speciesCB.removeItemListener(this);
		versionCB.removeItemListener(this);
		GenericVersion gVersion = this.glu.getUnknownVersion(group);
		String species = this.glu.versionName2species.get(gVersion.versionName);
		initializeSpeciesCB();
		if (DEBUG_EVENTS) {
			System.out.println("Species is " + species + ", version is " + gVersion.versionName);
		}

		if (!species.equals(speciesCB.getSelectedItem())) {
			gmodel.removeGroupSelectionListener(this);
			gmodel.removeSeqSelectionListener(this);

			speciesCB.removeItemListener(this);
			versionCB.removeItemListener(this);

			// Set the selected species (the combo box is already populated)
			speciesCB.setSelectedItem(species);
			// populate the versionName combo box.
			refreshVersionCB(species);
		}

		initVersion(gVersion.versionName);

		versionCB.setSelectedItem(gVersion.versionName);
		versionCB.setEnabled(true);
		all_residuesB.setEnabled(false);
		partial_residuesB.setEnabled(false);
		refresh_dataB.setEnabled(false);
		addListeners();
	}


	private void clearFeaturesTable() {
		this.feature_model = new FeaturesTableModel(this, null, null);
		this.feature_table.setModel(this.feature_model);
		featuresTableScrollPane.setViewportView(this.feature_table);
		this.feature_tree_view.clearTreeView();
	}

	/**
	 * Create the table with the list of features and their status.
	 */
	void createFeaturesTable() {
		String versionName = (String) this.versionCB.getSelectedItem();
		SmartAnnotBioSeq curSeq = (SmartAnnotBioSeq) gmodel.getSelectedSeq();
		if (DEBUG_EVENTS) {
			System.out.println("Creating new table with chrom " + (curSeq == null ? null : curSeq.getID()));
		}

		List<GenericFeature> features = this.glu.getFeatures(versionName);
		if (DEBUG_EVENTS) {
			System.out.println("features for " + versionName + ": " + features.toString());
		}
		if (features == null || features.isEmpty()) {
			clearFeaturesTable();
			this.feature_tree_view.clearTreeView();
			return;
		}
		this.feature_tree_view.initOrRefreshTree(features);

		if (DEBUG_EVENTS) {
			System.out.println("Creating table with features: " + features.toString());
		}

		this.feature_model = new FeaturesTableModel(this, features, curSeq);
		this.feature_model.fireTableDataChanged();
		this.feature_table = new JTableX(this.feature_model);
		this.feature_table.setRowHeight(20);    // TODO: better than the default value of 16, but still not perfect.

		// Handle sizing of the columns
		this.feature_table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);   // Allow columns to be resized
		int maxFeatureNameLength = 1;
		for (GenericFeature feature : features) {
			maxFeatureNameLength = Math.max(maxFeatureNameLength, feature.featureName.length());
		}
		// the second column contains the feature names.  Resize it so that feature names are fully displayed.
		TableColumn col = this.feature_table.getColumnModel().getColumn(FeaturesTableModel.FEATURE_NAME_COLUMN);
		col.setPreferredWidth(maxFeatureNameLength);

		// Don't enable combo box for full genome sequence
		TableWithVisibleComboBox.setComboBoxEditors(this.feature_table, FeaturesTableModel.LOAD_STRATEGY_COLUMN, !this.IsGenomeSequence());

		this.feature_model.fireTableDataChanged();
		featuresTableScrollPane.setViewportView(this.feature_table);


		disableButtonsIfNecessary();
		changeVisibleDataButtonIfNecessary(features);	// might have been disabled when switching to another chromosome or genome.
	}

	/**
	 * Load any features that have a whole strategy and haven't already been loaded.
	 * @param versionName
	 */
	private void loadWholeRangeFeatures(String versionName) {
		SmartAnnotBioSeq curSeq = (SmartAnnotBioSeq) gmodel.getSelectedSeq();
		for (GenericFeature gFeature : this.glu.getFeatures(versionName)) {
			if (gFeature.loadStrategy != LoadStrategy.WHOLE) {
				continue;
			}

			if (!gFeature.LoadStatusMap.containsKey(curSeq)) {
				System.out.println("ERROR!  " + curSeq.getID() + " does not contain feature status");
			}
			LoadStatus ls = gFeature.LoadStatusMap.get(curSeq);
			if (ls != LoadStatus.UNLOADED) {
				continue;
			}
			if (gFeature.gVersion.gServer.serverType == ServerType.QuickLoad) {
				// These have already been loaded(QuickLoad is loaded for the entire genome at once)
				if (ls == LoadStatus.UNLOADED) {
					gFeature.LoadStatusMap.put(curSeq, LoadStatus.LOADED);
				}
				continue;
			}

			if (DEBUG_EVENTS) {
				System.out.println("Selected : " + gFeature.featureName);
			}
			this.glu.loadAndDisplayAnnotations(gFeature, curSeq, feature_model);
		}
	}

	/**
	 * Don't allow buttons to be used if they're not valid.
	 * @param seqID
	 */
	private void disableButtonsIfNecessary() {
		// Don't allow buttons for a full genome sequence
		boolean enabled = !IsGenomeSequence();
		if (enabled) {
			SmartAnnotBioSeq curSeq = (SmartAnnotBioSeq) gmodel.getSelectedSeq();
			enabled = curSeq.getSeqGroup() != null;	// Don't allow a null sequence group either.
			if (enabled) {		// Don't allow buttons for an "unknown" versionName
				List<GenericVersion> gVersions = curSeq.getSeqGroup().getVersions();
				enabled = (!gVersions.isEmpty() && gVersions.get(0).gServer.serverType != ServerType.Unknown);
			}
		}

		all_residuesB.setEnabled(enabled);
		partial_residuesB.setEnabled(enabled);
		refresh_dataB.setEnabled(enabled);
	}

	private void disableAllButtons() {
		all_residuesB.setEnabled(false);
		partial_residuesB.setEnabled(false);
		refresh_dataB.setEnabled(false);
	}

	/**
	 * Accessor method.
	 * See if we need to enable/disable the refresh_dataB button
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

		if (refresh_dataB.isEnabled() != enabled) {
			refresh_dataB.setEnabled(enabled);
		}
	}

	boolean IsGenomeSequence() {
		// hardwiring names for genome and encode virtual seqs, need to generalize this
		SmartAnnotBioSeq curSeq = (SmartAnnotBioSeq) gmodel.getSelectedSeq();
		final String seqID = curSeq == null ? null : curSeq.getID();
		return (seqID == null || ENCODE_REGIONS_ID.equals(seqID) || GENOME_SEQ_ID.equals(seqID));
	}

}

