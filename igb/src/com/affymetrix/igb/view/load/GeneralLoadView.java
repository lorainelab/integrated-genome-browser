/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingWorker;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symloader.ResidueTrackSymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderInst;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.view.DataLoadPrefsView;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.LoadPartialSequenceAction;
import com.affymetrix.igb.action.LoadWholeSequenceAction;
import com.affymetrix.igb.featureloader.QuickLoad;
import com.affymetrix.igb.glyph.EmptyTierGlyphFactory;
import com.affymetrix.igb.view.TierPrefsView;
import com.affymetrix.igb.view.TrackView;
import java.awt.Font;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author lorainelab
 */
public final class GeneralLoadView {

	private static final boolean DEBUG_EVENTS = false;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	public static int TAB_DATALOAD_PREFS = -1;
	private static final String LOAD = IGBConstants.BUNDLE.getString("load");
	private static GenericAction refreshDataAction;
	private static SeqMapView gviewer;
	private static DataManagementTableModel dataManagementTableModel;
	private FeatureTreeView feature_tree_view;
	private static GeneralLoadView singleton;
	private static IGBService igbService;
	//gui components 
	private static JRPButton all_residuesB;
	private static javax.swing.JTable dataManagementTable;
	private static JRPButton partial_residuesB;
	private static JRPButton refresh_dataB;
	private static javax.swing.JTree tree;
	private static Font font = new Font("SansSerif", Font.BOLD, 16);

	public static void init(IGBService _igbService) {
		singleton = new GeneralLoadView(_igbService);
	}

	public static synchronized GeneralLoadView getLoadView() {
		return singleton;
	}

	/** Creates new form GeneralLoadView */
	private GeneralLoadView(IGBService _igbService) {
		igbService = _igbService;
		gviewer = Application.getSingleton().getMapView();
		initComponents();
		GeneralLoadUtils.loadServerMapping();
		final PreferencesPanel pp = PreferencesPanel.getSingleton();
		TAB_DATALOAD_PREFS = pp.addPrefEditorComponent(DataLoadPrefsView.getSingleton());
	}

	private void initComponents() {
		feature_tree_view = new FeatureTreeView();
		tree = feature_tree_view.getTree();
		dataManagementTableModel = new DataManagementTableModel(this);
		dataManagementTable = new JTableX(dataManagementTableModel);
		refreshDataAction = gviewer.getRefreshDataAction();
		refresh_dataB = new JRPButton("DataAccess_refreshData", refreshDataAction);
		all_residuesB = new JRPButton("DataAccess_allSequence", LoadWholeSequenceAction.getAction());
		partial_residuesB = new JRPButton("DataAccess_sequenceInView", LoadPartialSequenceAction.getAction());

		all_residuesB.setToolTipText(MessageFormat.format(LOAD, IGBConstants.BUNDLE.getString("nucleotideSequence")));
		all_residuesB.setMaximumSize(all_residuesB.getPreferredSize());
		all_residuesB.setIcon(CommonUtils.getInstance().getIcon("images/dna.gif"));
		all_residuesB.setEnabled(false);
		all_residuesB.setFont(font);
		all_residuesB.setText("Load All Sequence");

		partial_residuesB.setToolTipText(MessageFormat.format(LOAD, IGBConstants.BUNDLE.getString("partialNucleotideSequence")));
		partial_residuesB.setMaximumSize(partial_residuesB.getPreferredSize());
		partial_residuesB.setEnabled(false);
		partial_residuesB.setFont(font);
		partial_residuesB.setIcon(CommonUtils.getInstance().getIcon("images/dna.gif"));
		partial_residuesB.setText("Load Sequence In View");

		refresh_dataB.setToolTipText(BUNDLE.getString("refreshDataTip"));
		refresh_dataB.setMaximumSize(refresh_dataB.getPreferredSize());
		refreshDataAction.setEnabled(false);
		refresh_dataB.setFont(font);
		refresh_dataB.setIcon(MenuUtil.getIcon("images/refresh22.png"));
		refresh_dataB.setText("Load Data");
	}

	public javax.swing.JTree getTree() {
		return tree;
	}

