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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.osgi.service.PropertyHandler;
import com.affymetrix.igb.osgi.service.RepositoryChangeListener;
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.util.ScriptFileLoader;
import com.affymetrix.igb.util.ThreadUtils;
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
	private FileTracker load_dir_tracker = FileTracker.DATA_DIR_TRACKER;
	private static final String APACHE_BUNDLE_VENDOR = "The Apache Software Foundation";
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
	public boolean addMenu(JMenu new_menu) {
		String menuName = new_menu.getName();
		JMenuBar main_menu_bar = MenuUtil.getMainMenuBar();
		int num_menus = main_menu_bar.getMenuCount();
	    for (int i=0; i<num_menus; i++) {
	      JMenu menu_i = main_menu_bar.getMenu(i);
	      if (menuName.equals(menu_i.getName())) {
	        menu_i.getName();
	        return false; // already a menu with this name
	      }
	    }

	    // Add the new menu, but keep the "Help" menu in last place
	    if (num_menus > 0 && "Help".equals(main_menu_bar.getMenu(num_menus-1).getName())) {
	    	main_menu_bar.add(new_menu, num_menus-1);
	    } else {
	    	main_menu_bar.add(new_menu);
	    }
	    main_menu_bar.validate();
	    return true;
	}

	@Override
	public boolean removeMenu(String menuName) {
		JMenuBar main_menu_bar = MenuUtil.getMainMenuBar();
		int num_menus = main_menu_bar.getMenuCount();
	    for (int i=0; i<num_menus; i++) {
	      JMenu menu_i = main_menu_bar.getMenu(i);
	      if (menuName.equals(menu_i.getName())) {
	    	main_menu_bar.remove(i);
	        return true;
	      }
	    }
	    return false; // not found
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
	public void setStatus(String message) {
		Application.getSingleton().setStatus(message);
	}

	@Override
	public boolean confirmPanel(String text) {
		return Application.confirmPanel(text);
	}

	@Override
	public ImageIcon getIcon(String name) {
		ImageIcon icon = null;
		java.net.URL imgURL = com.affymetrix.igb.IGB.class.getResource(name + "_icon.gif");
		if (imgURL != null) {
			icon = new ImageIcon(imgURL);
		}
		return icon;
	}

	@Override
	public void addStopRoutine(IStopRoutine routine) {
		IGB igb = (IGB)IGB.getSingleton();
		igb.addStopRoutine(routine);
	}

	@Override
	public int getTier(Bundle bundle) {
		if (bundle.getBundleId() == 0) { // system bundle
			return 0;
		}
		int tier = 3;
		String vendorString = ((String)bundle.getHeaders().get(Constants.BUNDLE_VENDOR));
		if (vendorString != null && vendorString.equals(APACHE_BUNDLE_VENDOR)) {
			tier = 1;
		}
		else {
			String tierString = ((String)bundle.getHeaders().get(IGBService.IGB_TIER_HEADER));
			if (tierString != null) {
				try {
					tier = Integer.parseInt(tierString.trim());
				}
				catch (Exception x) {}
			}
		}
		return tier;
	}

	@Override
	public JMenu getFileMenu() {
		IGB igb = (IGB)IGB.getSingleton();
		return igb.getFileMenu();
	}

	@Override
	public JMenu getViewMenu() {
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
	public String get_arg(String label, String[] args) {
		return IGB.get_arg(label, args);
	}

	@Override
	public String getAppName() {
		return IGBConstants.APP_NAME;
	}

	@Override
	public String getAppVersion() {
		return IGBConstants.APP_VERSION;
	}

	@Override
	public SeqSpan getVisibleSpan() {
		return ((SeqMapView)getMapView()).getVisibleSpan();
	}

	@Override
	public void setRegion(int start, int end, BioSeq seq) {
		((SeqMapView)getMapView()).setRegion(start, end, seq);
	}

	@Override
	public final BioSeq getAnnotatedSeq() {
		return ((SeqMapView)getMapView()).getAnnotatedSeq();
	}

	@Override
	public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view) {
		((SeqMapView)getMapView()).setAnnotatedSeq(seq, preserve_selection, preserve_view);
	}

	@Override
	public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view_x, boolean preserve_view_y) {
		((SeqMapView)getMapView()).setAnnotatedSeq(seq, preserve_selection, preserve_view_x, preserve_view_y);
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
		ScriptFileLoader.doActions(batchFileStr);
	}

	@Override
	public void doSingleAction(String line) {
		ScriptFileLoader.doSingleAction(line);
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
	public void updateWidget() {
		((SeqMapView)getMapView()).getSeqMap().updateWidget();
	}

	@Override
	public void removeItem(List<GlyphI> glyphs) {
		AffyTieredMap map = ((SeqMapView)getMapView()).getSeqMap();
		map.removeItem(glyphs);
	}

	@Override
	public BioSeq getViewSeq() {
		return ((SeqMapView)getMapView()).getViewSeq();
	}

	@Override
	public JComponent getMapView() {
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
	public void setPropertyHandler(PropertyHandler propertyHandler) {
		((SeqMapView)getMapView()).setPropertyHandler(propertyHandler);
	}

	@Override
	public JFrame getFrame() {
		return Application.getSingleton().getFrame();
	}

	@Override
	public void saveState() {
		((IGB)IGB.getSingleton()).getWindowService().saveState();
		((SeqMapView)getMapView()).saveSession();
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
	public File getLoadDirectory() {
		return load_dir_tracker.getFile();
	}

	@Override
	public void setLoadDirectory(File file) {
		load_dir_tracker.setFile(file);
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
	public Executor getPrimaryExecutor(Object key) {
		return ThreadUtils.getPrimaryExecutor(key);
	}

	@Override
	public void runOnEventQueue(Runnable r) {
		ThreadUtils.runOnEventQueue(r);
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
