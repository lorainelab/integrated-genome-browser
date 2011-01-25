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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.action.UCSCViewAction;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.ExtensionPointRegistry;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.osgi.service.RepositoryChangeListener;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.view.SearchView;
import com.affymetrix.igb.view.SeqMapView;

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
	public void displayError(String title, String errorText) {
		ErrorHandler.errorPanel(title, errorText);
	}

	@Override
	public void displayError(String errorText) {
		ErrorHandler.errorPanel(errorText);
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
	public boolean confirmPanel(String text) {
		return Application.confirmPanel(text);
	}

	public ImageIcon getIcon(String name) {
		ImageIcon icon = null;
		java.net.URL imgURL = com.affymetrix.igb.IGB.class.getResource(name + "_icon.gif");
		if (imgURL != null) {
			icon = new ImageIcon(imgURL);
		}
		return icon;
	}

	public void addStopRoutine(IStopRoutine routine) {
		IGB igb = (IGB)IGB.getSingleton();
		igb.addStopRoutine(routine);
	}

	public ExtensionPointRegistry getExtensionPointRegistry() {
		return ExtensionPointRegistry.getInstance();
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

	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor) {
		return SearchView.searchForRegexInResidues(
				forward, regex, residues, residue_offset, Application.getSingleton().getMapView().getAxisTier(), glyphs, hitColor);
	}

	public JComponent getMapView() {
		return Application.getSingleton().getMapView();
	}

	private BioSeq getViewSeq() {
		return ((SeqMapView)getMapView()).getViewSeq();
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

	public String getCommandLineBatchFileStr() {
		return IGB.commandLineBatchFileStr;
	}

	public void setCommandLineBatchFileStr(String str) {
		IGB.commandLineBatchFileStr = str;
	}
}
