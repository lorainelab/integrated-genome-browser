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
import java.awt.Component;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.action.UCSCViewAction;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.RepositoryChangeListener;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.view.PluginInfo;
import com.affymetrix.igb.view.SearchView;
import com.affymetrix.igb.view.SeqMapView;

public class IGBServiceImpl implements IGBService, BundleActivator, RepositoryChangeListener {

	private static IGBServiceImpl instance = new IGBServiceImpl();
	public static IGBServiceImpl getInstance() {
		return instance;
	}
	private List<RepositoryChangeListener> repositoryChangeListeners;
	private Set<String> tier1Bundles; // required bundles
	private Set<String> tier2Bundles; // optional bundles

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

	private HashMap<String, JComponent> addedPlugins = new HashMap<String, JComponent>();
	public void addPlugIn(JComponent plugIn, String tabName) {
		IGB.singleton_igb.loadPlugIn(new PluginInfo(plugIn.getClass().getName(), tabName, true), plugIn);
		addedPlugins.put(tabName, plugIn);
	}

	public boolean removePlugIn(String name) {
		if (name == null) {
			return false;
		}
		JTabbedPane tab_pane = IGB.singleton_igb.getTabPane() ;
		Map<Component, Frame> comp2window = IGB.singleton_igb.getComp2Window();
		JComponent plugIn = addedPlugins.get(name);
		Frame frame = comp2window.get(plugIn);
		if (frame == null) {
			for (int i = 0; i < tab_pane.getTabCount(); i++) {
				if (name.equals(tab_pane.getTitleAt(i))) {
					tab_pane.remove(i);
					return true;
				}
			}
		}
		else {
			frame.dispose();
			comp2window.remove(plugIn);
		}
		PreferenceUtils.saveComponentState(name, PreferenceUtils.COMPONENT_STATE_TAB); // default - can't delete state
		return false;
	}
/*
	private JComponent getView(String viewName) {
		Class<?> viewClass;
		try {
			viewClass = Class.forName(viewName);
		}
		catch (ClassNotFoundException x) {
			System.out.println("IGBServiceImpl.getView() failed for " + viewName);
			return null;
		}
		for (Object plugin : IGB.singleton_igb.getPlugins()) {
			if (viewClass.isAssignableFrom(plugin.getClass())) {
				return (JComponent)plugin;
			}
		}
		return null;
	}
*/
	public void displayError(String title, String errorText) {
		ErrorHandler.errorPanel(title, errorText);
	}

	public void displayError(String errorText) {
		ErrorHandler.errorPanel(errorText);
	}

	public void addNotLockedUpMsg(String message) {
		Application.getSingleton().addNotLockedUpMsg(message);
	}

	public void removeNotLockedUpMsg(String message) {
		Application.getSingleton().removeNotLockedUpMsg(message);
	}

	public Set<String> getTier1Bundles() {
		return tier1Bundles;
	}

	public Set<String> getTier2Bundles() {
		return tier2Bundles;
	}

	public List<String> getRepositories() {
		List<String> repositories = new ArrayList<String>();
		for (GenericServer repositoryServer : ServerList.getRepositoryInstance().getAllServers()) {
			if (repositoryServer.isEnabled()) {
				repositories.add(repositoryServer.URL);
			}
		}
		return repositories;
	}

	void setTier1Bundles(Set<String> _tier1Bundles) {
		tier1Bundles = _tier1Bundles;
	}

	void setTier2Bundles(Set<String> _tier2Bundles) {
		tier2Bundles = _tier2Bundles;
	}

	public void failRepository(String url) {
		ServerList.getRepositoryInstance().removeServer(url);
	}

	public void displayRepositoryPreferences() {
		if (OSGiHandler.TAB_PLUGIN_PREFS != -1) {
			PreferencesPanel pv = PreferencesPanel.getSingleton();
			pv.setTab(OSGiHandler.TAB_PLUGIN_PREFS);	// Repository preferences tab
			JFrame f = pv.getFrame();
			f.setVisible(true);
		} else {
			System.out.println("Plugin Repository Preferences not instantiated");
		}
	}

	public void addRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener) {
		repositoryChangeListeners.add(repositoryChangeListener);
	}

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

	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor) {
		return SearchView.searchForRegexInResidues(
				forward, regex, residues, residue_offset, Application.getSingleton().getMapView().getAxisTier(), glyphs, hitColor);
	}

	private SeqMapView getMapView() {
		return Application.getSingleton().getMapView();
	}

	private BioSeq getViewSeq() {
		return getMapView().getViewSeq();
	}

	public boolean isSeqResiduesAvailable() {
		BioSeq vseq = getViewSeq();
		return vseq != null && vseq.isComplete();
	}

	public int getSeqResiduesMin() {
		return getViewSeq().getMin();
	}

	public int getSeqResiduesMax() {
		return getViewSeq().getMax();
	}

	public String getSeqResidues() {
		return getViewSeq().getResidues();
	}

	public void updateMap() {
		Application.getSingleton().getMapView().getSeqMap().updateWidget();
	}

	public void removeGlyphs(List<GlyphI> glyphs) {
		Application.getSingleton().getMapView().getSeqMap().removeItem(glyphs);
	}

	public String getUCSCQuery() {
		return UCSCViewAction.getUCSCQuery();
	}
}
