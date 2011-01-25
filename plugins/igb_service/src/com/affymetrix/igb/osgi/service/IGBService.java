package com.affymetrix.igb.osgi.service;

import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;

import com.affymetrix.genometryImpl.util.FloatTransformer;
import com.affymetrix.genoviz.bioviews.GlyphI;
//import com.affymetrix.igb.view.SeqMapView.SeqMapRefreshed;

public interface IGBService {
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
	public ExtensionPointRegistry getExtensionPointRegistry();
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
	// for GeneralLoadView
	public String getCommandLineBatchFileStr();
	public void setCommandLineBatchFileStr(String str);
	// for transforms
	public void addTransform(ExtensionFactory<FloatTransformer> transformerFactory);
	public void removeTransform(String name);
}