	public DataManagementTableModel getLoadModeDataTableModel() {
		return dataManagementTableModel;
	}

	public javax.swing.JTable getDataManagementTable() {
		return dataManagementTable;
	}

	public JRPButton getPartial_residuesButton() {
		return partial_residuesB;
	}

	public JRPButton getAll_ResiduesButton() {
		return all_residuesB;
	}

	public JRPButton getRefreshDataButton() {
		return refresh_dataB;
	}

	public GenericAction getRefreshAction() {
		return refreshDataAction;
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
	public void loadResidues(GenericAction action) {
		Object src = null;

		if (action.equals(partial_residuesB.getAction())) {
			src = partial_residuesB;
		} else if (action.equals(all_residuesB.getAction())) {
			src = all_residuesB;
		}

		if (src != partial_residuesB && src != all_residuesB) {
			return;
		}

		final String genomeVersionName = (String) SeqGroupView.getInstance().getVersionCB().getSelectedItem();
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
					Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
//					igbService.removeNotLockedUpMsg("Loading residues for " + seq.getID());
				}
			}
		};

		// Use a SwingWorker to avoid locking up the GUI.
		ThreadUtils.getPrimaryExecutor(src).execute(worker);
	}

	public boolean loadResiduesInView(boolean tryFull) {
		final String genomeVersionName = (String) SeqGroupView.getInstance().getVersionCB().getSelectedItem();
		SeqSpan visibleSpan = gviewer.getVisibleSpan();
		return loadResidues(genomeVersionName, visibleSpan.getBioSeq(), visibleSpan, true, tryFull, false);
	}

	public boolean loadResidues(SeqSpan span, boolean tryFull) {
		final String genomeVersionName = (String) SeqGroupView.getInstance().getVersionCB().getSelectedItem();
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
						Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING, "Unable to load partial sequence");
						return false;
					} else {
						if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)) {
							if (show_error_panel) {
								ErrorHandler.errorPanel("Couldn't load partial or full sequence", "Couldn't locate the sequence.");
							}
							Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING,
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
					Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING,
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
	public static void loadWholeRangeFeatures(ServerType serverType) {
		List<LoadStrategy> loadStrategies = new ArrayList<LoadStrategy>();
		loadStrategies.add(LoadStrategy.GENOME);
		loadFeatures(loadStrategies, serverType);
	}

	static void loadFeatures(List<LoadStrategy> loadStrategies, ServerType serverType) {
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			loadFeature(loadStrategies, gFeature, serverType);
		}
	}

	static boolean loadFeature(List<LoadStrategy> loadStrategies, GenericFeature gFeature, ServerType serverType) {
		if (!loadStrategies.contains(gFeature.getLoadStrategy())) {
			return false;
		}

		if (serverType != null && gFeature.gVersion.gServer.serverType != serverType) {
			return false;
		}

		GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);

		return true;
	}

	public static void AutoloadQuickloadFeature() {
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			if (gFeature.getLoadStrategy() != LoadStrategy.GENOME
					|| gFeature.gVersion.gServer.serverType != ServerType.QuickLoad) {
				continue;
			}

			//If Loading whole genome for unoptimized file then load everything at once.
			if (((QuickLoad) gFeature.symL).getSymLoader() instanceof SymLoaderInst) {
				((QuickLoad) gFeature.symL).loadAllSymmetriesThread(gFeature);
			} else {
				GeneralLoadUtils.iterateSeqList(gFeature);
			}
		}
	}

	public void useAsRefSequence(final GenericFeature feature) throws Exception {
		if (feature != null && feature.symL != null) {
			final QuickLoad quickload = (QuickLoad) feature.symL;
			if (quickload.getSymLoader() instanceof ResidueTrackSymLoader) {

				final CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>(feature.featureName) {

					@Override
					protected Void runInBackground() {
						try {
							SymWithProps sym;
							SeqSymmetry child;
							SimpleSymWithResidues rchild;

							for (BioSeq seq : feature.symL.getChromosomeList()) {
								sym = seq.getAnnotation(feature.getURI().toString());
								if (sym != null) {

									//Clear previous sequence
									seq.setComposition(null);

									for (int i = 0; i < sym.getChildCount(); i++) {
										child = sym.getChild(i);
										if (child instanceof SimpleSymWithResidues) {
											rchild = (SimpleSymWithResidues) child;
											BioSeq.addResiduesToComposition(seq, rchild.getResidues(), rchild.getSpan(seq));
										}
									}
									seq.removeAnnotation(sym);
								}
							}

							((ResidueTrackSymLoader) quickload.getSymLoader()).loadAsReferenceSequence(true);
							TrackView.updateDependentData();

						} catch (Exception ex) {
							ex.printStackTrace();
						}
						return null;
					}

					@Override
					protected void finished() {
						gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, true, true);
					}
				};

				worker.execute();
			}
		}
	}

	public void refreshTreeView() {

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				refreshTree();
			}
		});
	}

	private void refreshTree() {
		final List<GenericFeature> features = GeneralLoadUtils.getSelectedVersionFeatures();
		if (features == null || features.isEmpty()) {
			dataManagementTableModel.clearFeatures();
		}
		feature_tree_view.initOrRefreshTree(features);
	}

	public void refreshTreeViewAndRestore() {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				String state = feature_tree_view.getState();
				refreshTree();
				feature_tree_view.restoreState(state);
			}
		});
	}

	/**
	 * Create the table with the list of features and their status.
	 */
	public List<GenericFeature> createFeaturesTable() {
		String versionName = (String) SeqGroupView.getInstance().getVersionCB().getSelectedItem();
		final List<GenericFeature> visibleFeatures = GeneralLoadUtils.getVisibleFeatures();

		if (DEBUG_EVENTS) {
			BioSeq curSeq = gmodel.getSelectedSeq();
			System.out.println("Creating new table with chrom " + (curSeq == null ? null : curSeq.getID()));
			System.out.println("features for " + versionName + ": " + visibleFeatures.toString());
		}
		ThreadUtils.runOnEventQueue(new Runnable() {
			public void run() {
				initDataManagementTable();
			}
		});

		disableButtonsIfNecessary();
		changeVisibleDataButtonIfNecessary(visibleFeatures);	// might have been disabled when switching to another chromosome or genome.
		return visibleFeatures;
	}
	
	
	public void initDataManagementTable() {
		final List<GenericFeature> visibleFeatures = GeneralLoadUtils.getVisibleFeatures();
		int maxFeatureNameLength = 1;
		for (GenericFeature feature : visibleFeatures) {
			maxFeatureNameLength = Math.max(maxFeatureNameLength, feature.featureName.length());
		}
		final int finalMaxFeatureNameLength = maxFeatureNameLength;	// necessary for threading
		dataManagementTableModel.createVirtualFeatures(visibleFeatures);

		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setPreferredWidth(20);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setMinWidth(20);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setMaxWidth(20);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setPreferredWidth(24);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setMinWidth(24);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setMaxWidth(24);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setPreferredWidth(80);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setMinWidth(80);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setMaxWidth(130);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.FEATURE_NAME_COLUMN).setPreferredWidth(finalMaxFeatureNameLength);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.FEATURE_NAME_COLUMN).setMinWidth(110);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setPreferredWidth(130);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setMinWidth(130);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setPreferredWidth(15);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setMinWidth(15);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setMaxWidth(15);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setPreferredWidth(28);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setMinWidth(28);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setMaxWidth(28);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setPreferredWidth(28);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setMinWidth(28);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setMaxWidth(28);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setPreferredWidth(60);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setMinWidth(60);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setMaxWidth(60);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.INFO_FEATURE_COLUMN).setPreferredWidth(20);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.INFO_FEATURE_COLUMN).setMinWidth(20);
		dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.INFO_FEATURE_COLUMN).setMaxWidth(20);

		dataManagementTable.getTableHeader().setReorderingAllowed(false);
		TableCellRenderer renderer = dataManagementTable.getTableHeader().getDefaultRenderer();
		JLabel label = (JLabel) renderer;
		label.setHorizontalAlignment(JLabel.CENTER);
		dataManagementTable.getTableHeader().setDefaultRenderer(renderer);

		Font f = new Font("SansSerif", Font.BOLD, 12);
		dataManagementTable.getTableHeader().setFont(f);
		dataManagementTable.setRowSelectionAllowed(false);
		dataManagementTable.setCellSelectionEnabled(true);

		// Don't enable combo box for full genome sequence
		// Enabling of combo box for local files with unknown chromosomes happens in setComboBoxEditors()
		DataManagementTable.setComboBoxEditors((JTableX) dataManagementTable, !GeneralLoadView.IsGenomeSequence());
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

	public void disableAllButtons() {
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
		return (String) SeqGroupView.getInstance().getSpeciesCB().getSelectedItem();
	}

	public void addFeature(final GenericFeature feature) {
		feature.setVisible();

		List<LoadStrategy> loadStrategies = new java.util.ArrayList<LoadStrategy>();
		loadStrategies.add(LoadStrategy.GENOME);

		if (!loadFeature(loadStrategies, feature, null)) {
			addFeatureTier(feature);
		}

		createFeaturesTable();

	}

	public static void addFeatureTier(final GenericFeature feature) {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				EmptyTierGlyphFactory.addEmtpyTierfor(feature, gviewer);
				List<SeqSymmetry> syms = gviewer.getSelectedSyms();
				if(!syms.isEmpty())
					gviewer.select(new ArrayList<SeqSymmetry>(1), true);
		
				gviewer.getSeqMap().packTiers(true, true, false, false);
				gviewer.getSeqMap().stretchToFit(false, true);
				gviewer.getSeqMap().updateWidget();
				
				if(!syms.isEmpty())
					gviewer.select(syms, false);
				
				TierPrefsView.getSingleton().refreshList();
			}
		});
	}
	
	void removeAllFeautres(Set<GenericFeature> features) {
		for (GenericFeature feature : features) {
			if (feature.isVisible()) {
				GeneralLoadView.getLoadView().removeFeature(feature, true);
			}
		}
	}
	
	public void removeFeature(final GenericFeature feature, final boolean refresh) {
		removeFeature(feature, refresh, true);
	}
	
	void removeFeature(final GenericFeature feature, final boolean refresh, final boolean removeLocal) {
		if (feature == null) {
			return;
		}

		CThreadWorker<Void, Void> delete = new CThreadWorker<Void, Void>("Removing feature  " + feature.featureName) {

			@Override
			protected Void runInBackground() {
				if (!feature.getMethods().isEmpty()) {
					for (BioSeq bioseq : feature.gVersion.group.getSeqList()) {
						for (String method : feature.getMethods()) {
							TrackView.deleteDependentData(gviewer.getSeqMap(), method, bioseq);
							TrackView.deleteSymsOnSeq(gviewer.getSeqMap(), method, bioseq, feature);
						}
					}
				}

				feature.clear();

				if (removeLocal) {
					// If feature is local then remove it from server.
					GenericVersion version = feature.gVersion;
					if (version.gServer.serverType.equals(ServerType.LocalFiles)) {
						if (version.removeFeature(feature)) {
							SeqGroupView.getInstance().refreshTable();
							if (gmodel.getSelectedSeqGroup().getSeqCount() > 0
									&& !gmodel.getSelectedSeqGroup().getSeqList().contains(gmodel.getSelectedSeq())) {
								gmodel.setSelectedSeq(gmodel.getSelectedSeqGroup().getSeqList().get(0));
							} else {
								gmodel.setSelectedSeq(null);
							}
						}
					}
				}

				return null;
			}

			@Override
			protected void finished() {
				if (refresh) {
					// Refresh
					refreshTreeViewAndRestore();
					createFeaturesTable();
					gviewer.dataRemoved();
				}
			}
		};

		ThreadUtils.getPrimaryExecutor(feature).execute(delete);

	}

	protected GenericAction getRefreshDataAction() {
		return refreshDataAction;
	}

	public static DataManagementTableModel getLoadModeTableModel() {
		if (dataManagementTableModel != null) {
			return dataManagementTableModel;
		}
		return null;
	}

	public FeatureTreeView getFeatureTree() {
		if (feature_tree_view != null) {
			return feature_tree_view;
		}
		return null;
	}
}
