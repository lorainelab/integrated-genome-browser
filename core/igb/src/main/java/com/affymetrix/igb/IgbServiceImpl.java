/**
 * Copyright (c) 2010 Affymetrix, Inc.
 *
 * <p>
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * <p>
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.data.DataProviderFactoryManager;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.general.DataProviderManager;
//import com.affymetrix.igb.prefs.AddDataProvider;
import com.affymetrix.igb.prefs.OtherOptionsView;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.shared.LoadResidueAction;
import com.affymetrix.igb.shared.TrackUtils;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.util.MenuBarManager;
import com.affymetrix.igb.util.ServiceUtils;
import com.affymetrix.igb.view.AltSpliceView;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.print.PrinterException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.event.ListSelectionListener;
import org.lorainelab.igb.genoviz.extensions.SeqMapViewI;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ExecutionException;


/**
 * implementation of the IgbService, using the IGB instance for all of the
 * methods. This is the way for bundles to access IGB functionality that is not
 * public.
 *
 */
public class IgbServiceImpl implements IgbService {

    private static IgbServiceImpl instance = new IgbServiceImpl();

    public static IgbServiceImpl getInstance() {
        return instance;
    }

    private MenuBarManager menuBarManager;
    private TreeMultimap<Integer, JMenu> parentMenuEntries;
    private DataProviderFactoryManager dataProviderFactoryManager;
    private DataProviderManager dataProviderManager;

    private IgbServiceImpl() {
        super();
        parentMenuEntries = TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
        initializeMenuBarManagerServiceTracker();
    }

    @Override
    public void addNotLockedUpMsg(String message) {
        IGB.getInstance().addNotLockedUpMsg(message);
    }

    @Override
    public void dismissPreferences() {
        PreferencesPanel.getSingleton().getFrame().setVisible(false);
    }

    @Override
    public void loadResidues(boolean partial) {
        GeneralLoadView.getLoadView().loadResidues(partial);
    }

    @Override
    public void removeNotLockedUpMsg(String message) {
        IGB.getInstance().removeNotLockedUpMsg(message);
    }

    @Override
    public void setStatus(final String message) {
        ThreadUtils.runOnEventQueue(() -> IGB.getInstance().setStatus(message));
    }

    @Override
    public ImageIcon getIcon(String name) {
        return CommonUtils.getInstance().getIcon("images/" + name);
    }

    @Override
    public void loadAndDisplayAnnotations(DataSet gFeature) {
        GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
    }

    @Override
    public void loadAndDisplaySpan(SeqSpan span, DataSet feature) {
        GeneralLoadUtils.loadAndDisplaySpan(span, feature);
    }

    @Override
    public void loadChromosomes(DataSet gFeature) {
        GeneralLoadUtils.addDataSet(gFeature);
    }

    @Override
    public void updateGeneralLoadView() {
        GeneralLoadView.getLoadView().refreshTreeViewAndRestore();
        GeneralLoadView.getLoadView().refreshDataManagementView();
    }

    @Override
    public void doActions(String batchFileStr) {
        ScriptManager.getInstance().runScript(batchFileStr);
    }

    @Override
    public void runScriptString(String line, String ext) {
        ScriptManager.getInstance().runScriptString(line, ext);
    }

    @Override
    public void performSelection(String selectParam) {
        ServiceUtils.getInstance().performSelection(selectParam);
    }

