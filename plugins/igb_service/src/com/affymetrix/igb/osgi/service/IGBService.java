package com.affymetrix.igb.osgi.service;

import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JMenu;

import com.affymetrix.genoviz.bioviews.GlyphI;

public interface IGBService {
	public String[] getRequiredBundles();
	public boolean addMenu(JMenu menu);
	public boolean removeMenu(String menuName);
	public void addPlugIn(JComponent plugin, String name);
	public boolean removePlugIn(String name);
	public void displayError(String title, String errorText);
	public void displayError(String errorText);
	public void addNotLockedUpMsg(String message);
	public void removeNotLockedUpMsg(String message);
	// for restrictions page
	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor);
	public boolean isSeqResiduesAvailable();
	public int getSeqResiduesMin();
	public int getSeqResiduesMax();
	public String getSeqResidues();
	public void updateMap();
	public void removeGlyphs(List<GlyphI> glyphs);
	// for external page
	public String getUCSCQuery();
}
