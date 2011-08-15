package com.affymetrix.igb.view.load;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.comparator.StringVersionDateComparator;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.TierMaintenanceListenerHolder;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBoxWithSingleListener;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBox;

import com.affymetrix.igb.general.Persistence;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.view.DataLoadPrefsView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.LoadSequence;
import com.affymetrix.igb.util.JComboBoxToolTipRenderer;
import com.affymetrix.igb.util.ScriptFileLoader;
import com.affymetrix.igb.util.ThreadHandler;
import com.affymetrix.igb.view.TrackView;
import java.awt.Font;
import javax.swing.table.TableCellRenderer;

public final class GeneralLoadView extends IGBTabPanel
		implements ItemListener, GroupSelectionListener, SeqSelectionListener, GenericServerInitListener {

	private static final long serialVersionUID = 1L;
	private static final int TAB_POSITION = Integer.MIN_VALUE;
	private static final boolean DEBUG_EVENTS = false;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final String SELECT_SPECIES = IGBConstants.BUNDLE.getString("speciesCap");
	private static final String SELECT_GENOME = IGBConstants.BUNDLE.getString("genomeVersionCap");
	private static final String CHOOSE = "Choose";
	public static int TAB_DATALOAD_PREFS = -1;
	private static final String LOAD = IGBConstants.BUNDLE.getString("load");
	private AnnotatedSeqGroup curGroup = null;
	private final JRPComboBox versionCB;
	private final JComboBoxToolTipRenderer versionCBRenderer;
	private final JRPButton all_residuesB;
	private final JRPButton partial_residuesB;
	private final JRPComboBox speciesCB;
	private final JComboBoxToolTipRenderer speciesCBRenderer;
	private final AbstractAction refreshDataAction;
	private static SeqMapView gviewer;
	private static JTableX loadedTracksTable;
	private static LoadModeDataTableModel loadModeDataTableModel;
	JScrollPane featuresTableScrollPane;
	private final FeatureTreeView feature_tree_view;
	//private TrackInfoView track_info_view;
	private volatile boolean lookForPersistentGenome = true;	// Once this is set to false, don't invoke persistent genome code
	private final JSplitPane jSplitPane;
	private static GeneralLoadView singleton;

	public static void init(IGBService _igbService) {
		singleton = new GeneralLoadView(_igbService);
	}

	public static synchronized GeneralLoadView getLoadView() {
		return singleton;
	}

	private GeneralLoadView(IGBService _igbService) {
		super(_igbService, BUNDLE.getString("dataAccessTab"), BUNDLE.getString("dataAccessTab"), true, TAB_POSITION);
		gviewer = Application.getSingleton().getMapView();
		this.setLayout(new BorderLayout());

		JPanel choicePanel = new JPanel();
		choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.X_AXIS));
		choicePanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

		speciesCB = new JRPComboBoxWithSingleListener("DataAccess_species");
		speciesCB.addItem(SELECT_SPECIES);
		speciesCB.setMaximumSize(new Dimension(speciesCB.getPreferredSize().width * 4, speciesCB.getPreferredSize().height));
		speciesCB.setEnabled(false);
		speciesCB.setEditable(false);
		speciesCB.setToolTipText(CHOOSE + " " + SELECT_SPECIES);

		speciesCBRenderer = new JComboBoxToolTipRenderer();
		speciesCB.setRenderer(speciesCBRenderer);
		speciesCBRenderer.setToolTipEntry(SELECT_SPECIES, CHOOSE + " " + SELECT_SPECIES);

		choicePanel.add(new JLabel(CHOOSE + ":"));
		choicePanel.add(Box.createHorizontalStrut(5));
		choicePanel.add(speciesCB);
		choicePanel.add(Box.createHorizontalStrut(50));

		versionCB = new JRPComboBoxWithSingleListener("DataAccess_version");
		versionCB.addItem(SELECT_GENOME);
		versionCB.setMaximumSize(new Dimension(versionCB.getPreferredSize().width * 4, versionCB.getPreferredSize().height));
		versionCB.setEnabled(false);
		versionCB.setEditable(false);
		versionCB.setToolTipText(CHOOSE + " " + SELECT_GENOME);

		versionCBRenderer = new JComboBoxToolTipRenderer();
		versionCB.setRenderer(versionCBRenderer);
		versionCBRenderer.setToolTipEntry(SELECT_GENOME, CHOOSE + " " + SELECT_GENOME);

		choicePanel.add(versionCB);


		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 3));

		all_residuesB = new JRPButton("DataAccess_allSequence", LoadSequence.getWholeAction());
		all_residuesB.setToolTipText(MessageFormat.format(LOAD, IGBConstants.BUNDLE.getString("nucleotideSequence")));
		all_residuesB.setMaximumSize(all_residuesB.getPreferredSize());
		all_residuesB.setEnabled(false);
		buttonPanel.add(all_residuesB);
		partial_residuesB = new JRPButton("DataAccess_sequenceInView", LoadSequence.getPartialAction());
		partial_residuesB.setToolTipText(MessageFormat.format(LOAD, IGBConstants.BUNDLE.getString("partialNucleotideSequence")));
		partial_residuesB.setMaximumSize(partial_residuesB.getPreferredSize());
		partial_residuesB.setEnabled(false);
		buttonPanel.add(partial_residuesB);
		this.refreshDataAction = gviewer.getRefreshDataAction();
		JRPButton refresh_dataB = new JRPButton("DataAccess_refreshData", refreshDataAction);
		refresh_dataB.setToolTipText(BUNDLE.getString("refreshDataTip"));
		refresh_dataB.setMaximumSize(refresh_dataB.getPreferredSize());
		refreshDataAction.setEnabled(false);
		buttonPanel.add(refresh_dataB);
		this.add("South", buttonPanel);

		loadModeDataTableModel = new LoadModeDataTableModel(this);
		loadedTracksTable = new JTableX(loadModeDataTableModel);
		loadedTracksTable.setModel(loadModeDataTableModel);
		loadedTracksTable.setRowHeight(20);    // TODO: better than the default value of 16, but still not perfect.
		// Handle sizing of the columns
		loadedTracksTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);   // Allow columns to be resized

		featuresTableScrollPane = new JScrollPane(GeneralLoadView.loadedTracksTable);
		featuresTableScrollPane.setViewportView(loadedTracksTable);

		JPanel featuresPanel = new JPanel();
		featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
		//featuresPanel.add(new JLabel(IGBConstants.BUNDLE.getString("chooseLoadMode")));
		featuresPanel.add(featuresTableScrollPane);


		this.add("North", choicePanel);

		/* COMMENTED OUT.  The Track Info table makes the data load view
		 *                 too busy, so for now, the code is commented out
		 */
