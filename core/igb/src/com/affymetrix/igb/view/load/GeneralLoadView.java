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

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symloader.ResidueTrackSymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderInst;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.LoadPartialSequenceAction;
import com.affymetrix.igb.action.LoadWholeSequenceAction;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.view.TrackView;
import java.awt.Font;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 *
 * @author nick & david
 */
public final class GeneralLoadView {

	private static final boolean DEBUG_EVENTS = false;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final String LOAD = IGBConstants.BUNDLE.getString("load");
	private static GenericAction refreshDataAction;
	private static SeqMapView gviewer;
	private static DataManagementTableModel tableModel;
	private FeatureTreeView feature_tree_view;
	private static GeneralLoadView singleton;
	private static IGBService igbService;
	//gui components
	private static JRPButton all_residuesB;
	private static JTableX table;
	private static JRPButton partial_residuesB;
	private static JRPButton refresh_dataB;
	private static javax.swing.JTree tree;
	private static Font font = new Font("SansSerif", Font.BOLD, 16);
	private boolean showLoadingConfirm = false;

	public static void init(IGBService _igbService) {
		singleton = new GeneralLoadView(_igbService);
	}

	public static synchronized GeneralLoadView getLoadView() {
		return singleton;
	}

	/**
	 * Creates new form GeneralLoadView
	 */
	private GeneralLoadView(IGBService _igbService) {
		igbService = _igbService;
		gviewer = Application.getSingleton().getMapView();
		initComponents();
		GeneralLoadUtils.loadServerMapping();
		PreferencesPanel.getSingleton();
	}

	private void initComponents() {
		feature_tree_view = new FeatureTreeView();
		tree = feature_tree_view.getTree();
		
		tableModel = new DataManagementTableModel(this);
		tableModel.addTableModelListener(TrackstylePropertyMonitor.getPropertyTracker());
		table = new JTableX(tableModel);
		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(table);
		initDataManagementTable();
		
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

	public JTree getTree() {
		return tree;
	}

	public DataManagementTableModel getTableModel() {
		return tableModel;
	}

	public JTable getTable() {
		return table;
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
	 * Handles clicking of partial residue, all residue, and refresh data
	 * buttons.
	 *
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

		CThreadWorker<Boolean, Void> worker = new CThreadWorker<Boolean, Void>("load " + (partial ? "partial" : "all") + " residues for " + seq, Thread.MIN_PRIORITY) {

			public Boolean runInBackground() {
				return loadResidues(genomeVersionName, seq, gviewer.getVisibleSpan(), partial, false, true);
			}

			@Override
			public void finished() {
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
		CThreadHolder.getInstance().execute(src, worker);
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
	 * Load any features that have a autoload strategy and haven't already been
	 * loaded.
	 */
	public static void loadAutoLoadFeatures() {
		List<LoadStrategy> loadStrategies = new ArrayList<LoadStrategy>();
		loadStrategies.add(LoadStrategy.AUTOLOAD);
		loadFeatures(loadStrategies, null);
		GeneralLoadUtils.bufferDataForAutoload();
	}

	/**
	 * Load any features that have a whole strategy and haven't already been
	 * loaded.
	 *
	 * @param versionName
	 */
	public static void loadWholeRangeFeatures(ServerTypeI serverType) {
		List<LoadStrategy> loadStrategies = new ArrayList<LoadStrategy>();
		loadStrategies.add(LoadStrategy.GENOME);
		loadFeatures(loadStrategies, serverType);
	}

	/**
	 * @param gFeature the feature to check
	 * @return if this feature is "preloaded", that is, it has a view mode that
	 * is displayed without "Load Data", like Semantic zooming
	 */
	/*
	 * private static boolean isPreLoaded(GenericFeature gFeature) { if
	 * (gFeature.getMethods().size() > 1) { return false; } String method =
	 * null; if (gFeature.getMethods().size() == 0) { if (gFeature.symL != null
	 * && gFeature.symL.uri != null) { method = gFeature.symL.uri.toString(); }
	 * } else { method = gFeature.getMethods().iterator().next(); } if (method
	 * == null) { return false; } ITrackStyleExtended style =
	 * TrackView.getInstance().getStyle(method, gFeature); TierGlyph tierGlyph =
	 * TrackView.getInstance().getTier(style, Direction.BOTH); if (tierGlyph !=
	 * null && tierGlyph.getViewModeGlyph() != null) { return
	 * tierGlyph.getViewModeGlyph().isPreLoaded(); } return false; }
	 */
	static void loadFeatures(List<LoadStrategy> loadStrategies, ServerTypeI serverType) {
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			if (GeneralLoadUtils.isLoaded(gFeature)) {
				continue;
			}
//			if (isPreLoaded(gFeature)) {
//				continue;
//			}

			loadFeature(loadStrategies, gFeature, serverType);
		}
	}

	static boolean loadFeature(List<LoadStrategy> loadStrategies, GenericFeature gFeature, ServerTypeI serverType) {
		if (!loadStrategies.contains(gFeature.getLoadStrategy())) {
			return false;
		}

		if (serverType != null && gFeature.gVersion.gServer.serverType != serverType) {
			return false;
		}

		GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);

		return true;
	}

	public synchronized static void AutoloadQuickloadFeature() {
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			if (gFeature.getLoadStrategy() != LoadStrategy.GENOME
					|| gFeature.gVersion.gServer.serverType != ServerTypeI.QuickLoad) {
				continue;
			}

			if (GeneralLoadUtils.isLoaded(gFeature)) {
				continue;
			}

			//If Loading whole genome for unoptimized file then load everything at once.
			if (((QuickLoadSymLoader) gFeature.symL).getSymLoader() instanceof SymLoaderInst) {
				GeneralLoadUtils.loadAllSymmetriesThread(gFeature);
			} else {
				GeneralLoadUtils.iterateSeqList(gFeature);
			}
		}
	}

	public void useAsRefSequence(final GenericFeature feature) throws Exception {
		if (feature != null && feature.symL != null) {
			final QuickLoadSymLoader quickload = (QuickLoadSymLoader) feature.symL;
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
			tableModel.clearFeatures();
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

	private static void refreshDataManagementTable(final List<GenericFeature> visibleFeatures){
		
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				table.stopCellEditing();
				tableModel.createVirtualFeatures(visibleFeatures);
				DataManagementTable.setComboBoxEditors(table, !GeneralLoadView.IsGenomeSequence());
			}
		});
	}
	