    @Override
    public Optional<DataSet> getDataSet(GenomeVersion genomeVersion, DataProvider gServer, String feature_url, boolean showErrorForUnsupported) {
        try {
            URI uri = new URI(feature_url);
            return genomeVersion.getAvailableDataContainers().stream()
                    .filter(dc -> dc.getDataProvider() == gServer)
                    .flatMap(dc -> dc.getDataSets().stream())
                    .filter(ds -> ds.getURI().toString().equals(uri.toString()))
                    .findFirst();
        } catch (URISyntaxException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<GenomeVersion> determineAndSetGroup(final String version) {
        return ServiceUtils.getInstance().determineAndSetGroup(version);
    }

    @Override
    public Color getDefaultBackgroundColor() {
        return IGBStateProvider.getDefaultTrackStyle().getBackground();
    }

    @Override
    public Color getDefaultForegroundColor() {
        return IGBStateProvider.getDefaultTrackStyle().getForeground();
    }

    @Override
    public void zoomToCoord(String seqID, int start, int end) {
        ((SeqMapView) getSeqMapView()).getMapRangeBox().zoomToSeqAndSpan(((SeqMapView) getSeqMapView()), seqID, start, end);
    }

    @Override
    public void mapRefresh(List<GlyphI> glyphs) {
        GlyphI axis_tier = ((SeqMapView) getSeqMapView()).getAxisTier();
        new CopyOnWriteArrayList<>(glyphs).forEach(axis_tier::addChild);
    }

    @Override
    public NeoAbstractWidget getSeqMap() {
        return IGB.getInstance().getMapView().getSeqMap();
    }

    @Override
    public void addListSelectionListener(ListSelectionListener listener) {
        AffyLabelledTierMap map = (AffyLabelledTierMap) getSeqMap();
        map.addListSelectionListener(listener);
    }

    @Override
    public void removeListSelectionListener(ListSelectionListener listener) {
        AffyLabelledTierMap map = (AffyLabelledTierMap) getSeqMap();
        map.removeListSelectionListener(listener);
    }

    @Override
    public SeqMapViewI getSeqMapView() {
        return IGB.getInstance().getMapView();
    }

    @Override
    public boolean loadResidues(final SeqSpan viewspan, final boolean partial) {
        return GeneralLoadView.getLoadView().loadResidues(viewspan, partial);
    }

    @Override
    public GenericAction loadResidueAction(final SeqSpan viewspan, final boolean partial) {
        return new LoadResidueAction(viewspan, partial);
    }

    @Override
    public JFrame getApplicationFrame() {
        return IGB.getInstance().getFrame();
    }

    @Override
    public Component getMainViewComponent() {
        AffyLabelledTierMap map = (AffyLabelledTierMap) getSeqMap();
        return map.getNeoCanvas();
    }

    @Override
    public Component getMainViewComponentWithLabels() {
        AffyLabelledTierMap map = (AffyLabelledTierMap) getSeqMap();
        return map.getSplitPane();
    }

    @Override
    public Component getSpliceViewComponent() {
        AltSpliceView slice_view = AltSpliceView.getSingleton();
        return ((AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap());
    }

    @Override
    public Component getSpliceViewComponentWithLabels() {
        AltSpliceView slice_view = AltSpliceView.getSingleton();
        return ((AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap()).getSplitPane();
    }

    @Override
    public void saveState() {
        (IGB.getInstance()).getWindowService().saveState();
        ((SeqMapView) getSeqMapView()).saveSession();
        ((IGB) IGB.getInstance()).getTabs().forEach(org.lorainelab.igb.services.window.tabs.IgbTabPanel::saveSession);
    }

    @Override
    public void loadState() {
        (IGB.getInstance()).getWindowService().restoreState();
        SeqMapView mapView = IGB.getInstance().getMapView();
        mapView.loadSession();
        ((IGB) IGB.getInstance()).getTabs().forEach(org.lorainelab.igb.services.window.tabs.IgbTabPanel::loadSession);
    }

    @Override
    public IgbTabPanel getTabPanel(String viewName) {
        return (IGB.getInstance()).getView(viewName);
    }

    //Easier for scripting if we don't require full name.
    @Override
    public IgbTabPanel getTabPanelFromDisplayName(String viewName) {
        return (IGB.getInstance()).getViewByDisplayName(viewName);
    }

    @Override
    public void selectTab(IgbTabPanel panel) {
        (IGB.getInstance()).getWindowService().selectTab(panel);
    }

    @Override
    public void packMap(boolean fitx, boolean fity) {
        AffyTieredMap map = (AffyTieredMap) getSeqMap();
        map.packTiers(false, false, false);
        map.stretchToFit(fitx, fity);
        map.updateWidget();
    }

    @Override
    public View getView() {
        return ((AffyTieredMap) getSeqMap()).getView();
    }

    @Override
    public List<TierGlyph> getAllTierGlyphs() {
        return ((SeqMapView) getSeqMapView()).getTierManager().getAllTierGlyphs(false);
    }

    @Override
    public List<TierGlyph> getSelectedTierGlyphs() {
        return ((SeqMapView) getSeqMapView()).getTierManager().getSelectedTiers();
    }

    @Override
    public List<TierGlyph> getVisibleTierGlyphs() {
        return ((SeqMapView) getSeqMapView()).getTierManager().getVisibleTierGlyphs();
    }

    @Override
    public java.util.Optional<DataProvider> loadServer(String server_url) {
        return DataProviderManager.getServerFromUrlStatic(server_url);
    }

    @Override
    public boolean areAllServersInited() {
        return DataProviderManager.ALL_SOURCES_INITIALIZED;
    }

    @Override
    public Optional<DataProvider> getServer(String url) {
        return DataProviderManager.getServerFromUrlStatic(url);

    }

    @Override
    public void openURI(URI uri, String fileName, GenomeVersion loadGroup, String speciesName, boolean isReferenceSequence) {
        GeneralLoadUtils.openURI(uri, Optional.empty(), fileName, loadGroup, speciesName, isReferenceSequence);
    }

    @Override
    public String getSelectedSpecies() {
        return GeneralLoadView.getLoadView().getSelectedSpecies();
    }

    @Override
    public void addStyleSheet(String name, InputStream istr) {
        XmlStylesheetParser.addStyleSheet(name, istr);
    }

    @Override
    public void removeStyleSheet(String name) {
        XmlStylesheetParser.removeStyleSheet(name);
    }

    @Override
    public void addTrack(SeqSymmetry sym, String method) {
        TrackUtils.getInstance().addTrack(sym, method, null, null);
    }

    @Override
    public void addSpeciesItemListener(ItemListener il) {
        SeqGroupView.getInstance().getSpeciesCB().addItemListener(il);
    }

    @Override
    public void addPartialResiduesActionListener(ActionListener al) {
        ((SeqMapView) getSeqMapView()).getPartial_residuesButton().addActionListener(al);
    }

    @Override
    public Set<DataProvider> getEnabledServerList() {
        return DataProviderManager.getEnabledServers();
    }

    @Override
    public Collection<DataProvider> getAllServersList() {
        return DataProviderManager.getAllServers();
    }

    @Override
    public void goToRegion(String region) {
        ((SeqMapView) getSeqMapView()).getMapRangeBox().setRange(region);
    }

    @Override
    public DataSet findFeatureWithURI(DataContainer version, URI featureURI) {
        return GeneralUtils.findFeatureWithURI(version.getDataSets(), featureURI);
    }

    @Override
    public void print(int pageFormat, boolean noDialog) throws PrinterException {
        IGB.getInstance().getMapView().getSeqMap().print(0, true);
    }

    @Override
    public void refreshDataManagementView() {
        GeneralLoadView.getLoadView().refreshDataManagementView();
    }

    @Override
    public void loadVisibleFeatures() {
        GeneralLoadView.getLoadView().loadVisibleFeatures();
    }

    @Override
    public void selectFeatureAndCenterZoomStripe(String selectParam) {
        ServiceUtils.getInstance().selectFeatureAndCenterZoomStripe(selectParam);
    }

    @Override
    public void openPreferencesOtherPanel() {
        PreferencesPanel pv = PreferencesPanel.getSingleton();
        pv.setTab(OtherOptionsView.class);    // Other preferences tab
        JFrame f = pv.getFrame();
        f.setState(JFrame.NORMAL);
        f.setVisible(true);
    }

    @Override
    public void addDataSourcesEndpoint(String url, String name) {
        addDataSourcesEndpoint(url, name, null);
    }
    /**
     * Adds Quickload data source in IGB Preferences.
     * @param url : the quickloadurl provided by the user or converted UCSC trackhub url from the trackhub page
     * @param name : the quickloadname(shortLabel) provided by the user or from the hub.txt file
     */
    @Override
    public void addDataSourcesEndpoint(String url, String name, String selectGenomeVersion) {
        org.slf4j.Logger LOG = LoggerFactory.getLogger(IgbServiceImpl.class);
        BundleContext bundleContext = FrameworkUtil.getBundle(IgbServiceImpl.class).getBundleContext();
        ServiceReference serviceRef = bundleContext.getServiceReference(DataProviderManager.class.getName());
        dataProviderManager = (DataProviderManager) bundleContext.getService(serviceRef);
        serviceRef = bundleContext.getServiceReference(DataProviderFactoryManager.class.getName());
        dataProviderFactoryManager = (DataProviderFactoryManager) bundleContext.getService(serviceRef);
        CThreadWorker<Boolean, Void> worker;
        worker = new CThreadWorker<Boolean, Void>("Adding Data Source: " + name.trim()) {
            boolean isUnavailable = false;
            @Override
            protected Boolean runInBackground() {
                if (!Strings.isNullOrEmpty(url) || !Strings.isNullOrEmpty(name)) {
                    Optional<DataProviderFactory> factory = dataProviderFactoryManager.findFactoryByName("Quickload");
                    if (factory.isPresent()) {
                        DataProvider createdDataProvider = factory.get().createDataProvider(url, name, -1);
                        dataProviderManager.addDataProvider(createdDataProvider);
                        if (createdDataProvider.getStatus() == LoadUtils.ResourceStatus.NotResponding) {
                            isUnavailable = true;
                        }
                    }
                }
                return true;
            }

            @Override
            protected void finished() {
                boolean serverAdded = true;
                try {
                    serverAdded = get();
                } catch (InterruptedException | ExecutionException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
                GenometryModel gmodel = GenometryModel.getInstance();
                GenomeVersion genomeVersion = gmodel.getSelectedGenomeVersion();
                if(selectGenomeVersion == null)
                {
                    if (serverAdded && genomeVersion != null) {
                        ModalUtils.infoPanel("<html>Your data source <b>" + name.trim() + "</b> is now available in <b>Data Access Tab</b> under <b>Available Data</b>.</html>", "", false);
                    }
                    if (isUnavailable) {
                        ModalUtils.infoPanel("Your newly added Data Source is not responding, please confirm you have entered everything correctly.");
                    }
                }
                else {
                    if(serverAdded){
                        ModalUtils.infoPanel("<html>Your data source <b>" + name.trim() + "</b> has been added as a <b>Quickload</b>, please wait while the genome loads.</html>", "", false);
                        SeqGroupView.getInstance().setSelectedGenomeVersion(selectGenomeVersion);
                    }
                    if (isUnavailable) {
                        ModalUtils.infoPanel("External Data Provider is not responding, please try again later.");
                    }
                }
            }
        };
        CThreadHolder.getInstance().execute(new Object(),worker);

    }

    @Override
    public float getDefaultTrackSize() {
        return TrackConstants.default_track_name_size;
    }

    @Override
    public void deselect(GlyphI tierGlyph) {
        IGB.getInstance().getMapView().getTierManager().deselect(tierGlyph);
    }

    @Override
    public void setHome() {
        SeqGroupView.getInstance().getSpeciesCB().setSelectedItem(SeqGroupView.SELECT_SPECIES);
    }

    @Override
    public DataSet createFeature(String featureName, SymLoader loader) {
        return GeneralLoadView.getLoadView().createDataSet(null, featureName, loader);
    }

    @Override
    public void bringToFront() {
        JFrame f = IGB.getInstance().getFrame();
        boolean tmp = f.isAlwaysOnTop();
        f.setAlwaysOnTop(true);
        f.toFront();
        f.requestFocus();
        f.repaint();
        f.setAlwaysOnTop(tmp);
    }

    @Override
    public void openPreferencesPanelTab(Class<? extends PreferencesPanelProvider> cls) {
        PreferencesPanel pv = PreferencesPanel.getSingleton();
        pv.setTab(cls);
        JFrame f = pv.getFrame();
        f.setState(JFrame.NORMAL);
        f.setVisible(true);
    }

    @Override
    public int getPreferencesPanelTabIndex(Component c) {
        PreferencesPanel pv = PreferencesPanel.getSingleton();
        return pv.getTabIndex(c);
    }

    @Override
    public List<String> getLoadedFeatureNames() {
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        for (DataSet vFeature : GeneralLoadView.getLoadView().getTableModel().features) {
            builder.add(vFeature.getDataSetName());
        }
        return builder.build();
    }

    @Override
    public int addToolbarAction(GenericAction genericAction) {
        IGB igb = IGB.getInstance();
        return igb.addToolbarAction(genericAction);
    }

    @Override
    public void removeToolbarAction(GenericAction action) {
        IGB igb = IGB.getInstance();
        igb.removeToolbarAction(action);
    }

    @Override
    public void deleteAllTracks() {
        GeneralLoadView.getLoadView().removeAllDataSets(GeneralLoadUtils.getVisibleFeatures());
    }

    @Override
    public void deleteTrack(URI uri) {
        GeneralLoadUtils.findFeatureFromUri(uri)
                .ifPresent(featureToRemove -> {
                    GeneralLoadView.getLoadView().removeDataSet(featureToRemove, true);
                });
    }

    @Override
    public void addParentMenuBarEntry(JMenu parentMenu, int weight) {
        if (menuBarManager == null) {
            parentMenuEntries.put(weight, parentMenu);
        } else {
            menuBarManager.addParentMenuEntry(parentMenu, weight);
        }
    }

    private void initializeMenuBarManagerServiceTracker() {
        BundleContext bundleContext = FrameworkUtil.getBundle(IgbServiceImpl.class).getBundleContext();
        ServiceTracker<MenuBarManager, Object> dependencyTracker;
        dependencyTracker = new ServiceTracker<MenuBarManager, Object>(bundleContext, MenuBarManager.class, null) {

            @Override
            public Object addingService(ServiceReference<MenuBarManager> serviceReference) {
                menuBarManager = bundleContext.getService(serviceReference);
                parentMenuEntries.keySet().stream().forEach(key -> {
                    parentMenuEntries.get(key).forEach(entry -> {
                        menuBarManager.addParentMenuEntry(entry, key);
                    });
                });
                return super.addingService(serviceReference);
            }

        };
        dependencyTracker.open();
    }

    public List<String> getSpeciesList(){
        return GeneralLoadUtils.getSpeciesList();
    }

    public List<String> getAllVersions(String species){
        return SeqGroupView.getInstance().getAllVersions(species);
    }

    @Override
    public void writeCustomMsgToIGBConsoleTab(String Msg, String MsgType) {
        JTextArea consoleTab;
        Timestamp ts;


        ts = new Timestamp((new Date()).getTime());
        consoleTab = (JTextArea)((JViewport)((JScrollPane) (IGB.getInstance()).getViewByDisplayName("Console").getComponent(0)).getViewport()).getView();
        consoleTab.append("\n" + ts.toString().split("\\s+")[1] + " " + MsgType + " " + Msg);
    }


    @Override
    public ITrackStyleExtended getAnnotStyle(String unique_name) {
        return IGBStateProvider.getGlobalStateProvider().getAnnotStyle(unique_name);
    }
}
