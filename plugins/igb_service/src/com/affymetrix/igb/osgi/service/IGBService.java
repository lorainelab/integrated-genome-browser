package com.affymetrix.igb.osgi.service;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JMenu;

import com.affymetrix.genoviz.bioviews.GlyphI;
//import com.affymetrix.igb.view.SeqMapView.SeqMapRefreshed;

public interface IGBService {
	public boolean addMenu(JMenu menu);
	public boolean removeMenu(String menuName);
	public void displayError(String title, String errorText);
	public void displayError(String errorText);
	public void addNotLockedUpMsg(String message);
	public void removeNotLockedUpMsg(String message);
	public ImageIcon getIcon(String name);
	// for plugins page
	public Set<String> getTier1Bundles();
	public Set<String> getTier2Bundles();
	public List<String> getRepositories();
	public void failRepository(String url);
	public void displayRepositoryPreferences();
	public void addRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	public void removeRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	// for restrictions page
	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor);
	public boolean isSeqResiduesAvailable();
	public int getSeqResiduesMin();
	public int getSeqResiduesMax();
	public String getSeqResidues();
	public void updateMap();
	public void removeGlyphs(List<GlyphI> glyphs);
	// for search page
//	public void addToRefreshList(SeqMapRefreshed smr);
	// for external page
	public String getUCSCQuery();
}
