/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy of the license must be included with
 * any distribution of this source code. Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb;

import com.affymetrix.common.CommonUtils;
import static com.affymetrix.common.CommonUtils.APP_NAME;
import static com.affymetrix.common.CommonUtils.APP_VERSION;
import static com.affymetrix.common.CommonUtils.IS_LINUX;
import static com.affymetrix.common.CommonUtils.IS_MAC;
import static com.affymetrix.common.CommonUtils.IS_WINDOWS;
import static com.affymetrix.common.CommonUtils.isDevelopmentMode;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenomeVersionSelectionEvent;
import com.affymetrix.genometry.event.GroupSelectionListener;
import com.affymetrix.genometry.event.SeqSelectionEvent;
import com.affymetrix.genometry.event.SeqSelectionListener;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.StateProvider;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.StatusAlert;
import static com.affymetrix.igb.IGBConstants.GOOGLE_ANALYTICS_ID;
import com.affymetrix.igb.general.Persistence;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.JRPMenuBar;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.util.IGBTrustManager;
import com.affymetrix.igb.view.IGBToolBar;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.StatusBar;
import com.affymetrix.igb.view.load.GeneralLoadViewGUI;
import com.affymetrix.igb.view.welcome.MainWorkspaceManager;
import com.affymetrix.igb.window.service.IWindowService;
import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;
import com.boxysystems.jgoogleanalytics.LoggingAdapter;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel;
import com.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the Integrated Genome Browser (IGB, pronounced ig-bee).
 *
 * @version $Id: IGB.java 11452 2012-05-07 22:32:36Z lfrohman $
 */
public class IGB implements GroupSelectionListener, SeqSelectionListener {

    public static final String COMPONENT_NAME = "IGB";
    private static final Logger logger = LoggerFactory.getLogger(IGB.class);
    private static IGB INSTANCE;
    private static final String COUNTER_URL = "http://www.igbquickload.org/igb/counter";
    public static final String NODE_PLUGINS = "plugins";
    private JFrame igbMainFrame;
    private JRPMenuBar mbar;
    private IGBToolBar toolbar;
    private SeqMapView mapView;
    private Image applicationIconImage;
    private GenomeVersion prev_selected_group = null;
    private BioSeq prev_selected_seq = null;
    public static volatile String commandLineBatchFileStr = null;	// Used to run batch file actions if passed via command-line
    private IWindowService windowService;
    private SwingWorker<Void, Void> scriptWorker = null; // thread for running scripts - only one script can run at a time
    private final StatusBar statusBar;
    private static final int delay = 2; //delay in seconds
    private final LinkedList<StatusAlert> statusAlertList;
    private final ActionListener status_alert_listener = e -> {
        if (e.getActionCommand().equals(String.valueOf(StatusAlert.HIDE_ALERT))) {
            removeStatusAlert((StatusAlert) e.getSource());
        }
    };

    private final LinkedList<String> progressStringList = new LinkedList<>(); // list of progress bar messages.
    ActionListener update_status_bar = ae -> {
        synchronized (progressStringList) {
            String s = progressStringList.pop();
            progressStringList.addLast(s);
            setNotLockedUpStatus(s);
        }
    };
    Timer timer = new Timer(delay * 1000, update_status_bar);

    private IGB() {
        statusAlertList = new LinkedList<>();
        statusBar = new StatusBar(status_alert_listener);
        setLaf();
        igbMainFrame = new JFrame(APP_NAME + " " + APP_VERSION);
        mapView = new SeqMapView(true, "SeqMapView", igbMainFrame);
        mbar = new JRPMenuBar();
        toolbar = new IGBToolBar();
        igbMainFrame.setJMenuBar(mbar);
        initializeApplicationIconImage();
    }

    private void initializeApplicationIconImage() {
        Optional<ImageIcon> applicationImageIcon = CommonUtils.getInstance().getApplicationIcon();
        if (applicationImageIcon.isPresent()) {
            applicationIconImage = applicationImageIcon.get().getImage();
            igbMainFrame.setIconImage(applicationIconImage);
        }
    }

