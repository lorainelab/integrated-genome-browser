package com.lorainelab.igb.service.api;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.swing.JRPMenu;
import com.google.common.base.Optional;
import com.lorainelab.igb.genoviz.extensions.api.SeqMapViewI;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.print.PrinterException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.event.ListSelectionListener;

/**
 * OSGi Service to allow bundles indirect access to IGB internals.
 *
 */
public interface IgbService {

    public static final String UTF8 = "UTF-8";
    /**
     * A potential parameter in either the URL of a bookmark, or a command-line
     * option.
     * This allows a response file to be loaded, instead of anything else.
     */
    public final static String SCRIPTFILETAG = "scriptfile";

    public final static boolean DEBUG_EVENTS = false;

    /**
     * Add a lockedUp message to the list of locked messages and display with
     * a little progress bar so that the app doesn't look locked up.
     *
     * @param message text of the message
     */
    public void addNotLockedUpMsg(String message);

    /**
     * Remove a lockedUp message from the list of locked messages and undisplay
     * it.
     *
     * @param message text of the message
     */
    public void removeNotLockedUpMsg(String message);

    /**
     * Sets the text in the status bar.
     * Will optionally echo a copy of the string to System.out.
     * It is safe to call this method even if the status bar is not being
     * displayed.
     */
    public void setStatus(String message);

    /**
     * Get the specified icon.
     *
     * @param name of the icon
     * @return the specified icon
     */
    public ImageIcon getIcon(String name);

    public JRPMenu addTopMenu(String id, String text, int index);

    public void loadAndDisplayAnnotations(GenericFeature gFeature);

    public void loadAndDisplaySpan(final SeqSpan span, final GenericFeature feature);

    public void loadChromosomes(GenericFeature gFeature);

    public void updateGeneralLoadView();

    public void doActions(final String batchFileStr);

    public void runScriptString(String line, String ext);

    public void performSelection(String selectParam);

    public GenericFeature getFeature(AnnotatedSeqGroup seqGroup, GenericServer gServer, String feature_url, boolean showErrorForUnsupported);

    public Optional<AnnotatedSeqGroup> determineAndSetGroup(final String version);

    public Color getDefaultBackgroundColor();

    public Color getDefaultForegroundColor();

    // for SearchView
    public void zoomToCoord(String seqID, int start, int end);

    public void mapRefresh(List<GlyphI> glyphs);

    public NeoAbstractWidget getSeqMap();

    // Listener
    public void addListSelectionListener(ListSelectionListener listener);

    public void removeListSelectionListener(ListSelectionListener listener);

    /**
     * Get the SeqMapViewI, the main window for IGB.
     *
     * @return the SeqMapViewI
     */
    public SeqMapViewI getSeqMapView();

    // for SearchView
    public boolean loadResidues(final SeqSpan viewspan, final boolean partial);

    public GenericAction loadResidueAction(final SeqSpan viewspan, final boolean partial);

    // for Graph Adjuster
    /**
     * Get the main JFrame for the application.
     *
     * @return the main JFrame for the IGB instance
     */
    public JFrame getFrame();

    /**
     * Save the current state of the application.
     */
    public void saveState();

    /**
     * Load the current state of the application.
     */
    public void loadState();

    public IgbTabPanel getTabPanel(String className);

    /**
     * select the given tab in the tab panel, bringing it to the front
     *
     * @param panel the IGBTabPanel
     */
    public void selectTab(IgbTabPanel panel);

    public void packMap(boolean fitx, boolean fity);

    public View getView();

    // for plugins
    public List<TierGlyph> getAllTierGlyphs();

    public List<TierGlyph> getSelectedTierGlyphs();

    public List<TierGlyph> getVisibleTierGlyphs();

    public RepositoryChangeHolderI getRepositoryChangerHolder();

    // ServerList
    public GenericServer loadServer(String server_url);

    public boolean areAllServersInited();

    public GenericServer getServer(String URLorName);

    // Open Uri
    public void openURI(URI uri, final String fileName, final AnnotatedSeqGroup loadGroup, final String speciesName, final boolean isReferenceSequence);

    public String getSelectedSpecies();

    public void addStyleSheet(String name, InputStream istr);

    public void removeStyleSheet(String name);

    public void addTrack(SeqSymmetry sym, String method);

    public void addSpeciesItemListener(ItemListener il);

    public void addPartialResiduesActionListener(ActionListener al);

    public IgbTabPanel getTabPanelFromDisplayName(String viewName);

    public Set<GenericServer> getEnabledServerList();

    public Collection<GenericServer> getAllServersList();

    public void discoverServer(final GenericServer server);

//	public void changeViewMode(SeqMapViewI gviewer, ITrackStyleExtended style, String viewMode, RootSeqSymmetry rootSym, ITrackStyleExtended comboStyle);
    public void goToRegion(String region);

    public GenericFeature findFeatureWithURI(GenericVersion version, URI featureURI);

    public void print(int pageFormat, boolean noDialog) throws PrinterException;

    public void refreshDataManagementView();

    public void loadVisibleFeatures();

    public void selectFeatureAndCenterZoomStripe(String selectParam);

    public void openPreferencesOtherPanel();

    public void openPreferencesPanelTab(int tabIndex);

    public int getPreferencesPanelTabIndex(Component c);

    public float getDefaultTrackSize();

    public void deselect(GlyphI tierGlyph);

    public void setHome();

    public GenericServer addServer(ServerTypeI serverType, String serverName, String serverURL, int order);

    public GenericServer addServer(ServerTypeI serverType, String serverName, String serverURL, int order, String mirrorURL); //qlmirror

    public void removeServer(GenericServer gServer);

    public Component determineSlicedComponent();

    public GenericFeature createFeature(String featureName, SymLoader loader);

    public void bringToFront();
}
