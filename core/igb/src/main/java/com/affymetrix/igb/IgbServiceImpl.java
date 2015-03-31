/**
 * Copyright (c) 2010 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.prefs.DataLoadPrefsView;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.shared.LoadResidueAction;
import com.affymetrix.igb.shared.TrackUtils;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.util.ServiceUtils;
import com.affymetrix.igb.view.AltSpliceView;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.lorainelab.igb.genoviz.extensions.SeqMapViewI;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.print.PrinterException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.event.ListSelectionListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * implementation of the IgbService, using the IGB instance for all of the
 * methods. This is the way for bundles to access IGB functionality that is not
 * public.
 *
 */
public class IgbServiceImpl implements IgbService, BundleActivator {

    private static IgbServiceImpl instance = new IgbServiceImpl();

    public static IgbServiceImpl getInstance() {
        return instance;
    }

    private IgbServiceImpl() {
        super();
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
    }

    @Override
    public void addNotLockedUpMsg(String message) {
        IGB.getInstance().addNotLockedUpMsg(message);
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
    public JRPMenu addTopMenu(String id, String text, int index) {
        IGB igb = IGB.getInstance();
        return igb.addTopMenu(id, text, index);
    }

    @Override
    public void loadAndDisplayAnnotations(GenericFeature gFeature) {
        GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
    }

    @Override
    public void loadAndDisplaySpan(SeqSpan span, GenericFeature feature) {
        GeneralLoadUtils.loadAndDisplaySpan(span, feature);
    }

    @Override
    public void loadChromosomes(GenericFeature gFeature) {
        GeneralLoadUtils.addFeature(gFeature);
    }

    @Override
    public void updateGeneralLoadView() {
        GeneralLoadView.getLoadView().refreshTreeView();
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
    public GenericFeature getFeature(AnnotatedSeqGroup seqGroup, GenericServer gServer, String feature_url, boolean showErrorForUnsupported) {
        return ServiceUtils.getInstance().getFeature(seqGroup, gServer, feature_url, showErrorForUnsupported);
    }

    @Override
    public Optional<AnnotatedSeqGroup> determineAndSetGroup(final String version) {
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
        ((IGB) IGB.getInstance()).getTabs().forEach(com.lorainelab.igb.services.window.tabs.IgbTabPanel::saveSession);
    }

    @Override
    public void loadState() {
        (IGB.getInstance()).getWindowService().restoreState();
        SeqMapView mapView = IGB.getInstance().getMapView();
        mapView.loadSession();
        ((IGB) IGB.getInstance()).getTabs().forEach(com.lorainelab.igb.services.window.tabs.IgbTabPanel::loadSession);
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
        map.packTiers(false, true, false);
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
    public GenericServer loadServer(String server_url) {
        return ServiceUtils.getInstance().loadServer(server_url);
    }

    @Override
    public boolean areAllServersInited() {
        return ServerList.getServerInstance().areAllServersInited();
    }

    @Override
    public GenericServer getServer(String URLorName) {
        return ServerList.getServerInstance().getServer(URLorName);
    }

    @Override
    public void openURI(URI uri, String fileName, AnnotatedSeqGroup loadGroup, String speciesName, boolean isReferenceSequence) {
        GeneralLoadUtils.openURI(uri, fileName, loadGroup, speciesName, isReferenceSequence);
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
    public Set<GenericServer> getEnabledServerList() {
        return ServerList.getServerInstance().getEnabledServers();
    }

    @Override
    public Collection<GenericServer> getAllServersList() {
        return ServerList.getServerInstance().getAllServers();
    }

    @Override
    public void discoverServer(final GenericServer server) {
        CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>("discover server " + server.getServerName()) {

            @Override
            protected Void runInBackground() {
                GeneralLoadUtils.discoverServer(server);
                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(server, worker);
    }

    @Override
    public void goToRegion(String region) {
        ((SeqMapView) getSeqMapView()).getMapRangeBox().setRange(region);
    }

    @Override
    public GenericFeature findFeatureWithURI(GenericVersion version, URI featureURI) {
        return GeneralUtils.findFeatureWithURI(version.getFeatures(), featureURI);
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
        if (PreferencesPanel.TAB_OTHER_OPTIONS_VIEW != -1) {
            PreferencesPanel pv = PreferencesPanel.getSingleton();
            pv.setTab(PreferencesPanel.TAB_OTHER_OPTIONS_VIEW);	// Other preferences tab
            JFrame f = pv.getFrame();
            f.setVisible(true);
        } else {
            System.out.println("Other Preferences not instantiated");
        }
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
    public GenericServer addServer(ServerTypeI serverType,
            String serverName, String serverURL, int order) {
        GenericServer server = GeneralLoadUtils.addServer(ServerList.getServerInstance(), serverType,
                serverName, serverURL, order, false, null); // qlmirror
        DataLoadPrefsView.getSingleton().refreshServers();
        return server;
    }

    @Override
    public GenericServer addServer(ServerTypeI serverType,
            String serverName, String serverURL, int order, String mirrorURL) { // qlmirror
        GenericServer server = GeneralLoadUtils.addServer(ServerList.getServerInstance(), serverType,
                serverName, serverURL, order, false, mirrorURL);
        DataLoadPrefsView.getSingleton().refreshServers();
        return server;
    }

    @Override
    public void removeServer(GenericServer gServer) {
        ServerList.getServerInstance().removeServer(gServer.getURL());
        DataLoadPrefsView.getSingleton().refreshServers();
    }

    @Override
    public GenericFeature createFeature(String featureName, SymLoader loader) {
        return GeneralLoadView.getLoadView().createFeature(featureName, loader);
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
    public void openPreferencesPanelTab(int tabIndex) {
        PreferencesPanel pv = PreferencesPanel.getSingleton();
        pv.setTab(tabIndex);
        JFrame f = pv.getFrame();
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
        for (GenericFeature vFeature : GeneralLoadView.getLoadView().getTableModel().features) {
            builder.add(vFeature.featureName);
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
        GeneralLoadView.getLoadView().removeAllFeautres(GeneralLoadUtils.getVisibleFeatures());
    }

    @Override
    public void deleteTrack(URI uri) {
        GeneralLoadUtils.findFeatureFromUri(uri)
                .ifPresent(featureToRemove -> {
                    GeneralLoadView.getLoadView().removeFeature(featureToRemove, true);
                });
    }

}