    public SeqMapView getMapView() {
        return mapView;
    }

    public JFrame getFrame() {
        return igbMainFrame;
    }

    private static void setLaf() {
        // Turn on anti-aliased fonts. (Ignored prior to JDK1.5)
        System.setProperty("swing.aatext", "true");

        // Letting the look-and-feel determine the window decorations would
        // allow exporting the whole frame, including decorations, to an eps file.
        // But it also may take away some things, like resizing buttons, that the
        // user is used to in their operating system, so leave as false.
        JFrame.setDefaultLookAndFeelDecorated(false);
        if (IS_WINDOWS) {
            try {
                // If this is Windows and Nimbus is not installed, then use the Windows look and feel.
                Class<?> cl = Class.forName(LookAndFeelFactory.WINDOWS_LNF);
                LookAndFeel look_and_feel = (LookAndFeel) cl.newInstance();

                if (look_and_feel.isSupportedLookAndFeel()) {
                    LookAndFeelFactory.installJideExtension();
                    // Is there a better way to do it? HV 03/02/12
                    look_and_feel.getDefaults().entrySet().stream().forEach(entry -> {
                        UIManager.getDefaults().put(entry.getKey(), entry.getValue());
                    });
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
                    look_and_feel.getDefaults().entrySet().stream().forEach(entry -> {
                        UIManager.getDefaults().put(entry.getKey(), entry.getValue());
                    });
                    UIManager.setLookAndFeel(look_and_feel);
                }
            } catch (Exception ulfe) {
                // Windows look and feel is only supported on Windows, and only in
                // some version of the jre.  That is perfectly ok.
            }
        }

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

    private static String getTitleBar(BioSeq seq) {
        StringBuilder title = new StringBuilder(128);
        if (seq != null) {
            if (title.length() > 0) {
                title.append(" - ");
            }
            String seqid = seq.getId().trim();
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
        title.append(APP_NAME).append(" ").append(APP_VERSION);
        return title.toString();
    }

    private static String getVersionInfo(BioSeq seq) {
        if (seq == null) {
            return null;
        }
        String version_info = null;
        if (seq.getGenomeVersion() != null) {
            GenomeVersion genomeVersion = seq.getGenomeVersion();
            if (genomeVersion.getDescription() != null) {
                version_info = genomeVersion.getDescription();
            } else {
                version_info = genomeVersion.getName();
            }
        }
        if (null != version_info) {
            switch (version_info) {
                case "hg17":
                    version_info = "hg17 = NCBI35";
                    break;
                case "hg18":
                    version_info = "hg18 = NCBI36";
                    break;
            }
        }
        return version_info;
    }

    public void init(String[] args, BundleContext bundleContext) {
        logger.debug("Setting look and feel");
        // Set up a custom trust manager so that user is prompted
        // to accept or reject untrusted (self-signed) certificates
        // when connecting to server over HTTPS
        logger.debug("installTrustManager");
        IGBTrustManager.installTrustManager();
        printDetails(args);
        logger.debug("Done setting up ConsoleView");

        if (IS_MAC) {
            MacIntegration mi = MacIntegration.getInstance();
            if (applicationIconImage != null) {
                mi.setDockIconImage(applicationIconImage);
            }
        }
        StateProvider stateProvider = new IGBStateProvider();
        DefaultStateProvider.setGlobalStateProvider(stateProvider);
        igbMainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        GenometryModel gmodel = GenometryModel.getInstance();
        gmodel.addGroupSelectionListener(this);
        gmodel.addSeqSelectionListener(this);
        // WARNING!!  IGB _MUST_ be added as genomeVersion and seq selection listener to model _BEFORE_ map_view is,
        //    otherwise assumptions for persisting genomeVersion / seq / span prefs are not valid!
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

        gmodel.addSeqSelectionListener(mapView);
        gmodel.addGroupSelectionListener(mapView);
        gmodel.addSymSelectionListener(mapView.getSymSelectionListener());
        Rectangle frame_bounds = PreferenceUtils.retrieveWindowLocation("main window",
                new Rectangle(0, 0, 1100, 720)); // 1.58 ratio -- near golden ratio and 1920/1200, which is native ratio for large widescreen LCDs.
        PreferenceUtils.setWindowSize(igbMainFrame, frame_bounds);
        // Show the frame before loading the plugins.  Thus any error panel
        // that is created by an exception during plugin set-up will appear
        // on top of the main frame, not hidden by it.
        igbMainFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                JFrame frame = (JFrame) evt.getComponent();
                String message = "Do you really want to exit?";

                if (ModalUtils.confirmPanel(message, PreferenceUtils.ASK_BEFORE_EXITING, PreferenceUtils.default_ask_before_exiting)) {
                    try {
                        TrackStyle.autoSaveUserStylesheet();
                        Persistence.saveCurrentView(mapView);
                        defaultCloseOperations();
                        bundleContext.getBundle(0).stop();
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    } catch (BundleException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });
        ScriptManager.getInstance().setInputHandler(new ScriptManager.InputHandler() {
            @Override
            public InputStream getInputStream(String fileName) throws Exception {
                return LocalUrlCacher.getInputStream(relativeToAbsolute(fileName).toURL());
            }

            /* This method is used to convert the given file path from relative to absolute.
             */
            private URI relativeToAbsolute(String path) throws URISyntaxException {
                if (!(path.startsWith(FILE_PROTOCOL)) && !(path.startsWith(HTTP_PROTOCOL)) && !(path.startsWith(HTTPS_PROTOCOL)) && !(path.startsWith(FTP_PROTOCOL))) {
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
        GeneralLoadViewGUI.init(IgbServiceImpl.getInstance());
        MainWorkspaceManager.getWorkspaceManager().setSeqMapViewObj(mapView);
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

    private void notifyCounter() {
        if (!isDevelopmentMode()) {
            JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(
                    APP_NAME, APP_VERSION, GOOGLE_ANALYTICS_ID);
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
    }

    private void openQuickStart() {
        String version = PreferenceUtils.getStringParam(APP_NAME, null);
        if (version == null || !version.equals(APP_VERSION)) {
            PreferenceUtils.getTopNode().put(APP_NAME, APP_VERSION);
            GeneralUtils.browse(IGBConstants.BUNDLE.getString("quickstart"));
        }
    }

    public void defaultCloseOperations() {
        windowService.shutdown();
    }

    public void setWindowService(final IWindowService windowService) {
        this.windowService = windowService;
        windowService.setMainFrame(igbMainFrame);
        windowService.setSeqMapView(MainWorkspaceManager.getWorkspaceManager());
        windowService.setStatusBar(statusBar);
        windowService.setToolBar(toolbar);
        windowService.setTabsMenu(mbar);
    }

    public JRPMenu addTopMenu(String id, String text, int index) {
        return MenuUtil.getRPMenu(mbar, id, text, index);
    }

    public int addToolbarAction(GenericAction genericAction) {
        addToolbarAction(genericAction, genericAction.getToolbarIndex());
        return toolbar.getItemCount();
    }

    public void addToolbarAction(GenericAction genericAction, int index) {
        toolbar.addToolbarAction(genericAction, index);
    }

    public void removeToolbarAction(GenericAction action) {
        toolbar.removeToolbarAction(action);
    }

    void saveToolBar() {
        toolbar.saveToolBar();
    }

    @Override
    public void groupSelectionChanged(GenomeVersionSelectionEvent evt) {
        GenomeVersion selected_group = evt.getSelectedGroup();
        if ((prev_selected_group != selected_group) && (prev_selected_seq != null)) {
            Persistence.saveSeqSelection(prev_selected_seq);
            Persistence.saveSeqVisibleSpan(mapView);
        }
        prev_selected_group = selected_group;
    }

    @Override
    public void seqSelectionChanged(SeqSelectionEvent evt) {
        BioSeq selected_seq = evt.getSelectedSeq();
        if ((prev_selected_seq != null) && (prev_selected_seq != selected_seq)) {
            Persistence.saveSeqVisibleSpan(mapView);
        }
        prev_selected_seq = selected_seq;
        getFrame().setTitle(getTitleBar(selected_seq));
    }

    public IWindowService getWindowService() {
        return windowService;
    }

    public Set<IgbTabPanel> getTabs() {
        return windowService.getPlugins();
    }

    public void setSelField(Map<String, Object> properties, String message, SeqSymmetry sym) {
        toolbar.setSelectionText(properties, message, sym);
    }

    /**
     * Get a named view.
     */
    public IgbTabPanel getView(String viewName) {
        for (IgbTabPanelI plugin : windowService.getPlugins()) {
            IgbTabPanel igbTabPanel = plugin.getIgbTabPanel();
            if (igbTabPanel.getClass().getName().equals(viewName)) {
                return igbTabPanel;
            }
        }
        String message = getClass().getName() + ".getView() failed for " + viewName;
        logger.error(message);
        return null;
    }

    /**
     * Get a named view. This differs from {@link #getView(String)} in that it wants the display name instead of the
     * full name. This is easier for scripting.
     *
     * @param viewName the display name of a tab instead of a full package and class name.
     */
    public IgbTabPanel getViewByDisplayName(String viewName) {
        for (IgbTabPanelI plugin : windowService.getPlugins()) {
            IgbTabPanel igbTabPanel = plugin.getIgbTabPanel();
            if (igbTabPanel.getDisplayName().equals(viewName)) {
                return igbTabPanel;
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
     * Put the action's accelerator key (if there is one) in the panel's input and action maps. This makes the action
     * available via shortcut, even if it is "hidden" in a pop up menu.
     *
     * @param theAction to which the shortcut points.
     */
    public void addAction(GenericAction theAction) {
        JPanel panel = (JPanel) this.igbMainFrame.getContentPane();
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
                            theAction.getId(), existingAction.getClass(), ks);
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

    public StatusBar getStatusBar() {
        return statusBar;
    }

    public void addNotLockedUpMsg(final String s) {
        synchronized (progressStringList) {
            progressStringList.addFirst(s);
        }
        update_status_bar.actionPerformed(null);

        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void removeNotLockedUpMsg(final String s) {
        synchronized (progressStringList) {
            progressStringList.remove(s);

            if (progressStringList.isEmpty()) {
                setNotLockedUpStatus(null);
                timer.stop();
            }
        }
    }

    /**
     * Set the status text, and show a little progress bar so that the application doesn't look locked up.
     *
     * @param s text of the message
     */
    private synchronized void setNotLockedUpStatus(String s) {
        statusBar.setStatus(s);
    }

    /**
     * Sets the text in the status bar. Will also echo a copy of the string to System.out. It is safe to call this
     * method even if the status bar is not being displayed.
     */
    public void setStatus(String s) {
        setStatus(s, true);
    }

    /**
     * Sets the text in the status bar. Will optionally echo a copy of the string to System.out. It is safe to call this
     * method even if the status bar is not being displayed.
     *
     * @param echo Whether to echo a copy to System.out.
     */
    public void setStatus(final String s, final boolean echo) {
        statusBar.setStatus(s);
        if (echo && s != null && !s.isEmpty()) {
            logger.info(s);
        }
    }

    public void addStatusAlert(final StatusAlert s) {
        synchronized (statusAlertList) {
            statusAlertList.addFirst(s);
        }
        setStatusAlert(s);
    }

    public void removeStatusAlert(final StatusAlert s) {
        synchronized (statusAlertList) {
            statusAlertList.remove(s);
        }

        if (statusAlertList.isEmpty()) {
            setStatusAlert(null);
        } else {
            setStatusAlert(statusAlertList.pop());
        }
    }

    private synchronized void setStatusAlert(StatusAlert s) {
        statusBar.setStatusAlert(s);
    }

    public void showError(String title, String message, List<GenericAction> actions, Level level) {
        statusBar.showError(title, message, actions, level);
    }

    public static IGB getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new IGB();
            INSTANCE.toolbar.initHack();
        }
        return INSTANCE;
    }

}
