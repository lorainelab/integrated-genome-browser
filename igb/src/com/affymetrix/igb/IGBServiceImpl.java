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
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.action.LoadResidueAction;
import com.affymetrix.igb.general.RepositoryChangerHolder;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.RepositoryChangeHolderI;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.TransformTierGlyph;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.util.ScriptFileLoader;
import com.affymetrix.igb.shared.TrackUtils;
import com.affymetrix.igb.util.UnibrowControlServlet;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.print.PrinterException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
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
	public boolean confirmPanel(final String message, final Preferences node,
			final String check, final boolean def_val) {
		return Application.confirmPanel(message, node, check, def_val);
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
	public void loadAndDisplaySpan(SeqSpan span, GenericFeature feature) {
		GeneralLoadUtils.loadAndDisplaySpan(span, feature);
	}

	@Override
	public void updateGeneralLoadView() {
		GeneralLoadView.getLoadView().refreshTreeView();
		GeneralLoadView.getLoadView().refreshDataManagementView();
	}

	@Override
	public void doActions(String batchFileStr) {
		ScriptFileLoader.getInstance().runScript(batchFileStr);
	}

	@Override
	public void doSingleAction(String line) {
		ScriptFileLoader.getInstance().doSingleAction(line);
	}

	@Override
	public void performSelection(String selectParam) {
		UnibrowControlServlet.getInstance().performSelection(selectParam);
	}

	@Override
	public GenericFeature getFeature(GenericServer gServer, String feature_url) {
		return UnibrowControlServlet.getInstance().getFeature(gServer, feature_url);
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
	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor) {
		return ((IGB) IGB.getSingleton()).searchForRegexInResidues(
				forward, regex, residues, residue_offset, Application.getSingleton().getMapView().getAxisTier(), glyphs, hitColor);
	}

	@Override
	public void zoomToCoord(String seqID, int start, int end) {
		((SeqMapView) getSeqMapView()).getMapRangeBox().zoomToSeqAndSpan(((SeqMapView) getSeqMapView()), seqID, start, end);
	}

	@Override
	public void mapRefresh(List<GlyphI> glyphs) {
		TransformTierGlyph axis_tier = ((SeqMapView) getSeqMapView()).getAxisTier();
		for (GlyphI glyph : glyphs) {
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
	public void deleteGlyph(GlyphI glyph) {
		List<TierLabelGlyph> tiers = new ArrayList<TierLabelGlyph>();
		for (TierLabelGlyph tierLabelGlyph : ((AffyLabelledTierMap) getSeqMap()).getTierLabels()) {
			if (tierLabelGlyph.getInfo() == glyph) {
				tiers.add(tierLabelGlyph);
			}
		}
		for (TierLabelGlyph tlg : tiers) {
			ITrackStyleExtended style = tlg.getReferenceTier().getAnnotStyle();
			String method = style.getMethodName();
			if (method != null) {
				TrackView.getInstance().delete((AffyTieredMap) getSeqMap(), method, style);
			} else {
				for (AbstractGraphGlyph gg : TierLabelManager.getContainedGraphs(tiers)) {
					style = gg.getGraphState().getTierStyle();
					method = style.getMethodName();
					TrackView.getInstance().delete((AffyTieredMap) getSeqMap(), method, style);
				}
			}
		}
		((SeqMapView) getSeqMapView()).dataRemoved();	// refresh
	}

	public void deleteGraph(GraphSym gsym) {
		TrackView.getInstance().delete((AffyTieredMap) getSeqMap(), gsym.getID(), gsym.getGraphState().getTierStyle());
		((SeqMapView) getSeqMapView()).dataRemoved();	// refresh
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
	public ITrackStyleExtended getTrackStyle(String meth) {
		return TrackStyle.getInstance(meth, false);
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
		return UnibrowControlServlet.getInstance().loadServer(server_url);
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
		TrackUtils.getInstance().addTrack(sym, method, null);
	}

	@Override
	public void addSpeciesItemListener(ItemListener il) {
		SeqGroupView.getInstance().getSpeciesCB().addItemListener(il);
	}

	@Override
	public void addPartialResiduesActionListener(ActionListener al) {
		GeneralLoadView.getLoadView().getPartial_residuesButton().addActionListener(al);
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
	public void changeViewMode(SeqMapViewI gviewer, ITrackStyleExtended style, String viewMode, RootSeqSymmetry rootSym, ITrackStyleExtended comboStyle) {
		TrackView.getInstance().changeViewMode((SeqMapView)gviewer, style, viewMode, rootSym, comboStyle);
	}

	@Override
	public void goToRegion(String region) {
		((SeqMapView)getSeqMapView()).getMapRangeBox().setRange((SeqMapView)getSeqMapView(), region);
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

	public void selectFeatureAndCenterZoomStripe(String selectParam) {
		UnibrowControlServlet.getInstance().selectFeatureAndCenterZoomStripe(selectParam);
	}
}
