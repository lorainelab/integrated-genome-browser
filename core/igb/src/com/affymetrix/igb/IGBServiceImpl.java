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
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.shared.LoadResidueAction;
import com.affymetrix.igb.general.RepositoryChangerHolder;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.RepositoryChangeHolderI;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.prefs.DataLoadPrefsView;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.shared.TrackUtils;
import com.affymetrix.igb.util.ExportDialog;
import com.affymetrix.igb.util.ServiceUtils;
import com.affymetrix.igb.view.AltSpliceView;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
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
 * implementation of the IGBService, using the IGB instance for all of the
 * methods. This is the way for bundles to access IGB functionality that is not
 * public.
 *
 */
public class IGBServiceImpl implements IGBService, BundleActivator {

	private static IGBServiceImpl instance = new IGBServiceImpl();

	public static IGBServiceImpl getInstance() {
		return instance;
	}

	private IGBServiceImpl() {
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
		Application.getSingleton().addNotLockedUpMsg(message);
	}

	@Override
	public void removeNotLockedUpMsg(String message) {
		Application.getSingleton().removeNotLockedUpMsg(message);
	}

	@Override
	public void setStatus(final String message) {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				Application.getSingleton().setStatus(message);
			}
		});
	}

	@Override
	public boolean confirmPanel(String text) {
		return Application.confirmPanel(text);
	}

	@Override
	public boolean confirmPanel(final String message, final String check, final boolean def_val) {
		return Application.confirmPanel(message, check, def_val);
	}
	
	@Override
	public void infoPanel(final String message, final String check, final boolean def_val){
		Application.infoPanel(message, check, def_val);
	}
	
	@Override
	public ImageIcon getIcon(String name) {
		return CommonUtils.getInstance().getIcon("images/" + name);
	}

	@Override
	public JRPMenu getMenu(String menuName) {
		IGB igb = (IGB) IGB.getSingleton();
		return igb.getMenu(menuName);
	}

	@Override
	public JRPMenu addTopMenu(String id, String text) {
		IGB igb = (IGB) IGB.getSingleton();
		return igb.addTopMenu(id, text);
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
	public AnnotatedSeqGroup determineAndSetGroup(final String version) {
		return ServiceUtils.getInstance().determineAndSetGroup(version);
	}

	@Override
	public Color getDefaultBackgroundColor() {
		return IGBStateProvider.getDefaultInstance().getBackground();
	}

	@Override
	public Color getDefaultForegroundColor() {
		return IGBStateProvider.getDefaultInstance().getForeground();
	}

	@Override
	public void zoomToCoord(String seqID, int start, int end) {
		((SeqMapView) getSeqMapView()).getMapRangeBox().zoomToSeqAndSpan(((SeqMapView) getSeqMapView()), seqID, start, end);
	}

	@Override
	public void mapRefresh(List<GlyphI> glyphs) {
		GlyphI axis_tier = ((SeqMapView) getSeqMapView()).getAxisTier();
		for (GlyphI glyph : new CopyOnWriteArrayList<GlyphI>(glyphs)) {
			axis_tier.addChild(glyph);
		}
	}

	@Override
	public NeoAbstractWidget getSeqMap() {
		return Application.getSingleton().getMapView().getSeqMap();
	}

	@Override
	public void addListSelectionListener(ListSelectionListener listener){
		AffyLabelledTierMap map = (AffyLabelledTierMap) getSeqMap();
		map.addListSelectionListener(listener);
	}
	
	@Override
	public void removeListSelectionListener(ListSelectionListener listener){
		AffyLabelledTierMap map = (AffyLabelledTierMap) getSeqMap();
		map.removeListSelectionListener(listener);
	}
	
	@Override
	public SeqMapViewI getSeqMapView() {
		return Application.getSingleton().getMapView();
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
	public JFrame getFrame() {
		return Application.getSingleton().getFrame();
	}

	@Override
	public void saveState() {
		((IGB) IGB.getSingleton()).getWindowService().saveState();
		((SeqMapView) getSeqMapView()).saveSession();
		for (IGBTabPanel panel : ((IGB) Application.getSingleton()).getTabs()) {
			panel.saveSession();
		}
	}

	@Override
	public void loadState() {
		((IGB) IGB.getSingleton()).getWindowService().restoreState();
		SeqMapView mapView = Application.getSingleton().getMapView();
		mapView.loadSession();
		for (IGBTabPanel panel : ((IGB) Application.getSingleton()).getTabs()) {
			panel.loadSession();
		}
	}

	@Override
	public IGBTabPanel getTabPanel(String viewName) {
		return ((IGB) IGB.getSingleton()).getView(viewName);
	}

	//Easier for scripting if we don't require full name.
	@Override
	public IGBTabPanel getTabPanelFromDisplayName(String viewName) {
		return ((IGB) IGB.getSingleton()).getViewByDisplayName(viewName);
	}

	@Override
	public void selectTab(IGBTabPanel panel) {
		((IGB) IGB.getSingleton()).getWindowService().selectTab(panel);
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

	@SuppressWarnings({"unchecked", "rawtypes", "cast"})
	@Override
	public List<Glyph> getAllTierGlyphs() {
		return (List<Glyph>) (List) ((SeqMapView) getSeqMapView()).getTierManager().getAllTierGlyphs();
	}

	@SuppressWarnings({"unchecked", "rawtypes", "cast"})
	@Override
	public List<Glyph> getSelectedTierGlyphs() {
		return (List<Glyph>) (List) ((SeqMapView) getSeqMapView()).getTierManager().getSelectedTiers();
	}

	@SuppressWarnings({"unchecked", "rawtypes", "cast"})
	@Override
	public List<Glyph> getVisibleTierGlyphs() {
		return (List<Glyph>) (List) ((SeqMapView) getSeqMapView()).getTierManager().getVisibleTierGlyphs();
	}

	@Override
	public RepositoryChangeHolderI getRepositoryChangerHolder() {
		return RepositoryChangerHolder.getInstance();
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
	public void openURI(URI uri, String fileName, AnnotatedSeqGroup loadGroup, String speciesName, boolean loadAsTrack) {
		GeneralLoadUtils.openURI(uri, fileName, loadGroup, speciesName, loadAsTrack);
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
		CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>("discover server " + server.serverName) {

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
		((SeqMapView)getSeqMapView()).getMapRangeBox().setRange(region);
	}

	@Override
	public GenericFeature findFeatureWithURI(GenericVersion version, URI featureURI) {
		return GeneralUtils.findFeatureWithURI(version.getFeatures(), featureURI);
	}

	@Override
	public void print(int pageFormat, boolean noDialog) throws PrinterException {
		Application.getSingleton().getMapView().getSeqMap().print(0, true);
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
	public float getDefaultTrackSize(){
		return TrackConstants.default_track_name_size;
	}

	@Override
	public void deselect(GlyphI tierGlyph) {
		Application.getSingleton().getMapView().getTierManager().deselect(tierGlyph);
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
		ServerList.getServerInstance().removeServer(gServer.URL);
		DataLoadPrefsView.getSingleton().refreshServers();
	}
	
	@Override
	public Component determineSlicedComponent() {
		AltSpliceView slice_view = (AltSpliceView) ((IGB) IGB.getSingleton()).getView(AltSpliceView.class.getName());
		if (slice_view == null) {
			return null;
		}

		return ((AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap()).getSplitPane();
	}
	
	@Override
	public void setComponent(Component c) {
		ExportDialog.getSingleton().setComponent(c);
	}
	
	@Override
	public void exportScreenshot (File f, String ext, boolean isScript) throws IOException {
		ExportDialog.getSingleton().exportScreenshot(f, ext, isScript);
	}
}
