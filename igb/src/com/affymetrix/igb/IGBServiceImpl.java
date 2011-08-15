/**
 *   Copyright (c) 2010 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.event.RepositoryChangeListener;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.operator.graph.GraphOperator;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.SeqMapViewPopup;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.tiers.TransformTierGlyph;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.IGBUtils;
import com.affymetrix.igb.util.ScriptFileLoader;
import com.affymetrix.igb.util.UnibrowControlServlet;
import com.affymetrix.igb.view.MapRangeBox;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;

/**
 * implementation of the IGBService, using the IGB instance for
 * all of the methods. This is the way for bundles to access
 * IGB functionality that is not public.
 *
 */
public class IGBServiceImpl implements IGBService, BundleActivator, RepositoryChangeListener {

	private static IGBServiceImpl instance = new IGBServiceImpl();
	public static IGBServiceImpl getInstance() {
		return instance;
	}
	private List<RepositoryChangeListener> repositoryChangeListeners;

	private IGBServiceImpl() {
		super();
		repositoryChangeListeners = new ArrayList<RepositoryChangeListener>();
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
	public ImageIcon getIcon(String name) {
		return IGBUtils.getIcon(name);
	}

	@Override
	public void addStopRoutine(IStopRoutine routine) {
		IGB igb = (IGB)IGB.getSingleton();
		igb.addStopRoutine(routine);
	}

	@Override
	public JRPMenu getFileMenu() {
		IGB igb = (IGB)IGB.getSingleton();
		return igb.getFileMenu();
	}

	@Override
	public JRPMenu getViewMenu() {
		IGB igb = (IGB)IGB.getSingleton();
		return igb.getViewMenu();
	}

	@Override
	public List<String> getRepositories() {
		List<String> repositories = new ArrayList<String>();
		for (GenericServer repositoryServer : ServerList.getRepositoryInstance().getAllServers()) {
			if (repositoryServer.isEnabled()) {
				repositories.add(repositoryServer.URL);
			}
		}
		return repositories;
	}

	@Override
	public void failRepository(String url) {
		ServerList.getRepositoryInstance().removeServer(url);
	}

	@Override
	public void displayRepositoryPreferences() {
		if (IGB.TAB_PLUGIN_PREFS != -1) {
			PreferencesPanel pv = PreferencesPanel.getSingleton();
			pv.setTab(IGB.TAB_PLUGIN_PREFS);	// Repository preferences tab
			JFrame f = pv.getFrame();
			f.setVisible(true);
		} else {
			System.out.println("Plugin Repository Preferences not instantiated");
		}
	}

	@Override
	public void addRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener) {
		repositoryChangeListeners.add(repositoryChangeListener);
	}

