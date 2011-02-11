package com.affymetrix.igb.osgi.service;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;

import org.osgi.framework.Bundle;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genoviz.bioviews.GlyphI;

public interface IGBService {
	public static final String IGB_TIER_HEADER = "IGB-Tier";
	// extension point names
	public static final String GRAPH_TRANSFORMS = "igb.graph.transform";
	public static final String TAB_PANELS = "igb.tab_panels";

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
	// for RestrictionSites/SearchView
	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor);
	public JComponent getMapView();
	// for GeneralLoadView
	public String getCommandLineBatchFileStr();
	public void setCommandLineBatchFileStr(String str);
	// for SearchView
	public String getGenomeSeqId();
	public boolean loadResidues(final SeqSpan viewspan, final boolean partial);
	// for PropertyView
	public void setPropertyHandler(PropertyHandler propertyHandler);
	// for graph adjuster
	public JFrame getFrame();
	public File getLoadDirectory();
	public void setLoadDirectory(File file);
}
