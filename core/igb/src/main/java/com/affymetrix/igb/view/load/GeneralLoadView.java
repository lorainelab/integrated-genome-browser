/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.LocalDataProvider;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.quickload.QuickLoadSymLoader;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symloader.BedUtils;
import com.affymetrix.genometry.symloader.ResidueTrackSymLoader;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderInst;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithResidues;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.LoadUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import static com.affymetrix.igb.view.load.GeneralLoadUtils.LOADING_MESSAGE_PREFIX;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.lorainelab.igb.cache.api.RemoteFileCacheService;
import org.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import org.lorainelab.igb.services.IgbService;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.JTree;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

/**
 * a class for initializing components and background methods implementation.
 *
 * @author nick & david
 */
public final class GeneralLoadView {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GeneralLoadView.class);
    private static final boolean DEBUG_EVENTS = false;
    private static final GenometryModel gmodel = GenometryModel.getInstance();
    private static SeqMapView gviewer;
    private static GenericAction refreshDataAction;
    private static JRPButton partial_residuesB;
    private static DataManagementTableModel tableModel;
    private FeatureTreeView feature_tree_view;
    private static GeneralLoadView singleton;
    private static IgbService igbService;
    //gui components
    private static JTableX table;
    private static javax.swing.JTree tree;
    private boolean showLoadingConfirm = false;
    private RemoteFileCacheService remoteFileCacheService;
    private BundleContext bundleContext;

    public static void init(IgbService _igbService) {
        singleton = new GeneralLoadView(_igbService);
    }

    public static synchronized GeneralLoadView getLoadView() {
        return singleton;
    }

    private void initCacheServiceTracker() {
        ServiceTracker<RemoteFileCacheService, Object> dependencyTracker;

        dependencyTracker = new ServiceTracker<RemoteFileCacheService, Object>(bundleContext, RemoteFileCacheService.class, null) {
            @Override
            public Object addingService(ServiceReference<RemoteFileCacheService> serviceReference) {
                remoteFileCacheService = bundleContext.getService(serviceReference);
                return super.addingService(serviceReference);
            }
        };
        dependencyTracker.open();
    }

    /**
     * Creates new form GeneralLoadView
     */
    private GeneralLoadView(IgbService _igbService) {
        final Bundle bundle = FrameworkUtil.getBundle(GeneralLoadView.class);
        if (bundle != null) {
            bundleContext = bundle.getBundleContext();
            initCacheServiceTracker();
        }
        igbService = _igbService;
        gviewer = IGB.getInstance().getMapView();
        initComponents();
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
     * Handles clicking of partial residue, all residue, and refresh data buttons.
     */
    public void loadResidues(final boolean partial) {
        final BioSeq seq = gmodel.getSelectedSeq().orElse(null);

        CThreadWorker<Boolean, Void> worker = new CThreadWorker<Boolean, Void>("Loading residues for " + seq.getId(), Thread.MIN_PRIORITY) {

            @Override
            public Boolean runInBackground() {
                return loadResidues(gviewer.getVisibleSpan(), partial, false, true);
            }

            @Override
            public void finished() {
                try {
                    if (!isCancelled() && get()) {
                        gviewer.setAnnotatedSeq(seq, true, true, true);
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                } finally {
//					igbService.removeNotLockedUpMsg("Loading residues for " + seq.getName());
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
        return loadResidues(span, true, tryFull, false);
    }

    private boolean loadResidues(final SeqSpan viewspan, final boolean partial, final boolean tryFull, final boolean show_error_panel) {
        try {
            if (partial) {
                if (!GeneralLoadUtils.loadResidues(viewspan)
                        && !Thread.currentThread().isInterrupted()) {
                    if (!tryFull) {
                        if (show_error_panel) {
                            ErrorHandler.errorPanel("Couldn't load partial sequence", "Couldn't locate the partial sequence.  Try loading the full sequence.", Level.INFO);
                        }
                        Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING, "Unable to load partial sequence");
                        return false;
                    } else {
                        SimpleSeqSpan simpleSeqSpan = new SimpleSeqSpan(0, viewspan.getLength(), viewspan.getBioSeq());
                        if (!GeneralLoadUtils.loadResidues(simpleSeqSpan)) {
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
                SimpleSeqSpan simpleSeqSpan = new SimpleSeqSpan(0, viewspan.getLength(), viewspan.getBioSeq());
                if (!GeneralLoadUtils.loadResidues(simpleSeqSpan)
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
            logger.error(ex.getMessage(), ex);
            return false;
        }

        return true;
    }

    /**
     * Load any data that's marked for visible range.
     */
    public void loadVisibleFeatures() {
        List<LoadStrategy> loadStrategies = new ArrayList<>();
        loadStrategies.add(LoadStrategy.AUTOLOAD);
        loadStrategies.add(LoadStrategy.VISIBLE);
//		loadStrategies.add(LoadStrategy.CHROMOSOME);
        //TODO refactor code to not use serverType == null as a hack
        loadFeatures(loadStrategies, null);
    }

    /**
     * Load any features that have a autoload strategy and haven't already been loaded.
     */
    public static void loadAutoLoadFeatures() {
        List<LoadStrategy> loadStrategies = new ArrayList<>();
        loadStrategies.add(LoadStrategy.AUTOLOAD);
        loadFeatures(loadStrategies, null);
        GeneralLoadUtils.bufferDataForAutoload();
    }

    /**
     * Load any features that have a whole strategy and haven't already been loaded.
     */
    public static void loadWholeRangeFeatures(DataProvider dataProvider) {
        List<LoadStrategy> loadStrategies = new ArrayList<>();
        loadStrategies.add(LoadStrategy.GENOME);
        loadFeatures(loadStrategies, dataProvider);
    }

    static void loadFeatures(List<LoadStrategy> loadStrategies, DataProvider serverType) {
        for (DataSet dataSet : GeneralLoadUtils.getGenomeVersionDataSets()) {
            if (GeneralLoadUtils.isLoaded(dataSet)) {
                continue;
            }
            loadFeature(loadStrategies, dataSet, serverType);
        }
    }

    static boolean loadFeature(List<LoadStrategy> loadStrategies, DataSet gFeature, DataProvider serverType) {
        if (!loadStrategies.contains(gFeature.getLoadStrategy())) {
            return false;
        }
        GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
        return true;
    }

    private static final Predicate<? super DataSet> isLoaded = GeneralLoadUtils::isLoaded;

    public synchronized void loadGenomeLoadModeDataSets() {
        List<DataSet> unreachableGenomeLoadDataSets = GeneralLoadUtils.getGenomeVersionDataSets().stream()
                .filter(dataSet -> dataSet.getLoadStrategy() == LoadStrategy.GENOME)
                .filter(isLoaded.negate())
                .filter(dataSet -> !LocalUrlCacher.isURIReachable(dataSet.getURI())).collect(Collectors.toList());
        if (!unreachableGenomeLoadDataSets.isEmpty()) {
            ModalUtils.errorPanel("The following data sets are required, but unreachable {}" + System.lineSeparator() + Joiner.on(System.lineSeparator()).join(unreachableGenomeLoadDataSets));
        }
        unreachableGenomeLoadDataSets.iterator().forEachRemaining(dataSet -> {
            DataContainer dataContainer = dataSet.getDataContainer();
            dataContainer.removeDataSet(dataSet);
            if (dataContainer.getDataSets().isEmpty()) {
                dataContainer.getDataProvider().setStatus(LoadUtils.ResourceStatus.NotResponding);
            }
        });
        GeneralLoadView.getLoadView().refreshTreeView();

        GeneralLoadUtils.getGenomeVersionDataSets().stream()
                .filter(dataSet -> dataSet.getLoadStrategy() == LoadStrategy.GENOME)
                .filter(isLoaded.negate())
                .forEach(dataSet -> {
                    Optional<InputStream> fileIs = Optional.empty();
                    Optional<InputStream> indexFileIs = Optional.empty();
                    try {
                        URL fileUrl = dataSet.getURI().toURL();
                        Optional<URI> indexFile = dataSet.getIndex();
                        if (remoteFileCacheService != null && BedUtils.isRemoteBedFile(fileUrl)) {
                            fileIs = remoteFileCacheService.getFilebyUrl(fileUrl, false);
                            if (indexFile.isPresent() && indexFile.get().toString().startsWith("http")) {
                                indexFileIs = remoteFileCacheService.getFilebyUrl(new URL(indexFile.get().toString()), false);
                            } else if (!indexFile.isPresent()) {
                                indexFileIs = remoteFileCacheService.getFilebyUrl(new URL(fileUrl.toString() + ".tbi"), false);
                            }
                        }

                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    } finally {
                        if (fileIs.isPresent()) {
                            try {
                                fileIs.get().close();
                            } catch (IOException ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                        if (indexFileIs.isPresent()) {
                            try {
                                indexFileIs.get().close();
                            } catch (IOException ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }

                    if (dataSet.getSymL() instanceof SymLoaderInst) {
                        GeneralLoadUtils.loadAllSymmetriesThread(dataSet);
                    } else {
                        GeneralLoadUtils.iterateSeqList(dataSet);
                    }
                });
    }

    public void useAsRefSequence(final DataSet feature) throws Exception {
        if (feature != null && feature.getSymL() != null) {
            final QuickLoadSymLoader quickload = (QuickLoadSymLoader) feature.getSymL();
            if (quickload.getSymLoader() instanceof ResidueTrackSymLoader) {

                final CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>(feature.getDataSetName()) {

                    @Override
                    protected Void runInBackground() {
                        try {
                            SymWithProps sym;
                            SeqSymmetry child;
                            SimpleSymWithResidues rchild;

                            for (BioSeq seq : feature.getSymL().getChromosomeList()) {
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
                            logger.error(ex.getMessage(), ex);
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

        ThreadUtils.runOnEventQueue(this::refreshTree);
    }

    private void refreshTree() {
        final List<DataSet> features = GeneralLoadUtils.getGenomeVersionDataSets();
        if (features == null || features.isEmpty()) {
            tableModel.clearFeatures();
        }
        feature_tree_view.initOrRefreshTree(features);
    }

    public void refreshTreeViewAndRestore() {
        ThreadUtils.runOnEventQueue(() -> {
            String state = feature_tree_view.getState();
            refreshTree();
            feature_tree_view.restoreState(state);
        });
    }

    private static void refreshDataManagementTable(final List<DataSet> visibleFeatures) {

        ThreadUtils.runOnEventQueue(() -> {
            table.stopCellEditing();
            tableModel.generateFeature2StyleReference(visibleFeatures);
            DataManagementTable.setComboBoxEditors(table, !GeneralLoadView.IsGenomeSequence());
        });
    }

    public void refreshDataManagementView() {
        final List<DataSet> visibleFeatures = GeneralLoadUtils.getVisibleFeatures();
        refreshDataManagementTable(visibleFeatures);

        disableButtonsIfNecessary();
        changeVisibleDataButtonIfNecessary(visibleFeatures);
    }

    private void initDataManagementTable() {
        final List<DataSet> visibleFeatures = GeneralLoadUtils.getVisibleFeatures();
        int maxFeatureNameLength = 1;
        for (DataSet feature : visibleFeatures) {
            maxFeatureNameLength = Math.max(maxFeatureNameLength, feature.getDataSetName().length());
        }
        final int finalMaxFeatureNameLength = maxFeatureNameLength;	// necessary for threading
        table.stopCellEditing();
        tableModel.generateFeature2StyleReference(visibleFeatures);

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
            BioSeq curSeq = gmodel.getSelectedSeq().orElse(null);
            enabled = curSeq.getGenomeVersion() != null;	// Don't allow a null sequence group either.
            if (enabled) {		// Don't allow buttons for an "unknown" versionName
                Set<DataContainer> gVersions = curSeq.getGenomeVersion().getAvailableDataContainers();
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

    private void setAllButtons(final boolean enabled) {
        ThreadUtils.runOnEventQueue(() -> {
            partial_residuesB.setEnabled(enabled);
            refreshDataAction.setEnabled(enabled);
        });
    }

    /**
     * Accessor method. See if we need to enable/disable the refresh_dataB button by looking at the features' load
     * strategies.
     */
    void changeVisibleDataButtonIfNecessary(List<DataSet> features) {
        if (IsGenomeSequence()) {
            return;
            // Currently not enabling this button for the full sequence.
        }
        boolean enabled = false;
        for (DataSet gFeature : features) {
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
        BioSeq curSeq = gmodel.getSelectedSeq().orElse(null);
        final String seqID = curSeq == null ? null : curSeq.getId();
        return (seqID == null || IGBConstants.GENOME_SEQ_ID.equals(seqID));
    }

    public String getSelectedSpecies() {
        return (String) SeqGroupView.getInstance().getSpeciesCB().getSelectedItem();
    }

    public DataSet createDataSet(URI uri, String dataSetName, SymLoader loader) {
        if (uri == null) {
            try {
                uri = new URI(IGBStateProvider.getUniqueName("file:/" + removeIllegalCharacters(dataSetName)));
            } catch (URISyntaxException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        DataContainer dataContainer = GeneralLoadUtils.getLocalFileDataContainer(GenometryModel.getInstance().getSelectedGenomeVersion(), getSelectedSpecies());
        DataSet dataSet = new DataSet(uri, dataSetName, null, dataContainer, loader, false);
        dataContainer.addDataSet(dataSet);
        dataSet.setVisible(); // this should be automatically checked in the feature tree
        refreshTreeViewAndRestore();
        refreshDataManagementView();

        return dataSet;
    }

    private static String removeIllegalCharacters(String string) {
        string = string.replaceAll("\\s+", "_");
        string = string.replaceAll("\\|", "_");
        string = string.replaceAll("\u221E", "infinite");
        string = string.replaceAll("\\[", "(");
        string = string.replaceAll("\\]", ")");
        return string;
    }

    public void addFeature(final DataSet dataSet) {
        dataSet.setVisible();

        List<LoadStrategy> loadStrategies = new java.util.ArrayList<>();
        loadStrategies.add(LoadStrategy.GENOME);

        if (!loadFeature(loadStrategies, dataSet, null)) {
            addFeatureTier(dataSet);
        }

        refreshDataManagementView();
    }

    public static void addFeatureTier(final DataSet feature) {

        CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>(LOADING_MESSAGE_PREFIX + feature.getDataSetName(), Thread.MIN_PRIORITY) {

            @Override
            protected Object runInBackground() {
                TrackView.getInstance().addEmptyTierFor(feature, gviewer);
                return null;
            }

            @Override
            protected void finished() {
                AbstractAction action = new AbstractAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
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

    public void removeAllDataSets(Collection<DataSet> dataSets) {
        int i = 0;
        for (DataSet dataSet : dataSets) {
            if (i < dataSets.size() - 1) {
                removeDataSet(dataSet, false);
                removeTier(dataSet.getURI().toString());
                if (!Strings.isNullOrEmpty(dataSet.getMethod())) {
                    removeTier(dataSet.getMethod());
                }
            } else {
                removeDataSet(dataSet, true);
            }
            i++;
        }
    }

    public void removeDataSet(final DataSet feature, final boolean refresh) {
        removeDataSet(feature, refresh, true);
    }

    public void clearTrack(final ITrackStyleExtended style) {
        final String method = style.getMethodName();
        if (method != null) {
            final BioSeq bioseq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
            final DataSet feature = style.getFeature();

            // If genome is selected then delete all syms on the all seqs.
            if (IGBConstants.GENOME_SEQ_ID.equals(bioseq.getId())) {
                removeDataSet(feature, true);
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
                    TierGlyph tier = TrackView.getInstance().getTier(style, StyledGlyph.Direction.FORWARD);
                    if (tier != null) {
                        tier.removeAllChildren();
                        tier.setInfo(null);
                    }
                    tier = TrackView.getInstance().getTier(style, StyledGlyph.Direction.REVERSE);
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

    void removeDataSet(final DataSet feature, final boolean refresh, final boolean removeLocal) {
        if (feature == null) {
            return;
        }

        CThreadWorker<Void, Void> delete = new CThreadWorker<Void, Void>("Removing feature  " + feature.getDataSetName()) {

            @Override
            protected Void runInBackground() {
                if (!Strings.isNullOrEmpty(feature.getMethod())) {
                    for (BioSeq bioseq : feature.getDataContainer().getGenomeVersion().getSeqList()) {
                        TrackView.getInstance().deleteSymsOnSeq(gviewer, feature.getMethod(), bioseq, feature);
                    }
                }
                return null;
            }

            @Override
            protected void finished() {
                DataContainer dataContainer = feature.getDataContainer();
                if (dataContainer.getDataProvider() instanceof LocalDataProvider) {
                    if (dataContainer.removeDataSet(feature)) {
                        SeqGroupView.getInstance().refreshTable();
                        if (gmodel.getSelectedGenomeVersion().getSeqList() != null
                                && !gmodel.getSelectedGenomeVersion().getSeqList().contains(gmodel.getSelectedSeq().get())) {
                            gmodel.setSelectedSeq(gmodel.getSelectedGenomeVersion().getSeqList().get(0));
                        }
                    }
                }

                if (refresh) {
                    removeTier(feature.getURI().toString());
                    if (!Strings.isNullOrEmpty(feature.getMethod())) {
                        removeTier(feature.getMethod());
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

        };

        CThreadHolder.getInstance().execute(feature, delete);

    }

    private void removeTier(String method) {
        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method);
        TierGlyph tier = TrackView.getInstance().getTier(style, StyledGlyph.Direction.FORWARD);
        if (tier != null) {
            gviewer.getSeqMap().removeTier(tier);
        }
        tier = TrackView.getInstance().getTier(style, StyledGlyph.Direction.REVERSE);
        if (tier != null) {
            gviewer.getSeqMap().removeTier(tier);
        }

        if (style.isGraphTier()) {
            DefaultStateProvider.getGlobalStateProvider().removeGraphState(method);
        } else {
            DefaultStateProvider.getGlobalStateProvider().removeAnnotStyle(method);
        }
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