	@Override
	public void removeRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener) {
		repositoryChangeListeners.remove(repositoryChangeListener);
	}


	@Override
	public boolean repositoryAdded(String url) {
		boolean addedOK = true;
		for (RepositoryChangeListener repositoryChangeListener : repositoryChangeListeners) {
			addedOK &= repositoryChangeListener.repositoryAdded(url);
		}
		return addedOK;
	}

	@Override
	public void repositoryRemoved(String url) {
		for (RepositoryChangeListener repositoryChangeListener : repositoryChangeListeners) {
			repositoryChangeListener.repositoryRemoved(url);
		}
	}

	@Override
	public void loadAndDisplaySpan(SeqSpan span, GenericFeature feature) {
		GeneralLoadUtils.loadAndDisplaySpan(span, feature);
	}

	@Override
	public void updateGeneralLoadView() {
		GeneralLoadView.getLoadView().refreshTreeView();
		GeneralLoadView.getLoadView().createFeaturesTable();
	}

	@Override
	public void updateDependentData() {
		TrackView.updateDependentData();
	}

	@Override
	public void doActions(String batchFileStr) {
		ScriptFileLoader.runScript(batchFileStr);
	}

	@Override
	public void doSingleAction(String line) {
		ScriptFileLoader.doSingleAction(line);
	}

	@Override
	public void performSelection(String selectParam) {
		UnibrowControlServlet.getInstance().performSelection(selectParam);
	}

	@Override
	public void loadResidues(int start, int end) {
		UnibrowControlServlet.getInstance().loadResidues(start, end);
	}

	@Override
	public GenericFeature getFeature(GenericServer gServer, String feature_url) {
		return UnibrowControlServlet.getInstance().getFeature(gServer, feature_url);
	}

	@Override
	public GenericServer loadServer(String server_url) {
		return UnibrowControlServlet.getInstance().loadServer(server_url);
	}

	@Override
	public AnnotatedSeqGroup determineAndSetGroup(final String version) {
		return UnibrowControlServlet.getInstance().determineAndSetGroup(version);
	}

	@Override
	public Color getDefaultBackgroundColor() {
		return TrackStyle.getDefaultInstance().getBackground();
	}

	@Override
	public Color getDefaultForegroundColor() {
		return TrackStyle.getDefaultInstance().getForeground();
	}

	@Override
	public void setTabStateAndMenu(IGBTabPanel igbTabPanel, TabState tabState) {
		((IGB)IGB.getSingleton()).setTabStateAndMenu(igbTabPanel, tabState);
	}

	@Override
	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor) {
		return ((IGB)IGB.getSingleton()).searchForRegexInResidues(
				forward, regex, residues, residue_offset, Application.getSingleton().getMapView().getAxisTier(), glyphs, hitColor);
	}

	@Override
	public void zoomToCoord(String seqID, int start, int end) {
		MapRangeBox.zoomToSeqAndSpan(((SeqMapView)getSeqMapView()), seqID, start, end);
	}

	@Override
	public void mapRefresh(List<GlyphI> glyphs) {
		TransformTierGlyph axis_tier = ((SeqMapView)getSeqMapView()).getAxisTier();
		for(GlyphI glyph : glyphs){
			axis_tier.addChild(glyph);
		}
	}

	@Override
	public NeoAbstractWidget getSeqMap() {
		return Application.getSingleton().getMapView().getSeqMap();
	}

	@Override
	public SeqMapViewI getSeqMapView() {
		return Application.getSingleton().getMapView();
	}

	@Override
	public String getCommandLineBatchFileStr() {
		return IGB.commandLineBatchFileStr;
	}

	@Override
	public void setCommandLineBatchFileStr(String str) {
		IGB.commandLineBatchFileStr = str;
	}

	@Override
	public String getGenomeSeqId() {
		return IGBConstants.GENOME_SEQ_ID;
	}

	@Override
	public boolean loadResidues(final SeqSpan viewspan, final boolean partial) {
		return GeneralLoadView.getLoadView().loadResidues(viewspan, partial);
	}

	@Override
	public JFrame getFrame() {
		return Application.getSingleton().getFrame();
	}

	@Override
	public void saveState() {
		((IGB)IGB.getSingleton()).getWindowService().saveState();
		((SeqMapView)getSeqMapView()).saveSession();
		for (IGBTabPanel panel : ((IGB)Application.getSingleton()).getTabs()) {
			panel.saveSession();
		}
	}

	@Override
	public void loadState() {
		((IGB)IGB.getSingleton()).getWindowService().restoreState();
		SeqMapView mapView = Application.getSingleton().getMapView();
		mapView.loadSession();
		for (IGBTabPanel panel : ((IGB)Application.getSingleton()).getTabs()) {
			panel.loadSession();
		}
	}

	@Override
	public IGBTabPanel getView(String viewName) {
		return ((IGB)IGB.getSingleton()).getView(viewName);
	}

	@Override
	public void selectTab(IGBTabPanel panel) {
		((IGB)IGB.getSingleton()).getWindowService().selectTab(panel);
	}

	@Override
	public void deleteGlyph(GlyphI glyph) {
		//TrackView.deleteTrack((TierGlyph) glyph);
		// see rev 8414 change to com.affymetrix.igb.tiers.SeqMapViewPopup.removeTiers()
	}

	@Override
	public void packMap(boolean fitx, boolean fity) {
		AffyTieredMap map = (AffyTieredMap)getSeqMap();
		map.packTiers(false, true, false);
		map.stretchToFit(fitx, fity);
		map.updateWidget();
	}

	@Override
	public View getView() {
		return ((AffyTieredMap)getSeqMap()).getView();
	}

	@Override
	public void setTrackStyle(String meth, Color col, String description) {
		TrackStyle annot_style = TrackStyle.getInstance(meth, false);
		annot_style.setForeground(col);
		annot_style.setGlyphDepth(1);
		annot_style.setTrackName(description);
		annot_style.setCollapsed(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean doOperateGraphs(GraphOperator operator, List<? extends GlyphI> graph_glyphs) {
		return GraphGlyphUtils.doOperateGraphs(operator, (List<GraphGlyph>)graph_glyphs, (SeqMapView)getSeqMapView());
	}

	@Override
	public List<Glyph> getAllTierGlyphs() {
		List<Glyph> allTierGlyphs = new ArrayList<Glyph>();
		for (TierLabelGlyph labelGlyph : ((SeqMapView)getSeqMapView()).getTierManager().getAllTierLabels()) {
			allTierGlyphs.add(labelGlyph.getReferenceTier());
		}
		return allTierGlyphs;
	}

	@Override
	public boolean areAllServersInited() {
		return ServerList.getServerInstance().areAllServersInited();
	}

	@Override
	public void addServerInitListener(GenericServerInitListener listener) {
		ServerList.getServerInstance().addServerInitListener(listener);
	}

	@Override
	public void removeServerInitListener(GenericServerInitListener listener) {
		ServerList.getServerInstance().removeServerInitListener(listener);
	}

	@Override
	public GenericServer getServer(String URLorName) {
		return ServerList.getServerInstance().getServer(URLorName);
	}
}
