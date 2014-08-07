/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.BioSeqUtils;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.ResidueTrackSymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderInst;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithResidues;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import static com.affymetrix.igb.view.load.GeneralLoadUtils.LOADING_MESSAGE_PREFIX;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.JTree;

/**
 * a class for initializing components and background methods implementation.
 *
 * @author nick & david
 */
public final class GeneralLoadView {

    private static final boolean DEBUG_EVENTS = false;
    private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
    private static SeqMapView gviewer;
    private static GenericAction refreshDataAction;
    private static JRPButton partial_residuesB;
    private static DataManagementTableModel tableModel;
    private FeatureTreeView feature_tree_view;
    private static GeneralLoadView singleton;
    private static IGBService igbService;
    //gui components
    private static JTableX table;
    private static javax.swing.JTree tree;
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
        table = new JTableX("GeneralLoadView_DataManagementTable", tableModel);
        table.setCellSelectionEnabled(false);
        TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(table);
        initDataManagementTable();
        refreshDataAction = gviewer.getRefreshDataAction();
        partial_residuesB = gviewer.getPartial_residuesButton();
        partial_residuesB.setEnabled(false);
        refreshDataAction.setEnabled(false);
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

    public void initVersion(String versionName) {
        igbService.addNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), versionName));
        try {
            GeneralLoadUtils.initVersionAndSeq(versionName); // Make sure this genome versionName's feature names are initialized.
        } finally {
            igbService.removeNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), versionName));
        }
    }

    /**
     * Handles clicking of partial residue, all residue, and refresh data
     * buttons.
     */
    public void loadResidues(final boolean partial) {
        final BioSeq seq = gmodel.getSelectedSeq();

        CThreadWorker<Boolean, Void> worker = new CThreadWorker<Boolean, Void>(MessageFormat.format(BUNDLE.getString(partial ? "loadPartialResidues" : "loadAllResidues"), seq.getID()), Thread.MIN_PRIORITY) {

            public Boolean runInBackground() {
                return loadResidues(seq, gviewer.getVisibleSpan(), partial, false, true);
            }

            @Override
            public void finished() {
                try {
                    if (!isCancelled() && get()) {
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
        CThreadHolder.getInstance().execute(this, worker);
    }

    public boolean loadResidues(SeqSpan span, boolean tryFull) {
        if (!span.isForward()) {
            span = new SimpleSeqSpan(span.getMin(), span.getMax(), span.getBioSeq());
        }
        return loadResidues(span.getBioSeq(), span, true, tryFull, false);
    }

    private boolean loadResidues(final BioSeq seq,
            final SeqSpan viewspan, final boolean partial, final boolean tryFull, final boolean show_error_panel) {
        final String genomeVersionName = (String) SeqGroupView.getInstance().getVersionCB().getSelectedItem();
        try {
            if (partial) {
                if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, viewspan.getMin(), viewspan.getMax(), viewspan)
                        && !Thread.currentThread().isInterrupted()) {
                    if (!tryFull) {
                        if (show_error_panel) {
                            ErrorHandler.errorPanel("Couldn't load partial sequence", "Couldn't locate the partial sequence.  Try loading the full sequence.", Level.INFO);
                        }
                        Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING, "Unable to load partial sequence");
                        return false;
                    } else {
                        if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)) {
                            if (show_error_panel) {
                                ErrorHandler.errorPanel("Couldn't load partial or full sequence", "Couldn't locate the sequence.", Level.SEVERE);
                            }
                            Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING,
                                    "Couldn't load partial or full sequence. Couldn't locate the sequence.");
                            return false;
                        }
                    }
                }
            } else {
                if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)
                        && !Thread.currentThread().isInterrupted()) {
                    if (show_error_panel) {
                        ErrorHandler.errorPanel("Couldn't load full sequence", "Couldn't locate the sequence.", Level.SEVERE);
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
//		loadStrategies.add(LoadStrategy.CHROMOSOME);
        //TODO refactor code to not use serverType == null as a hack
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
     */
    public static void loadWholeRangeFeatures(ServerTypeI serverType) {
        List<LoadStrategy> loadStrategies = new ArrayList<LoadStrategy>();
        loadStrategies.add(LoadStrategy.GENOME);
        loadFeatures(loadStrategies, serverType);
    }

    static void loadFeatures(List<LoadStrategy> loadStrategies, ServerTypeI serverType) {
        for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
            if (GeneralLoadUtils.isLoaded(gFeature)) {
                continue;
            }
            loadFeature(loadStrategies, gFeature, serverType);
        }
    }

    static boolean loadFeature(List<LoadStrategy> loadStrategies, GenericFeature gFeature, ServerTypeI serverType) {
        if (!loadStrategies.contains(gFeature.getLoadStrategy())) {
            return false;
        }

        //TODO refactor code to not use serverType == null as a hack
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
                                            BioSeqUtils.addResiduesToComposition(seq, rchild.getResidues(), rchild.getSpan(seq));
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
                        gviewer.updatePanel();
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

    private static void refreshDataManagementTable(final List<GenericFeature> visibleFeatures) {

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
        table.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setPreferredWidth(finalMaxFeatureNameLength);
        table.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setMinWidth(110);
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
        table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setPreferredWidth(35);
        table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setMinWidth(35);
        table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setMaxWidth(35);

        // Don't enable combo box for full genome sequence
        // Enabling of combo box for local files with unknown chromosomes happens in setComboBoxEditors()
        DataManagementTable.setComboBoxEditors(table, !GeneralLoadView.IsGenomeSequence());
    }

    /**
     * Check if it is necessary to disable buttons.
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

    public GenericFeature createFeature(String featureName, SymLoader loader) {
        GenericVersion version = GeneralLoadUtils.getIGBFilesVersion(GenometryModel.getGenometryModel().getSelectedSeqGroup(), getSelectedSpecies());
        GenericFeature feature = new GenericFeature(featureName, null, version, loader, null, false);
        version.addFeature(feature);
        feature.setVisible(); // this should be automatically checked in the feature tree
        ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getIGBFilesServer(), LoadUtils.ServerStatus.Initialized, true);
        refreshDataManagementView();

        return feature;
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

        CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>(LOADING_MESSAGE_PREFIX + feature.featureName, Thread.MIN_PRIORITY) {

            @Override
            protected Object runInBackground() {
                TrackView.getInstance().addEmptyTierFor(feature, gviewer);
                return null;
            }

            @Override
            protected void finished() {
                AbstractAction action = new AbstractAction() {

                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(ActionEvent e) {
                        refreshDataManagementTable(GeneralLoadUtils.getVisibleFeatures());
                        gviewer.getSeqMap().packTiers(false, true, true);
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
                removeFeature(feature, true);
            }
        }
    }

    public void removeFeature(final GenericFeature feature, final boolean refresh) {
        removeFeature(feature, refresh, true);
    }

    public void clearTrack(final ITrackStyleExtended style) {
        final String method = style.getMethodName();
        if (method != null) {
            final BioSeq bioseq = GenometryModel.getGenometryModel().getSelectedSeq();
            final GenericFeature feature = style.getFeature();

            // If genome is selected then delete all syms on the all seqs.
            if (IGBConstants.GENOME_SEQ_ID.equals(bioseq.getID())) {
                removeFeature(feature, true);
                return;
            }

            CThreadWorker<Void, Void> clear = new CThreadWorker<Void, Void>("Clearing track  " + style.getTrackName()) {

                @Override
                protected Void runInBackground() {
                    TrackView.getInstance().deleteSymsOnSeq(gviewer, method, bioseq, feature);
                    return null;
                }

                @Override
                protected void finished() {
                    TierGlyph tier = TrackView.getInstance().getTier(style, TierGlyph.Direction.FORWARD);
                    if (tier != null) {
                        tier.removeAllChildren();
                        tier.setInfo(null);
                    }
                    tier = TrackView.getInstance().getTier(style, TierGlyph.Direction.REVERSE);
                    if (tier != null) {
                        tier.removeAllChildren();
                        tier.setInfo(null);
                    }
                    TrackView.getInstance().addTierFor(style, gviewer);
                    gviewer.getSeqMap().repackTheTiers(true, true, true);
                }

            };

            CThreadHolder.getInstance().execute(feature, clear);
        }
    }

    void removeFeature(final GenericFeature feature, final boolean refresh, final boolean removeLocal) {
        if (feature == null) {
            return;
        }

        CThreadWorker<Void, Void> delete = new CThreadWorker<Void, Void>("Removing feature  " + feature.featureName) {

            @Override
            protected Void runInBackground() {
                if (!feature.getMethods().isEmpty()) {
                    for (String method : feature.getMethods()) {
                        for (BioSeq bioseq : feature.gVersion.group.getSeqList()) {
                            TrackView.getInstance().deleteSymsOnSeq(gviewer, method, bioseq, feature);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void finished() {
                boolean refSeq = feature.gVersion.gServer.serverType.equals(ServerTypeI.LocalFiles) && feature.symL.isResidueLoader();
                if (removeLocal || refSeq) {
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

                if (refresh) {
                    removeTier(feature.getURI().toString());
                    if (!feature.getMethods().isEmpty()) {
                        for (String method : feature.getMethods()) {
                            removeTier(method);
                        }
                    }
                    feature.clear();

                    // Refresh
                    refreshTreeViewAndRestore();
                    refreshDataManagementView();
                    //gviewer.dataRemoved();
                    gviewer.getSeqMap().repackTheTiers(true, true, true);
                }

                ((AffyLabelledTierMap) gviewer.getSeqMap()).fireTierOrderChanged();
            }

            private void removeTier(String method) {
                ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method);
                TierGlyph tier = TrackView.getInstance().getTier(style, TierGlyph.Direction.FORWARD);
                if (tier != null) {
                    gviewer.getSeqMap().removeTier(tier);
                }
                tier = TrackView.getInstance().getTier(style, TierGlyph.Direction.REVERSE);
                if (tier != null) {
                    gviewer.getSeqMap().removeTier(tier);
                }

                if (style.isGraphTier()) {
                    DefaultStateProvider.getGlobalStateProvider().removeGraphState(method);
                } else {
                    DefaultStateProvider.getGlobalStateProvider().removeAnnotStyle(method);
                }
            }
        };

        CThreadHolder.getInstance().execute(feature, delete);

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