//		track_info_view = new TrackInfoView();
//		JSplitPane featurePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, featuresPanel, track_info_view);
//		featurePane.setResizeWeight(0.5);
//		JSplitPane jPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.feature_tree_view, featurePane);

		this.feature_tree_view = new FeatureTreeView();
		jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.feature_tree_view, featuresPanel);
		jSplitPane.setResizeWeight(0.5);
		this.add("Center", jSplitPane);

		this.setBorder(BorderFactory.createEtchedBorder());

		ServerList.getServerInstance().addServerInitListener(this);

		GeneralLoadUtils.loadServerMapping();
		populateSpeciesData();
		addListeners();
		final PreferencesPanel pp = PreferencesPanel.getSingleton();
		TAB_DATALOAD_PREFS = pp.addPrefEditorComponent(new DataLoadPrefsView());
	}

	private void addListeners() {
		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);

		speciesCB.setEnabled(true);
		versionCB.setEnabled(true);
		speciesCB.addItemListener(this);
		versionCB.addItemListener(this);
		speciesCB.addItemListener(new Welcome());
	}

	/**
	 * Discover servers, species, etc., asynchronously.
	 * @param loadGenome parameter to check if genomes should be loaded from
	 * actual server or not.
	 */
	private void populateSpeciesData() {
		for (final GenericServer gServer : ServerList.getServerInstance().getEnabledServers()) {
			Executor vexec = Executors.newSingleThreadExecutor();
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

				protected Void doInBackground() throws Exception {
					GeneralLoadUtils.discoverServer(gServer);
					return null;
				}
			};

			vexec.execute(worker);
		}
	}

	public void genericServerInit(GenericServerInitEvent evt) {
		boolean areAllServersInited = ServerList.getServerInstance().areAllServersInited();	// do this first to avoid race condition
		GenericServer gServer = (GenericServer) evt.getSource();

		if (gServer.getServerStatus() == ServerStatus.NotResponding) {
			refreshTreeView();
			return;
		}

		if (gServer.getServerStatus() != ServerStatus.Initialized) {
			return;	// ignore uninitialized servers
		}

		if (gServer.serverType != ServerType.LocalFiles) {
			if (gServer.serverType == null) {
				igbService.removeNotLockedUpMsg("Loading repository " + gServer);
			} else {
				igbService.removeNotLockedUpMsg("Loading server " + gServer + " (" + gServer.serverType.toString() + ")");
			}
		}

		// Need to refresh species names
		boolean speciesListener = this.speciesCB.getItemListeners().length > 0;
		String speciesName = (String) this.speciesCB.getSelectedItem();
		refreshSpeciesCB();

		if (speciesName != null && !speciesName.equals(SELECT_SPECIES)) {
			lookForPersistentGenome = false;
			String versionName = (String) this.versionCB.getSelectedItem();

			//refresh version names if a species is selected
			refreshVersionCB(speciesName);

			if (versionName != null && !versionName.equals(SELECT_GENOME)) {
				// refresh this version
				initVersion(versionName);

				// TODO: refresh feature tree view if a version is selected
				refreshTreeView();
			}
		}

		if (speciesListener) {
			this.speciesCB.addItemListener(this);
		}

		if (areAllServersInited) {
			runBatchOrRestore();
		}

	}

	private void runBatchOrRestore() {
		try {
			// Only run batch script or restore persistent genome once all the server responses have come back.
			String batchFile = IGB.commandLineBatchFileStr;
			if (batchFile != null) {
				IGB.commandLineBatchFileStr = null;	// we're not using this again!
				lookForPersistentGenome = false;
				Thread.sleep(1000);	// hack so event queue finishes
				ScriptFileLoader.runScript(batchFile);
			} else {
				if (lookForPersistentGenome) {
					lookForPersistentGenome = false;
					Thread.sleep(1000);	// hack so event queue finishes
					RestorePersistentGenome();
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(GeneralLoadView.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * bootstrap bookmark from Preferences for last species/versionName/genome / sequence / region
	 */
	private void RestorePersistentGenome() {
		// Get group and seq info from persistent preferences.
		// (Recovering as much data as possible before activating listeners.)
		final AnnotatedSeqGroup group = Persistence.restoreGroupSelection();
		if (group == null) {
			return;
		}

		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions == null || gVersions.isEmpty()) {
			return;
		}
		final String versionName = GeneralLoadUtils.getPreferredVersionName(gVersions);
		if (versionName == null || GeneralLoadUtils.versionName2species.get(versionName) == null || gmodel.getSeqGroup(versionName) != group) {
			return;
		}

		if (gmodel.getSelectedSeqGroup() != null || gmodel.getSelectedSeq() != null) {
			return;
		}

		CThreadWorker worker = new CThreadWorker("Loading previous genome " + versionName + " ...") {

			@Override
			protected Object runInBackground() {

				initVersion(versionName);

				gmodel.setSelectedSeqGroup(group);

				List<GenericFeature> features = GeneralLoadUtils.getSelectedVersionFeatures();
				if (features == null || features.isEmpty()) {
					return null;
				}

				BioSeq seq = Persistence.restoreSeqSelection(group);
				if (seq == null) {
					seq = group.getSeq(0);
					if (seq == null) {
						return null;
					}
				}


				// Try/catch may not be needed.
				try {
					Persistence.restoreSeqVisibleSpan(gviewer);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return seq;
			}

			@Override
			protected void finished() {
				try {
					gmodel.setSelectedSeq((BioSeq) get());
				} catch (Exception ex) {
					Logger.getLogger(GeneralLoadView.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		};

		ThreadHandler.getThreadHandler().execute(worker, worker);
	}

	/**
	 * Initialize Species combo box.  It is assumed that we have the species data at this point.
	 * If a species was already selected, leave it as the selected species.
	 */
	private void refreshSpeciesCB() {
		speciesCB.removeItemListener(this);
		int speciesListLength = GeneralLoadUtils.species2genericVersionList.keySet().size();
		if (speciesListLength == speciesCB.getItemCount() - 1) {
			// No new species.  Don't bother refreshing.
			return;
		}

		final List<String> speciesList = new ArrayList<String>();
		speciesList.addAll(GeneralLoadUtils.species2genericVersionList.keySet());
		Collections.sort(speciesList);

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				String oldSpecies = (String) speciesCB.getSelectedItem();

				speciesCB.removeAllItems();
				speciesCB.addItem(SELECT_SPECIES);
				for (String speciesName : speciesList) {
					speciesCBRenderer.setToolTipEntry(speciesName, SpeciesLookup.getCommonSpeciesName(speciesName));
					speciesCB.addItem(speciesName);
				}
				if (oldSpecies == null) {
					return;
				}

				if (speciesList.contains(oldSpecies)) {
					speciesCB.setSelectedItem(oldSpecies);
				} else {
					// species CB changed
					speciesCBChanged();
				}
			}
		});
	}

	/**
	 * Refresh the genome versions.
	 * @param speciesName
	 */
	private void refreshVersionCB(final String speciesName) {
		final List<GenericVersion> versionList = GeneralLoadUtils.species2genericVersionList.get(speciesName);
		final List<String> versionNames = new ArrayList<String>();
		if (versionList != null) {
			for (GenericVersion gVersion : versionList) {
				// the same versionName name may occur on multiple servers
				String versionName = gVersion.versionName;
				if (!versionNames.contains(versionName)) {
					versionNames.add(versionName);
					versionCBRenderer.setToolTipEntry(versionName, GeneralLoadUtils.listSynonyms(versionName));
				}
			}
			Collections.sort(versionNames, new StringVersionDateComparator());
		}

		// Sort the versions (by date)

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				versionCB.removeItemListener(GeneralLoadView.this);
				String oldVersion = (String) versionCB.getSelectedItem();

				if (versionList == null || speciesName.equals(SELECT_SPECIES)) {
					versionCB.setSelectedIndex(0);
					versionCB.setEnabled(false);
					return;
				}

				// Add names to combo boxes.
				versionCB.removeAllItems();
				versionCB.addItem(SELECT_GENOME);
				for (String versionName : versionNames) {
					versionCB.addItem(versionName);
				}
				versionCB.setEnabled(true);
				if (oldVersion != null && !oldVersion.equals(SELECT_GENOME) && GeneralLoadUtils.versionName2species.containsKey(oldVersion)) {
					versionCB.setSelectedItem(oldVersion);
				} else {
					versionCB.setSelectedIndex(0);
				}
				if (versionCB.getItemCount() > 1) {
					versionCB.addItemListener(GeneralLoadView.this);
				}
			}
		});
	}

	public void initVersion(String versionName) {
		igbService.addNotLockedUpMsg("Loading chromosomes for " + versionName);
		try {
			GeneralLoadUtils.initVersionAndSeq(versionName); // Make sure this genome versionName's feature names are initialized.
		} finally {
			igbService.removeNotLockedUpMsg("Loading chromosomes for " + versionName);
		}
	}

	/**
	 * Handles clicking of partial residue, all residue, and refresh data buttons.
	 * @param evt
	 */
	public void loadResidues(AbstractAction action) {
		Object src = null;

		if (action.equals(partial_residuesB.getAction())) {
			src = partial_residuesB;
		} else if (action.equals(all_residuesB.getAction())) {
			src = all_residuesB;
		}

		if (src != partial_residuesB && src != all_residuesB) {
			return;
		}

		final String genomeVersionName = (String) versionCB.getSelectedItem();
		final BioSeq seq = gmodel.getSelectedSeq();
		final boolean partial = src == partial_residuesB;

		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

			public Boolean doInBackground() {
				return loadResidues(genomeVersionName, seq, gviewer.getVisibleSpan(), partial, false, true);
			}

			@Override
			public void done() {
				try {
					if (get()) {
						gviewer.setAnnotatedSeq(seq, true, true, true);
					}
				} catch (Exception ex) {
					Logger.getLogger(GeneralLoadView.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
//					igbService.removeNotLockedUpMsg("Loading residues for " + seq.getID());
				}
			}
		};

		// Use a SwingWorker to avoid locking up the GUI.
		ThreadUtils.getPrimaryExecutor(src).execute(worker);
	}

	public boolean loadResiduesInView(boolean tryFull) {
		final String genomeVersionName = (String) versionCB.getSelectedItem();
		SeqSpan visibleSpan = gviewer.getVisibleSpan();
		return loadResidues(genomeVersionName, visibleSpan.getBioSeq(), visibleSpan, true, tryFull, false);
	}

	public boolean loadResidues(SeqSpan span, boolean tryFull) {
		final String genomeVersionName = (String) versionCB.getSelectedItem();
		if (!span.isForward()) {
			span = new SimpleSeqSpan(span.getMin(), span.getMax(), span.getBioSeq());
		}
		return loadResidues(genomeVersionName, span.getBioSeq(), span, true, tryFull, false);
	}

	public boolean loadResidues(final String genomeVersionName, final BioSeq seq,
			final SeqSpan viewspan, final boolean partial, final boolean tryFull, final boolean show_error_panel) {
		try {
			if (partial) {
				if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, viewspan.getMin(), viewspan.getMax(), viewspan)) {
					if (!tryFull) {
						if (show_error_panel) {
							ErrorHandler.errorPanel("Couldn't load partial sequence", "Couldn't locate the partial sequence.  Try loading the full sequence.");
						}
						Logger.getLogger(GeneralLoadView.class.getName()).log(Level.WARNING, "Unable to load partial sequence");
						return false;
					} else {
						if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)) {
							if (show_error_panel) {
								ErrorHandler.errorPanel("Couldn't load partial or full sequence", "Couldn't locate the sequence.");
							}
							Logger.getLogger(GeneralLoadView.class.getName()).log(Level.WARNING,
									"Couldn't load partial or full sequence. Couldn't locate the sequence.");
							return false;
						}
					}
				}
			} else {
				if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)) {
					if (show_error_panel) {
						ErrorHandler.errorPanel("Couldn't load full sequence", "Couldn't locate the sequence.");
					}
					Logger.getLogger(GeneralLoadView.class.getName()).log(Level.WARNING,
							"Couldn't load full sequence. Couldn't locate the sequence.");
					return false;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Load any data that's marked for visible range.
	 */
	public void loadVisibleFeatures() {
		if (DEBUG_EVENTS) {
			SeqSpan request_span = gviewer.getVisibleSpan();
			System.out.println("Visible load request span: " + request_span.getBioSeq() + ":" + request_span.getStart() + "-" + request_span.getEnd());
		}
		List<LoadStrategy> loadStrategies = new ArrayList<LoadStrategy>();
		loadStrategies.add(LoadStrategy.AUTOLOAD);
		loadStrategies.add(LoadStrategy.VISIBLE);
		loadStrategies.add(LoadStrategy.CHROMOSOME);
		loadFeatures(loadStrategies, null);
	}

	/**
	 * Load any features that have a autoload strategy and haven't already been loaded.
	 */
	public static void loadAutoLoadFeatures() {
		List<LoadStrategy> loadStrategies = new ArrayList<LoadStrategy>();
		loadStrategies.add(LoadStrategy.AUTOLOAD);
		loadFeatures(loadStrategies, null);
		GeneralLoadUtils.bufferDataForAutoload();
	}

	/**
	 * Load any features that have a whole strategy and haven't already been loaded.
	 * @param versionName
	 */
	static void loadWholeRangeFeatures(ServerType serverType) {
		List<LoadStrategy> loadStrategies = new ArrayList<LoadStrategy>();
		loadStrategies.add(LoadStrategy.GENOME);
		loadFeatures(loadStrategies, serverType);
	}

	static void loadFeatures(List<LoadStrategy> loadStrategies, ServerType serverType) {
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			loadFeature(loadStrategies, gFeature, serverType);
		}
	}

	static void loadFeature(List<LoadStrategy> loadStrategies, GenericFeature gFeature, ServerType serverType) {
		if (!loadStrategies.contains(gFeature.getLoadStrategy())) {
			return;
		}

		if (serverType != null && gFeature.gVersion.gServer.serverType != serverType) {
			return;
		}

		GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
	}

	static void AutoloadQuickloadFeature() {
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			if (gFeature.getLoadStrategy() != LoadStrategy.GENOME
					|| gFeature.gVersion.gServer.serverType != ServerType.QuickLoad) {
				continue;
			}

			GeneralLoadUtils.iterateSeqList(gFeature);
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

		// Select the null group (and the null seq), if it's not already selected.
		if (curGroup != null) {
			gmodel.setSelectedSeqGroup(null); // This method is being called on purpose to fire group selection event.
			gmodel.setSelectedSeq(null);	  // which in turns calls refreshTreeView method.
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

		if (curGroup != null) {
			gmodel.setSelectedSeqGroup(null);
			gmodel.setSelectedSeq(null);
		}

		if (versionName.equals(SELECT_GENOME)) {
			// Select the null group (and the null seq), if it's not already selected.
			return;
		}

		AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
		if (group == null) {
			System.out.println("Group was null -- trying species instead");
			group = gmodel.getSeqGroup(GeneralLoadUtils.versionName2species.get(versionName));
			if (group == null) {
				return;
			}
		}

		speciesCB.setEnabled(false);
		versionCB.setEnabled(false);

		(new InitVersionWorker(versionName, group)).execute();
	}

	/**
	 * Run initialization of version on thread, so we don't lock up the GUI.
	 * Merge with initVersion();
	 */
	private class InitVersionWorker extends SwingWorker<Void, Void> {

		private final String versionName;
		private final AnnotatedSeqGroup group;

		InitVersionWorker(String versionName, AnnotatedSeqGroup group) {
			this.versionName = versionName;
			this.group = group;
		}

		@Override
		public Void doInBackground() {
			igbService.addNotLockedUpMsg("Loading chromosomes for " + versionName);
			GeneralLoadUtils.initVersionAndSeq(versionName); // Make sure this genome versionName's feature names are initialized.
			return null;
		}

		@Override
		protected void done() {
			igbService.removeNotLockedUpMsg("Loading chromosomes for " + versionName);
			speciesCB.setEnabled(true);
			versionCB.setEnabled(true);
			if (curGroup != null || group != null) {
				// avoid calling these a half-dozen times
				gmodel.setSelectedSeqGroup(group);
				// TODO: Need to be certain that the group is selected at this point!
				gmodel.setSelectedSeq(group.getSeq(0));
			}
		}
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
			if (versionCB.getSelectedItem() != SELECT_GENOME) {
				versionCB.removeItemListener(this);
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

		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions.isEmpty()) {
			createUnknownVersion(group);
			return;
		}
		final String versionName = GeneralLoadUtils.getPreferredVersionName(gVersions);
		if (versionName == null) {
			System.out.println("ERROR -- couldn't find version");
			return;
		}
		final String speciesName = GeneralLoadUtils.versionName2species.get(versionName);
		if (speciesName == null) {
			// Couldn't find species matching this versionName -- we have problems.
			System.out.println("ERROR - Couldn't find species for version " + versionName);
			return;
		}

		if (!speciesName.equals(speciesCB.getSelectedItem())) {
			// Set the selected species (the combo box is already populated)
			ThreadUtils.runOnEventQueue(new Runnable() {

				public void run() {
					speciesCB.removeItemListener(GeneralLoadView.this);
					speciesCB.setSelectedItem(speciesName);
					speciesCB.addItemListener(GeneralLoadView.this);
				}
			});
		}
		if (!versionName.equals(versionCB.getSelectedItem())) {
			refreshVersionCB(speciesName);			// Populate the versionName CB
			ThreadUtils.runOnEventQueue(new Runnable() {

				public void run() {
					versionCB.removeItemListener(GeneralLoadView.this);
					versionCB.setSelectedItem(versionName);
					versionCB.addItemListener(GeneralLoadView.this);
				}
			});
		}

		refreshTreeView();	// Replacing clearFeaturesTable with refreshTreeView.
		// refreshTreeView should only be called if feature table
		// needs to be cleared.

		disableAllButtons();
		AutoloadQuickloadFeature();
	}

	/**
	 * Changed the selected chromosome.
	 * @param evt
	 */
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		BioSeq aseq = evt.getSelectedSeq();

		if (DEBUG_EVENTS) {
			System.out.println("GeneralLoadView.seqSelectionChanged() called, aseq: " + (aseq == null ? null : aseq.getID()));
		}

		if (aseq == null) {
			refreshTreeView();	// Replacing clearFeaturesTable with refreshTreeView.
			// refreshTreeView should only be called if feature table
			// needs to be cleared.

			disableAllButtons();
			return;
		}

		// validate that this sequence is in our group.
		AnnotatedSeqGroup group = aseq.getSeqGroup();
		if (group == null) {
			if (DEBUG_EVENTS) {
				System.out.println("sequence was null");
			}
			return;
		}
		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions.isEmpty()) {
			createUnknownVersion(group);
			return;
		}

		String speciesName = (String) this.speciesCB.getSelectedItem();
		String versionName = (String) this.versionCB.getSelectedItem();
		if (speciesName == null || versionName == null || speciesName.equals(SELECT_SPECIES) || versionName.equals(SELECT_GENOME)) {
			return;
		}

		if (!(GeneralLoadUtils.getPreferredVersionName(gVersions).equals(versionName))) {
			/*System.out.println("ERROR - versions don't match: " + versionName + "," +
			GeneralLoadUtils.getPreferredVersionName(gVersions));*/
			return;
		}

		createFeaturesTable();
		loadWholeRangeFeatures(ServerType.DAS2);
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
		GenericVersion gVersion = GeneralLoadUtils.getUnknownVersion(group);
		String species = GeneralLoadUtils.versionName2species.get(gVersion.versionName);
		refreshSpeciesCB();
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
		refreshDataAction.setEnabled(false);
		addListeners();
	}

	public void refreshTreeView() {

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				final List<GenericFeature> features = GeneralLoadUtils.getSelectedVersionFeatures();
				if (features == null || features.isEmpty()) {
					loadModeDataTableModel.clearFeatures();
				}
				feature_tree_view.initOrRefreshTree(features);
			}
		});
	}

	/**
	 * Create the table with the list of features and their status.
	 */
	public List<GenericFeature> createFeaturesTable() {
		String versionName = (String) this.versionCB.getSelectedItem();
		final List<GenericFeature> features = GeneralLoadUtils.getSelectedVersionFeatures();
		if (DEBUG_EVENTS) {
			BioSeq curSeq = gmodel.getSelectedSeq();
			System.out.println("Creating new table with chrom " + (curSeq == null ? null : curSeq.getID()));
			System.out.println("features for " + versionName + ": " + features.toString());
		}

		int maxFeatureNameLength = 1;
		for (GenericFeature feature : features) {
			maxFeatureNameLength = Math.max(maxFeatureNameLength, feature.featureName.length());
		}
		final int finalMaxFeatureNameLength = maxFeatureNameLength;	// necessary for threading

		final List<GenericFeature> visibleFeatures = loadModeDataTableModel.getVisibleFeatures(features);

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				loadModeDataTableModel.createVirtualFeatures(visibleFeatures);

				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.REFRESH_FEATURE_COLUMN).setPreferredWidth(20);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.REFRESH_FEATURE_COLUMN).setMinWidth(20);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.REFRESH_FEATURE_COLUMN).setMaxWidth(20);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.HIDE_FEATURE_COLUMN).setPreferredWidth(24);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.HIDE_FEATURE_COLUMN).setMinWidth(24);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.HIDE_FEATURE_COLUMN).setMaxWidth(24);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.LOAD_STRATEGY_COLUMN).setPreferredWidth(135);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.LOAD_STRATEGY_COLUMN).setMinWidth(110);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.LOAD_STRATEGY_COLUMN).setMaxWidth(150);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.FEATURE_NAME_COLUMN).setPreferredWidth(finalMaxFeatureNameLength);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.FEATURE_NAME_COLUMN).setMinWidth(110);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.FEATURE_NAME_COLUMN).setMaxWidth(200);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.TRACK_NAME_COLUMN).setPreferredWidth(160);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.DELETE_FEATURE_COLUMN).setPreferredWidth(15);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.DELETE_FEATURE_COLUMN).setMinWidth(15);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.DELETE_FEATURE_COLUMN).setMaxWidth(15);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.BACKGROUND_COLUMN).setPreferredWidth(25);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.BACKGROUND_COLUMN).setMinWidth(25);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.BACKGROUND_COLUMN).setMaxWidth(25);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.FOREGROUND_COLUMN).setPreferredWidth(25);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.FOREGROUND_COLUMN).setMinWidth(25);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.FOREGROUND_COLUMN).setMaxWidth(25);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.INFO_FEATURE_COLUMN).setPreferredWidth(20);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.INFO_FEATURE_COLUMN).setMinWidth(20);
				loadedTracksTable.getColumnModel().getColumn(LoadModeDataTableModel.INFO_FEATURE_COLUMN).setMaxWidth(20);

				loadedTracksTable.getTableHeader().setReorderingAllowed(false);
				TableCellRenderer renderer = loadedTracksTable.getTableHeader().getDefaultRenderer();
				JLabel label = (JLabel) renderer;
				label.setHorizontalAlignment(JLabel.CENTER);
				loadedTracksTable.getTableHeader().setDefaultRenderer(renderer);

				Font f = new Font("Serif", Font.BOLD, 12);
				loadedTracksTable.getTableHeader().setFont(f);
				loadedTracksTable.setRowSelectionAllowed(false);
				loadedTracksTable.setCellSelectionEnabled(true);

				// Don't enable combo box for full genome sequence
				// Enabling of combo box for local files with unknown chromosomes happens in setComboBoxEditors()
				LoadModeTable.setComboBoxEditors(loadedTracksTable, !GeneralLoadView.IsGenomeSequence());
			}
		});

		disableButtonsIfNecessary();
		changeVisibleDataButtonIfNecessary(features);	// might have been disabled when switching to another chromosome or genome.
		return features;
	}

	/**
	 * Check if it is necessary to disable buttons.
	 * @return
	 */
	public static boolean getIsDisableNecessary() {
		boolean enabled = !IsGenomeSequence();
		if (enabled) {
			BioSeq curSeq = gmodel.getSelectedSeq();
			enabled = curSeq.getSeqGroup() != null;	// Don't allow a null sequence group either.
			if (enabled) {		// Don't allow buttons for an "unknown" versionName
				Set<GenericVersion> gVersions = curSeq.getSeqGroup().getEnabledVersions();
				enabled = (!gVersions.isEmpty());
			}
		}
		return enabled;
	}

	/**
	 * Don't allow buttons to be used if they're not valid.
	 */
	private void disableButtonsIfNecessary() {
		// Don't allow buttons for a full genome sequence
		setAllButtons(getIsDisableNecessary());
	}

	private void disableAllButtons() {
		setAllButtons(false);
	}

	private void setAllButtons(final boolean enabled) {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				all_residuesB.setEnabled(enabled);
				partial_residuesB.setEnabled(enabled);
				refreshDataAction.setEnabled(enabled);
			}
		});
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
			if (gFeature.getLoadStrategy() != LoadStrategy.NO_LOAD && gFeature.getLoadStrategy() != LoadStrategy.GENOME) {
				enabled = true;
				break;
			}
		}

		if (refreshDataAction.isEnabled() != enabled) {
			refreshDataAction.setEnabled(enabled);
		}
	}

	private static boolean IsGenomeSequence() {
		BioSeq curSeq = gmodel.getSelectedSeq();
		final String seqID = curSeq == null ? null : curSeq.getID();
		return (seqID == null || IGBConstants.GENOME_SEQ_ID.equals(seqID));
	}

	public String getSelectedSpecies() {
		return (String) speciesCB.getSelectedItem();
	}

	public CThreadWorker removeFeature(final GenericFeature feature, final boolean refresh) {
		if (feature == null) {
			return null;
		}

		CThreadWorker<Void, Void> delete = new CThreadWorker<Void, Void>("Removing feature  " + feature.featureName) {

			@Override
			protected Void runInBackground() {
				for (BioSeq bioseq : feature.gVersion.group.getSeqList()) {
					for (String method : feature.getMethods()) {
						TrackView.deleteDependentData(gviewer.getSeqMap(), method, bioseq);
						TrackView.deleteSymsOnSeq(gviewer.getSeqMap(), method, bioseq);
					}
				}

				feature.clear();

				// If feature is local then remove it from server.
				GenericVersion version = feature.gVersion;
				if (version.gServer.serverType.equals(ServerType.LocalFiles)) {
					version.removeFeature(feature);
				}

				return null;
			}

			@Override
			protected void finished() {
				if (refresh) {
					// Refresh
					GeneralLoadView.getLoadView().refreshTreeView();
					GeneralLoadView.getLoadView().createFeaturesTable();
					gviewer.dataRemoved();
				}
				TierMaintenanceListenerHolder.getInstance().fireTierRemoved();
			}
		};

		ThreadUtils.getPrimaryExecutor(feature).execute(delete);

		return delete;
	}

	protected AbstractAction getRefreshDataAction() {
		return refreshDataAction;
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}
}
