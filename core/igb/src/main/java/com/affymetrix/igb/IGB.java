/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.StateProvider;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.APP_VERSION;
import com.affymetrix.igb.general.Persistence;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.prefs.WebLinkUtils;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.ScriptManager;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.util.IGBAuthenticator;
import com.affymetrix.igb.util.IGBTrustManager;
import com.affymetrix.igb.util.MainMenuUtil;
import com.affymetrix.igb.view.AltSpliceView;
import com.affymetrix.igb.view.IGBToolBar;
import com.affymetrix.igb.view.SeqGroupViewGUI;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.load.GeneralLoadViewGUI;
import com.affymetrix.igb.view.welcome.MainWorkspaceManager;
import com.affymetrix.igb.window.service.IMenuCreator;
import com.affymetrix.igb.window.service.IWindowService;
import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;
import com.boxysystems.jgoogleanalytics.LoggingAdapter;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.lorainelab.logging.console.ConsoleLoggerGUI;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the Integrated Genome Browser (IGB, pronounced ig-bee).
 *
 * @version $Id: IGB.java 11452 2012-05-07 22:32:36Z lfrohman $
 */
public final class IGB extends Application
        implements GroupSelectionListener, SeqSelectionListener {

    private static final Logger logger = LoggerFactory.getLogger(IGB.class);

    private static final String GUARANTEED_URL = "http://www.google.com"; // if URL goes away, the program will always give a "not connected" error
    private static final String COUNTER_URL = "http://www.igbquickload.org/igb/counter";
    public static final String NODE_PLUGINS = "plugins";
    private JFrame frm;
    private JMenuBar mbar;
    private IGBToolBar tool_bar;
    private SeqMapView map_view;
    private AnnotatedSeqGroup prev_selected_group = null;
    private BioSeq prev_selected_seq = null;
    public static volatile String commandLineBatchFileStr = null;	// Used to run batch file actions if passed via command-line
    private IWindowService windowService;
    private SwingWorker<Void, Void> scriptWorker = null; // thread for running scripts - only one script can run at a time
    final public static boolean IS_WINDOWS
            = System.getProperty("os.name").toLowerCase().contains("windows");
    final public static boolean IS_MAC
            = System.getProperty("os.name").toLowerCase().contains("mac");
    final public static boolean IS_LINUX
            = System.getProperty("os.name").toLowerCase().contains("linux");

    public IGB() {
        super();
    }

    @Override
    public SeqMapView getMapView() {
        return map_view;
    }

    @Override
    public JFrame getFrame() {
        return frm;
    }

    private static void loadSynonyms(String file, SynonymLookup lookup) {
        InputStream istr = null;
        try {
            istr = IGB.class.getResourceAsStream(file);
            lookup.loadSynonyms(IGB.class.getResourceAsStream(file), true);
        } catch (IOException ex) {
            logger.info(
                    "Problem loading default synonyms file " + file, ex);
        } finally {
            GeneralUtils.safeClose(istr);
        }
    }

    //TODO: Remove this redundant call to set LAF. For now it fixes bug introduced by OSGi.
    public static void setLaf() {

        // Turn on anti-aliased fonts. (Ignored prior to JDK1.5)
        System.setProperty("swing.aatext", "true");

        // Letting the look-and-feel determine the window decorations would
        // allow exporting the whole frame, including decorations, to an eps file.
        // But it also may take away some things, like resizing buttons, that the
        // user is used to in their operating system, so leave as false.
        JFrame.setDefaultLookAndFeelDecorated(false);

        // if this is != null, then the user-requested l-and-f has already been applied
        if (System.getProperty("swing.defaultlaf") == null) {
            if (IS_WINDOWS) {
                try {
                    // If this is Windows and Nimbus is not installed, then use the Windows look and feel.
                    Class<?> cl = Class.forName(LookAndFeelFactory.WINDOWS_LNF);
                    LookAndFeel look_and_feel = (LookAndFeel) cl.newInstance();

                    if (look_and_feel.isSupportedLookAndFeel()) {
                        LookAndFeelFactory.installJideExtension();
                        // Is there a better way to do it? HV 03/02/12
                        for (Entry<Object, Object> obj : look_and_feel.getDefaults().entrySet()) {
                            UIManager.getDefaults().put(obj.getKey(), obj.getValue());
                        }
                        UIManager.setLookAndFeel(look_and_feel);
                    }
                } catch (Exception ulfe) {
                    // Windows look and feel is only supported on Windows, and only in
                    // some version of the jre.  That is perfectly ok.
                }
            } else if (IS_LINUX) {
                try {
                    // If this is Windows and Nimbus is not installed, then use the Windows look and feel.
                    Class<?> cl = Class.forName(LookAndFeelFactory.METAL_LNF);
                    LookAndFeel look_and_feel = (LookAndFeel) cl.newInstance();

                    if (look_and_feel.isSupportedLookAndFeel()) {
                        LookAndFeelFactory.installJideExtension();
                        // Is there a better way to do it? HV 03/02/12
                        for (Entry<Object, Object> obj : look_and_feel.getDefaults().entrySet()) {
                            UIManager.getDefaults().put(obj.getKey(), obj.getValue());
                        }
                        UIManager.setLookAndFeel(look_and_feel);
                    }
                } catch (Exception ulfe) {
                    // Windows look and feel is only supported on Windows, and only in
                    // some version of the jre.  That is perfectly ok.
                }
            }
        }

    }

    public void init(String[] args) {
        logger.debug("Setting look and feel");

        setLaf();

        // Set up a custom trust manager so that user is prompted
        // to accept or reject untrusted (self-signed) certificates
        // when connecting to server over HTTPS
        logger.debug("installTrustManager");
        IGBTrustManager.installTrustManager();

        // Initialize the ConsoleView right off, so that ALL output will
        // be captured there.
        logger.debug("Setting up ConsoleView");
        ConsoleLoggerGUI.getInstance();
        printDetails(args);
        logger.debug("Done setting up ConsoleView");

        loadSynonyms("/" + Constants.SYNONYMS_TXT, SynonymLookup.getDefaultLookup());
        loadSynonyms("/" + Constants.CHROMOSOMES_TXT, SynonymLookup.getChromosomeLookup());

        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            MacIntegration mi = MacIntegration.getInstance();
            if (this.getIcon() != null) {
                mi.setDockIconImage(this.getIcon());
            }
        }

        frm = new JFrame(APP_NAME + " " + APP_VERSION);

        // when HTTP authentication is needed, getPasswordAuthentication will
        //    be called on the authenticator set as the default
        Authenticator.setDefault(new IGBAuthenticator(frm));

        StateProvider stateProvider = new IGBStateProvider();
        DefaultStateProvider.setGlobalStateProvider(stateProvider);

        frm.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        Image icon = getIcon();
        if (icon != null) {
            frm.setIconImage(icon);
        }

        GenometryModel gmodel = GenometryModel.getInstance();
        gmodel.addGroupSelectionListener(this);
        gmodel.addSeqSelectionListener(this);
        // WARNING!!  IGB _MUST_ be added as group and seq selection listener to model _BEFORE_ map_view is,
        //    otherwise assumptions for persisting group / seq / span prefs are not valid!

        MenuUtil.setAccelerators(
                new AbstractMap<String, KeyStroke>() {

                    @Override
                    public Set<java.util.Map.Entry<String, KeyStroke>> entrySet() {
                        return null;
                    }

                    @Override
                    public KeyStroke get(Object action_command) {
                        return PreferenceUtils.getAccelerator((String) action_command);
                    }
                }
        );
        map_view = new SeqMapView(true, "SeqMapView", getFrame());
        gmodel.addSeqSelectionListener(map_view);
        gmodel.addGroupSelectionListener(map_view);
        gmodel.addSymSelectionListener(map_view);

        mbar = new JMenuBar();
        frm.setJMenuBar(mbar);
        MainMenuUtil.getInstance().loadMenu(mbar, "IGB");

        Rectangle frame_bounds = PreferenceUtils.retrieveWindowLocation("main window",
                new Rectangle(0, 0, 1100, 720)); // 1.58 ratio -- near golden ratio and 1920/1200, which is native ratio for large widescreen LCDs.
        PreferenceUtils.setWindowSize(frm, frame_bounds);

        // Show the frame before loading the plugins.  Thus any error panel
        // that is created by an exception during plugin set-up will appear
        // on top of the main frame, not hidden by it.
        frm.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                JFrame frame = (JFrame) evt.getComponent();
                String message = "Do you really want to exit?";

                if (confirmPanel(message, PreferenceUtils.ASK_BEFORE_EXITING, PreferenceUtils.default_ask_before_exiting)) {
                    TrackStyle.autoSaveUserStylesheet();
                    Persistence.saveCurrentView(map_view);
                    defaultCloseOperations();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });

        ScriptManager.getInstance().setInputHandler(new ScriptManager.InputHandler() {
            public InputStream getInputStream(String fileName) throws Exception {
                return LocalUrlCacher.getInputStream(relativeToAbsolute(fileName).toURL());
            }

            /* This method is used to convert the given file path from relative to absolute.
             */
            private URI relativeToAbsolute(String path) throws URISyntaxException {
                if (!(path.startsWith("file:")) && !(path.startsWith("http:")) && !(path.startsWith("https:")) && !(path.startsWith("ftp:"))) {
                    return getAbsoluteFile(path).toURI();
                }
                return new URI(path);
            }

            /*Returns the File object at given path
             */
            private File getAbsoluteFile(String path) {
                return new File(path).getAbsoluteFile();
            }
        });

        WebLinkUtils.autoLoad();

        GeneralLoadViewGUI.init(IGBServiceImpl.getInstance());
        MainWorkspaceManager.getWorkspaceManager().setSeqMapViewObj(map_view);
        SeqGroupViewGUI.init(IGBServiceImpl.getInstance());
        checkInternetConnection();
        notifyCounter();
        openQuickStart();
        ToolTipManager.sharedInstance().setDismissDelay(10000);
    }

    private void printDetails(String[] args) {
        logger.info("Starting: " + APP_NAME + " " + APP_VERSION);
        logger.info("Java version: " + System.getProperty("java.version") + " from " + System.getProperty("java.vendor"));
        Runtime runtime = Runtime.getRuntime();
        logger.info("System memory: " + humanReadableByteCount(runtime.maxMemory(), true));

    }

    //credit http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void notifyCounter() {
        JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(
                IGBConstants.APP_NAME, IGBConstants.APP_VERSION, IGBConstants.GOOGLE_ANALYTICS_ID);
        LoggingAdapter loggingAdapter = new LoggingAdapter() {

            @Override
            public void logError(String error) {
                logger.debug("Google Analytics Error Message: {}", error);
            }

            @Override
            public void logMessage(String message) {
                logger.debug("Google Analytics Response Message: {}", message);
            }
        };
        tracker.setLoggingAdapter(loggingAdapter);
        tracker.trackAsynchronously(new FocusPoint("IGB_Loaded"));
        LocalUrlCacher.isValidURL(COUNTER_URL);
    }

    private void checkInternetConnection() {
        boolean connected = LocalUrlCacher.isValidURL(GUARANTEED_URL);
        if (!connected) {
            ErrorHandler.errorPanel(IGBConstants.BUNDLE.getString("internetError"));
        }
    }

    private void openQuickStart() {
        String version = PreferenceUtils.getStringParam(IGBConstants.APP_NAME, null);
        if (version == null || !version.equals(IGBConstants.APP_VERSION)) {
            PreferenceUtils.getTopNode().put(IGBConstants.APP_NAME, IGBConstants.APP_VERSION);
            GeneralUtils.browse(IGBConstants.BUNDLE.getString("quickstart"));
        }
    }

    public void defaultCloseOperations() {
        windowService.shutdown();
    }

    public IGBTabPanel[] setWindowService(final IWindowService windowService) {
        this.windowService = windowService;
        windowService.setMainFrame(frm);

        windowService.setSeqMapView(MainWorkspaceManager.getWorkspaceManager());

        windowService.setStatusBar(status_bar);
        if (tool_bar == null) {
            tool_bar = new IGBToolBar();
        }
        windowService.setToolBar(tool_bar);
        windowService.setTabsMenu(mbar);
        windowService.setMenuCreator(
                new IMenuCreator() {

                    @Override
                    public JMenuBar createMenu(String id) {
                        JMenuBar menubar = new JMenuBar();
                        MainMenuUtil.getInstance().loadMenu(menubar, id);
                        return menubar;
                    }
                });
        return new IGBTabPanel[]{GeneralLoadViewGUI.getLoadView(), SeqGroupViewGUI.getInstance(), AltSpliceView.getSingleton()};
    }

    public JRPMenu addTopMenu(String id, String text) {
        return MenuUtil.getRPMenu(mbar, id, text);
    }

    public int addToolbarAction(GenericAction genericAction) {
        if (tool_bar == null) {
            tool_bar = new IGBToolBar();
        }
        addToolbarAction(genericAction, tool_bar.getItemCount());

        return tool_bar.getItemCount();
    }

    public void addToolbarAction(GenericAction genericAction, int index) {
        if (tool_bar == null) {
            tool_bar = new IGBToolBar();
        }
        tool_bar.addToolbarAction(genericAction, index);
    }

    public void removeToolbarAction(GenericAction action) {
        if (tool_bar == null) {
            return;
        }
        tool_bar.removeToolbarAction(action);
    }

    void saveToolBar() {
        if (tool_bar == null) {
            return;
        }

        tool_bar.saveToolBar();
    }

    @Override
    public ImageIcon getSmallIcon() {
        return CommonUtils.getInstance().getApplicationSmallIcon();
    }

    /**
     * Returns the icon stored in the jar file. It is expected to be at
     * com.affymetrix.igb.igb.gif.
     *
     * @return null if the image file is not found or can't be opened.
     */
    @Override
    public Image getIcon() {
        ImageIcon imageIcon = CommonUtils.getInstance().getApplicationIcon();
        if (imageIcon != null) {
            return imageIcon.getImage();
        }
        return null;
    }

    @Override
    public void groupSelectionChanged(GroupSelectionEvent evt) {
        AnnotatedSeqGroup selected_group = evt.getSelectedGroup();
        if ((prev_selected_group != selected_group) && (prev_selected_seq != null)) {
            Persistence.saveSeqSelection(prev_selected_seq);
            Persistence.saveSeqVisibleSpan(map_view);
        }
        prev_selected_group = selected_group;
    }

    @Override
    public void seqSelectionChanged(SeqSelectionEvent evt) {
        BioSeq selected_seq = evt.getSelectedSeq();
        if ((prev_selected_seq != null) && (prev_selected_seq != selected_seq)) {
            Persistence.saveSeqVisibleSpan(map_view);
        }
        prev_selected_seq = selected_seq;
        getFrame().setTitle(getTitleBar(selected_seq));
    }

    public IWindowService getWindowService() {
        return windowService;
    }

    public JRPMenu getMenu(String menuId) {
        String id = "IGB_main_" + menuId + "Menu";
        int num_menus = mbar.getMenuCount();
        for (int i = 0; i < num_menus; i++) {
            JRPMenu menu_i = (JRPMenu) mbar.getMenu(i);
            if (id.equals(menu_i.getId())) {
                return menu_i;
            }
        }
        return null;
    }

    public Set<IGBTabPanel> getTabs() {
        return windowService.getPlugins();
    }

    @Override
    public void setSelField(Map<String, Object> properties, String message) {
        tool_bar.setSelectionText(properties, message);
    }

    /**
     * Get a named view.
     */
    public IGBTabPanel getView(String viewName) {
        for (IGBTabPanel plugin : windowService.getPlugins()) {
            if (plugin.getClass().getName().equals(viewName)) {
                return plugin;
            }
        }
        String message = getClass().getName() + ".getView() failed for " + viewName;
        logger.error(message);
        return null;
    }

    /**
     * Get a named view. This differs from {@link #getView(String)} in that it
     * wants the display name instead of the full name. This is easier for
     * scripting.
     *
     * @param viewName the display name of a tab instead of a full package and
     * class name.
     */
    public IGBTabPanel getViewByDisplayName(String viewName) {
        for (IGBTabPanel plugin : windowService.getPlugins()) {
            if (plugin.getDisplayName().equals(viewName)) {
                return plugin;
            }
        }
        String message = getClass().getName() + ".getView() failed for \"" + viewName + "\"";
        try {
            logger.error(message);
        } catch (Exception x) {
            System.err.println(message);
        }
        return null;
    }

    public SwingWorker<Void, Void> getScriptWorker() {
        return scriptWorker;
    }

    public void setScriptWorker(SwingWorker<Void, Void> scriptWorker) {
        this.scriptWorker = scriptWorker;
    }

    /**
     * Put the action's accelerator key (if there is one) in the panel's input
     * and action maps. This makes the action available via shortcut, even if it
     * is "hidden" in a pop up menu.
     *
     * @param theAction to which the shortcut points.
     */
    public void addAction(GenericAction theAction) {
        JPanel panel = (JPanel) this.frm.getContentPane();
        Object o = theAction.getValue(Action.ACCELERATOR_KEY);
        if (null != o && o instanceof KeyStroke) {
            KeyStroke ks = (KeyStroke) o;
            InputMap im = panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = panel.getActionMap();

            Object existingObject = im.get(ks);
            if (existingObject != null) {
                im.remove(ks);
                Action existingAction = am.get(existingObject);
                if (existingAction != null) {
                    logger.error("Trying to add set keystroke for action {}."
                            + " But action {} exists with same keystroke \"{}\"."
                            + "\nUsing keystroke with latest action.",
                            new Object[]{theAction.getId(), existingAction.getClass(), ks});
                    existingAction.putValue(Action.ACCELERATOR_KEY, null);
                    am.remove(existingObject);
                }
            }

//			GenericActionHolder h = GenericActionHolder.getInstance();
            String actionIdentifier = theAction.getId();
            im.put(ks, actionIdentifier);
            am.put(actionIdentifier, theAction);
        }
    }

    private static String getTitleBar(BioSeq seq) {
        StringBuilder title = new StringBuilder(128);
        if (seq != null) {
            if (title.length() > 0) {
                title.append(" - ");
            }
            String seqid = seq.getID().trim();
            Pattern pattern = Pattern.compile("chr([0-9XYM]*)");
            if (pattern.matcher(seqid).matches()) {
                seqid = seqid.replace("chr", "Chromosome ");
            }

            title.append(seqid);
            String version_info = getVersionInfo(seq);
            if (version_info != null) {
                title.append("  (").append(version_info).append(')');
            }
        }
        if (title.length() > 0) {
            title.append(" - ");
        }
        title.append(IGBConstants.APP_NAME).append(" ").append(IGBConstants.APP_VERSION);
        return title.toString();
    }

    private static String getVersionInfo(BioSeq seq) {
        if (seq == null) {
            return null;
        }
        String version_info = null;
        if (seq.getSeqGroup() != null) {
            AnnotatedSeqGroup group = seq.getSeqGroup();
            if (group.getDescription() != null) {
                version_info = group.getDescription();
            } else {
                version_info = group.getID();
            }
        }
        if ("hg17".equals(version_info)) {
            version_info = "hg17 = NCBI35";
        } else if ("hg18".equals(version_info)) {
            version_info = "hg18 = NCBI36";
        }
        return version_info;
    }
}