	public void refreshDataManagementView() {
		final List<GenericFeature> visibleFeatures = GeneralLoadUtils.getVisibleFeatures();
		refreshDataManagementTable(visibleFeatures);
		
		disableButtonsIfNecessary();
		changeVisibleDataButtonIfNecessary(visibleFeatures);
	}

	private void initDataManagementTable() {
		final List<GenericFeature> visibleFeatures = GeneralLoadUtils.getVisibleFeatures();
		int maxFeatureNameLength = 1;
		for (GenericFeature feature : visibleFeatures) {
			maxFeatureNameLength = Math.max(maxFeatureNameLength, feature.featureName.length());
		}
		final int finalMaxFeatureNameLength = maxFeatureNameLength;	// necessary for threading
		table.stopCellEditing();
		tableModel.createVirtualFeatures(visibleFeatures);

		table.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setPreferredWidth(20);
		table.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setMinWidth(20);
		table.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setMaxWidth(20);
		table.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setPreferredWidth(24);
		table.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setMinWidth(24);
		table.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setMaxWidth(24);
		table.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setPreferredWidth(120);
		table.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setMinWidth(110);
		table.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setMaxWidth(130);
		table.getColumnModel().getColumn(DataManagementTableModel.FEATURE_NAME_COLUMN).setPreferredWidth(finalMaxFeatureNameLength);
		table.getColumnModel().getColumn(DataManagementTableModel.FEATURE_NAME_COLUMN).setMinWidth(110);
		//dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.FEATURE_NAME_COLUMN).setMaxWidth(200);
		table.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setPreferredWidth(130);
		table.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setMinWidth(130);
		table.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setPreferredWidth(15);
		table.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setMinWidth(15);
		table.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setMaxWidth(15);
		table.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setPreferredWidth(29);
		table.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setMinWidth(29);
		table.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setMaxWidth(29);
		table.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setPreferredWidth(27);
		table.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setMinWidth(27);
		table.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setMaxWidth(27);
		table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setPreferredWidth(55);
		table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setMinWidth(55);
		table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setMaxWidth(55);

		// Don't enable combo box for full genome sequence
		// Enabling of combo box for local files with unknown chromosomes happens in setComboBoxEditors()
		DataManagementTable.setComboBoxEditors(table, !GeneralLoadView.IsGenomeSequence());
	}

	/**
	 * Check if it is necessary to disable buttons.
	 *
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
	 * Accessor method. See if we need to enable/disable the refresh_dataB
	 * button by looking at the features' load strategies.
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

		refreshDataManagementView();
	}

	public static void addFeatureTier(final GenericFeature feature) {

		CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>("Loading feature " + feature.featureName, Thread.MIN_PRIORITY) {

			@Override
			protected Object runInBackground() {
				TrackView.getInstance().addEmptyTierFor(feature, gviewer, true);
				return null;
			}

			@Override
			protected void finished() {
				AbstractAction action = new AbstractAction() {

					private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent e) {
						refreshDataManagementTable(GeneralLoadUtils.getVisibleFeatures());
						gviewer.getSeqMap().packTiers(true, true, false);
						gviewer.getSeqMap().stretchToFit(false, true);
						gviewer.getSeqMap().updateWidget();
						TierPrefsView.getSingleton().refreshList();
					}
				};
				gviewer.preserveSelectionAndPerformAction(action);
			}
		};

		CThreadHolder.getInstance().execute(feature, worker);
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
							TrackView.getInstance().deleteSymsOnSeq(gviewer.getSeqMap(), method, bioseq, feature);
						}
					}
				}

				feature.clear();

				if (removeLocal) {
					// If feature is local then remove it from server.
					GenericVersion version = feature.gVersion;
					if (version.gServer.serverType.equals(ServerTypeI.LocalFiles)) {
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
					refreshDataManagementView();
					gviewer.dataRemoved();
				}
			}
		};

		CThreadHolder.getInstance().execute(feature, delete);

	}

	protected GenericAction getRefreshDataAction() {
		return refreshDataAction;
	}

	public FeatureTreeView getFeatureTree() {
		return feature_tree_view;
	}

	public void setShowLoadingConfirm(boolean showLoadingConfirm) {
		this.showLoadingConfirm = showLoadingConfirm;
	}

	public boolean isLoadingConfirm() {
		return showLoadingConfirm;
	}
}
