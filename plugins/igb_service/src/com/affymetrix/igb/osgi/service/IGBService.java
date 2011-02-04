package com.affymetrix.igb.osgi.service;

import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;

import org.osgi.framework.Bundle;

import com.affymetrix.genoviz.bioviews.GlyphI;
//import com.affymetrix.igb.view.SeqMapView.SeqMapRefreshed;

public interface IGBService {
	public static final String IGB_TIER_HEADER = "IGB-Tier";
	// extension point names
	public static final String GRAPH_TRANSFORMS = "igb.graph.transform";

	public final static boolean DEBUG_EVENTS = false;
	public boolean addMenu(JMenu menu);
	public boolean removeMenu(String menuName);
	public void displayError(String title, String errorText);
	public void displayError(String errorText);
	public void addNotLockedUpMsg(String message);
	public void removeNotLockedUpMsg(String message);
	public boolean confirmPanel(String text);
	public ImageIcon getIcon(String name);
	public void addStopRoutine(IStopRoutine routine);
	public int getTier(Bundle bundle);
	public ExtensionPointRegistry getExtensionPointRegistry();
	public JMenu getViewMenu();
	// for plugins page
	public List<String> getRepositories();
	public void failRepository(String url);
	public void displayRepositoryPreferences();
	public void addRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	public void removeRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	// for restrictions page
	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor);
	public JComponent getMapView();
	// for GeneralLoadView
	public String getCommandLineBatchFileStr();
	public void setCommandLineBatchFileStr(String str);
}
