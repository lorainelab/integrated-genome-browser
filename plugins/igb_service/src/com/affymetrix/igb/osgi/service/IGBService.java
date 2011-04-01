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
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

/**
 * OSGi Service to allow bundles indirect access to IGB internals.
 * 
 */
public interface IGBService {
	public static final String UTF8 = "UTF-8";
	/**
	 * A potential parameter in either the URL of a bookmark, or a command-line option.  This allows a response file to be loaded, instead of anything else.
	 */
	public final static String SCRIPTFILETAG = "scriptfile";
	public static final String IGB_TIER_HEADER = "IGB-Tier";
	// service extension point names
	public static final String GRAPH_TRANSFORMS = "igb.graph.transform";
	public static final String TAB_PANELS = "igb.tab_panels";
	public static final String SYM_FEATURE_URL = "feature_url_";

	public final static boolean DEBUG_EVENTS = false;
	/**
	 * add a menu to the main IGB menu
	 * @param menu the JMenu to add to the main IGB menu
	 * @return true if the menu was added, false otherwise
	 */
	public boolean addMenu(JMenu menu);
	/**
	 * remove a menu from the main IGB menu
	 * @param menuName the name of the JMenu to remove from the main IGB menu
	 * @return true if the menu was removed, false otherwise
	 */
	public boolean removeMenu(String menuName);
	/**
	 * Add a lockedUp message to the list of locked messages and display with
	 * a little progress bar so that the app doesn't look locked up.
	 * @param s text of the message
	 */
	public void addNotLockedUpMsg(String message);
	/**
	 * Remove a lockedUp message from the list of locked messages and undisplay it
	 * @param s text of the message
	 */
	public void removeNotLockedUpMsg(String message);
	/**
	 * Shows a panel asking for the user to confirm something.
	 *
	 * @param message the message String to display to the user
	 * @return true if the user confirms, else false.
	 */
	public boolean confirmPanel(String text);
	/**
	 * get the specified icon
	 * @param name of the icon
	 * @return the specified icon
	 */
	public ImageIcon getIcon(String name);
	/**
	 * add a routine to the list of routines that will run when
	 * the program shuts down
	 * @param routine the routine to run
	 */
	public void addStopRoutine(IStopRoutine routine);
	/**
	 * get the tier number of the specified bundle
	 * tier 1 means embedded - user will not see this
	 * tier 3 means external - user can install / uninstall
	 * @param bundle the bundle to check
	 * @return the tier number
	 */
	public int getTier(Bundle bundle);
	/**
	 * get the file menu of the application
	 * @return the file menu of the IGB application
	 */
	public JMenu getFileMenu();
	/**
	 * get the view menu of the application
	 * @return the view menu of the IGB application
	 */
	public JMenu getViewMenu();
	// for plugins page
	/**
	 * get the list of all repositories from the 
	 * Bundle Repository Preferences tab
	 * @return the list of bundle repositories (URLs)
	 */
	public List<String> getRepositories();
	/**
	 * mark a bundle repository as down / unavailable
	 * due to an error trying to connect
	 * @param url the URL to mark
	 */
	public void failRepository(String url);
	/**
	 * display the Bundle Repository tab of the Preferences page
	 */
	public void displayRepositoryPreferences();
	/**
	 * add a RepositoryChangeListener, to be called when there
	 * is a change to the Bundle Repositories on the Bundle
	 * Repository tab of the Preferences page
	 * @param repositoryChangeListener the listener
	 */
	public void addRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	/**
	 * remove a RepositoryChangeListener - so that is is no longer
	 * called for changes to the Bundle Repository tab of
	 * the Perferences page
	 * @param repositoryChangeListener the listener
	 */
	public void removeRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	/**
	 * Returns the value of the argument indicated by label.
	 * If arguments are
	 *   "-flag_2 -foo bar", then get_arg("foo", args)
	 * returns "bar", get_arg("flag_2") returns a non-null string,
	 * and get_arg("flag_5") returns null.
	 * @param label label to search for
	 * @param args the args to search
	 * @return the value found, or "true" if found with no value, or
	 * null if not found
	 */
	public String get_arg(String label, String[] args);

	// for BookMark
	/**
	 * get the constant value for AppName
	 * @return the constant value for the AppName
	 */
	public String getAppName();
	/**
	 * get the constant value for AppVersion
	 * @return the constant value for the AppVersion
	 */
	public String getAppVersion();
	/**
	 * set the state of the given tab to the given state and update
	 * the view menu to the new value
	 * @param igbTabPanel the tab to change
	 * @param tabState the new state
	 */
	public void setTabStateAndMenu(IGBTabPanel igbTabPanel, TabState tabState);
	// for RestrictionSites/SearchView
	/**
	 * get a count of the number of hits that match the specified regular
	 * expression and mark the in the Seq Map View
	 * @param forward - true = forward search, false = reverse search
	 * @param regex - the regular expression to match
	 * @param residues the residues to search
	 * @param residue_offset the starting offset within the residues
	 * @param glyphs the glyphs to mark
	 * @param hitColor the color to mark them with
	 * @return
	 */
	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor);
	/**
	 * get the SeqMapView, the main window for IGB
	 * @return the SeqMapView
	 */
	public JComponent getMapView();
	// for GeneralLoadView
	/**
	 * get the string entered for command line batch file
	 * @return the Command Line Batch File String
	 */
	public String getCommandLineBatchFileStr();
	/**
	 * set the string for the command line batch file
	 * @param str the String
	 */
	public void setCommandLineBatchFileStr(String str);
	// for SearchView
	/**
	 * get the constant value for GenomeSeqId
	 * @return the constant value for the GenomeSeqId
	 */
	public String getGenomeSeqId();
	public boolean loadResidues(final SeqSpan viewspan, final boolean partial);
	// for PropertyView
	/**
	 * set a PropertyHandler to handle display of the properties of the
	 * SeqMapView (main view) selection
	 * @param propertyHandler the implementation of the PropertyHandler
	 * the will process the property display for the SeqMapView selection
	 */
	public void setPropertyHandler(PropertyHandler propertyHandler);
	// for graph adjuster
	/**
	 * get the main JFrame for the application
	 * @return the main JFrame for the IGB instance
	 */
	public JFrame getFrame();
	/**
	 * save the current state of the application
	 */
	public void saveState();
	/**
	 * load the current state of the application
	 */
	public void loadState();
	/**
	 * get the load directory, where to find local files to
	 * be loaded
	 * @return the load directory
	 */
	public File getLoadDirectory();
	/**
	 * set the load directory, where to find local files to
	 * be loaded
	 * @param file the load directory
	 */
	public void setLoadDirectory(File file);
}
