package com.affymetrix.igb;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometry.color.ColorProviderI;
import com.affymetrix.genometry.event.AxisPopupListener;
import com.affymetrix.genometry.event.ContextualPopupListener;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.event.GenericActionListener;
import com.affymetrix.genometry.event.GenericServerInitListener;
import com.affymetrix.genometry.filter.SymmetryFilterI;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.StatusAlert;
import com.affymetrix.igb.action.AutoLoadThresholdAction;
import com.affymetrix.igb.action.ChangeBackgroundColorAction;
import com.affymetrix.igb.action.ChangeExpandMaxAction;
import com.affymetrix.igb.action.ChangeExpandMaxAllAction;
import com.affymetrix.igb.action.ChangeFontSizeAction;
import com.affymetrix.igb.action.ChangeForegroundColorAction;
import com.affymetrix.igb.action.ChangeForwardColorAction;
import com.affymetrix.igb.action.ChangeLabelColorAction;
import com.affymetrix.igb.action.ChangeReverseColorAction;
import com.affymetrix.igb.action.ChangeTierHeightAction;
import com.affymetrix.igb.action.CollapseAction;
import com.affymetrix.igb.action.CustomizeAction;
import com.affymetrix.igb.action.ExpandAction;
import com.affymetrix.igb.action.ExportFileAction;
import com.affymetrix.igb.action.ExportSelectedAnnotationFileAction;
import com.affymetrix.igb.action.FloatTiersAction;
import com.affymetrix.igb.action.HideAction;
import com.affymetrix.igb.action.HomeAction;
import com.affymetrix.igb.action.LabelGlyphAction;
import com.affymetrix.igb.action.RemoveDataFromTracksAction;
import com.affymetrix.igb.action.RemoveFeatureAction;
import com.affymetrix.igb.action.RenameTierAction;
import com.affymetrix.igb.action.RepackSelectedTiersAction;
import com.affymetrix.igb.action.ScrollDownAction;
import com.affymetrix.igb.action.ScrollLeftAction;
import com.affymetrix.igb.action.ScrollRightAction;
import com.affymetrix.igb.action.ScrollUpAction;
import com.affymetrix.igb.action.SelectParentAction;
import com.affymetrix.igb.action.SeqMapToggleAction;
import com.affymetrix.igb.action.SetDirectionStyleArrowAction;
import com.affymetrix.igb.action.SetDirectionStyleColorAction;
import com.affymetrix.igb.action.ShowAllAction;
import com.affymetrix.igb.action.ShowMinusStrandAction;
import com.affymetrix.igb.action.ShowOneTierAction;
import com.affymetrix.igb.action.ShowPlusStrandAction;
import com.affymetrix.igb.action.ShowTwoTiersAction;
import com.affymetrix.igb.action.StartAutoScrollAction;
import com.affymetrix.igb.action.StopAutoScrollAction;
import com.affymetrix.igb.action.ThreadHandlerAction;
import com.affymetrix.igb.action.TierFontSizeAction;
import com.affymetrix.igb.action.ToggleEdgeMatchingAction;
import com.affymetrix.igb.action.UnFloatTiersAction;
import com.affymetrix.igb.action.UnsetDirectionStyleArrowAction;
import com.affymetrix.igb.action.UnsetDirectionStyleColorAction;
import com.affymetrix.igb.action.UseAsReferenceSeqAction;
import com.affymetrix.igb.action.ZoomInXAction;
import com.affymetrix.igb.action.ZoomInYAction;
import com.affymetrix.igb.action.ZoomOutXAction;
import com.affymetrix.igb.action.ZoomOutYAction;
import com.affymetrix.igb.action.ZoomingRepackAction;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.shared.ChangeExpandMaxOptimizeAction;
import com.affymetrix.igb.shared.CollapseExpandAction;
import com.affymetrix.igb.shared.LockTierHeightAction;
import com.affymetrix.igb.shared.TrackClickListener;
import com.affymetrix.igb.shared.UnlockTierHeightAction;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.swing.script.ScriptProcessor;
import com.affymetrix.igb.swing.script.ScriptProcessorHolder;
import com.affymetrix.igb.window.service.IWindowService;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.search.ISearchHints;
import com.lorainelab.igb.services.search.ISearchModeSym;
import com.lorainelab.igb.services.search.SearchListener;
import com.lorainelab.igb.services.window.WindowServiceLifecylceHook;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import com.lorainelab.igb.services.window.preferences.IPrefEditorComponent;
import com.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi Activator for igb bundle
 */
public class Activator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    private String commandLineBatchFileStr;
    private String[] args;
    private IGB igb;

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        verifyJidesoftLicense();
        igb = IGB.getInstance();
        args = CommonUtils.getInstance().getArgs(bundleContext);
        if (args != null) {
            commandLineBatchFileStr = CommonUtils.getInstance().getArg("-" + IgbService.SCRIPTFILETAG, args);
        }
        //wait for consoleLogger service
        setupConsoleServiceTracker(bundleContext);
        addGenericActionListener();
        initColorProvider(bundleContext);
        initFilter(bundleContext);
    }

    private void setupConsoleServiceTracker(final BundleContext bundleContext) {
        ServiceTracker<IgbMenuItemProvider, Object> consoleServiceTracker;
        consoleServiceTracker = new ServiceTracker<IgbMenuItemProvider, Object>(bundleContext, IgbMenuItemProvider.class, null) {
            @Override
            public Object addingService(ServiceReference<IgbMenuItemProvider> serviceReference) {
                if (serviceReference.getProperty("component.name").equals("ShowConsoleAction")) {
                    bundleContext.getService(serviceReference);
                    run(bundleContext);
                    logger.info("IGB Started");
                }
                return super.addingService(serviceReference);
            }
        };
        consoleServiceTracker.open();
    }

    private void setupWindowServiceTracker(final BundleContext bundleContext) {
        ServiceTracker<IWindowService, Object> serviceTracker;
        serviceTracker = new ServiceTracker<IWindowService, Object>(bundleContext, IWindowService.class, null) {
            @Override
            public Object addingService(ServiceReference<IWindowService> windowServiceReference) {
                logger.info("Starting Window Manager");
                IWindowService windowService = bundleContext.getService(windowServiceReference);
                final IgbTabPanelI[] tabs = igb.setWindowService(windowService);
                // set IgbService
                bundleContext.registerService(IgbService.class, IgbServiceImpl.getInstance(), null);
                // register tabs created in IGB itself - IGBTabPanel is an extension point
                for (IgbTabPanelI tab : tabs) {
                    bundleContext.registerService(IgbTabPanelI.class.getName(), tab, null);
                }
                logger.info("Window Manager Started");
                return super.addingService(windowServiceReference);
            }
        };
        serviceTracker.open();
    }

    private void verifyJidesoftLicense() {
        // Verify jidesoft license.
        logger.info("Verifying Jidesoft license");

        com.jidesoft.utils.Lm.verifyLicense("Dept. of Bioinformatics and Genomics, UNCC",
                "Integrated Genome Browser", ".HAkVzUi29bDFq2wQ6vt2Rb4bqcMi8i1");
    }

    /**
     * method to start IGB, called when the window service is available, creates and initializes IGB and registers the
     * IgbService add any extension points handling here
     *
     * @param windowServiceReference - the OSGi ServiceReference for the window service
     */
    private void run(final BundleContext bundleContext) {
        logger.info("Starting IGB");
        setupWindowServiceTracker(bundleContext);
        IGB.commandLineBatchFileStr = commandLineBatchFileStr;

        igb.init(args, bundleContext);

        registerServices(bundleContext);

        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, TrackClickListener.class);
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeSym.class);
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchHints.class);

        addPopupListener(bundleContext);
        addAxisPopupListener(bundleContext);
        addScriptListener(bundleContext);
        addPrefEditorComponentListener(bundleContext);
        initSeqMapViewActions();
        addStatusAlertListener(bundleContext);
        addSearchListener(bundleContext);
        addServerInitListener(bundleContext);
        logger.trace("commandlinebatchFileStr....");
        if (IGB.commandLineBatchFileStr != null && IGB.commandLineBatchFileStr.length() > 0) {
            ScriptExecutor se = new ScriptExecutor();
            se.start();
        }
        logger.trace("settingFrame visibile");
        //igb.getApplicationFrame().setVisible(true);
    }

    /**
     * Add actions to the tool bar. Call getAction on all subclasses of SeqMapViewActionA so that they appear in the
     * tool bar. Must be done after SeqMapView is created and assigned to IGB.map_view.
     */
    private void initSeqMapViewActions() {
        ChangeForegroundColorAction.getAction();
        ChangeBackgroundColorAction.getAction();
        ChangeLabelColorAction.getAction();
        ChangeExpandMaxAction.getAction();
        LabelGlyphAction.getAction();
        TierFontSizeAction.getAction();
        GenericActionHolder.getInstance().addGenericAction(
                new SeqMapToggleAction(
                        ShowOneTierAction.getAction(),
                        ShowTwoTiersAction.getAction()));
        CollapseExpandAction.createSingleton();
        ZoomInXAction.getAction();
        ZoomOutXAction.getAction();
        ZoomInYAction.getAction();
        ZoomOutYAction.getAction();
        HomeAction.getAction();
        ScrollUpAction.getAction();
        ScrollDownAction.getAction();
        ScrollLeftAction.getAction();
        ScrollRightAction.getAction();
        ThreadHandlerAction.getAction();
        FloatTiersAction.getAction();
        UnFloatTiersAction.getAction();
        SetDirectionStyleArrowAction.getAction();
        UnsetDirectionStyleArrowAction.getAction();
        SetDirectionStyleColorAction.getAction();
        UnsetDirectionStyleColorAction.getAction();
        GenericActionHolder.getInstance().addGenericAction(
                new SeqMapToggleAction(
                        FloatTiersAction.getAction(),
                        UnFloatTiersAction.getAction()));
        GenericActionHolder.getInstance().addGenericAction(
                new SeqMapToggleAction(
                        LockTierHeightAction.getAction(),
                        UnlockTierHeightAction.getAction()));
        RenameTierAction.getAction();
        // These are not in the toolbar,
        // but they have keyboard shortcuts (accelerators)
        // defined in the preferences.
        //RefreshDataAction.getAction(); // no singleton.
        SelectParentAction.getAction();
        //com.affymetrix.igb.bookmarks.action.AddBookmarkAction.getAction(); // no singleton.
        //ExitSeqViewerAction.getAction(); // no singleton.
        ToggleEdgeMatchingAction.getAction();
        ChangeTierHeightAction.getAction();
        ChangeExpandMaxOptimizeAction.getAction();
        RemoveFeatureAction.getAction();
        GenericActionHolder.getInstance().addGenericAction(
                new SeqMapToggleAction(
                        StartAutoScrollAction.getAction(),
                        StopAutoScrollAction.getAction()));
        ZoomingRepackAction.getAction();
        ShowPlusStrandAction.getAction();
        ShowMinusStrandAction.getAction();
        ChangeForwardColorAction.getAction();
        ChangeReverseColorAction.getAction();
        ChangeFontSizeAction.getAction();
        ChangeExpandMaxAllAction.getAction();
//		SetColorByScoreAction.getAction();
//		ColorByScoreAction.getAction();
        ExportFileAction.getAction();
        ExportSelectedAnnotationFileAction.getAction();
        UseAsReferenceSeqAction.getAction();
        CustomizeAction.getAction();
        HideAction.getAction();
        ShowAllAction.getAction();
        //CenterAtHairlineAction.getAction();
        //MaximizeTrackAction.getAction();
        CollapseAction.getAction();
        ExpandAction.getAction();
        RemoveDataFromTracksAction.getAction();
        RepackSelectedTiersAction.getAction();
        AutoLoadThresholdAction.getAction();
    }

    private void initColorProvider(final BundleContext bundleContext) {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ColorProviderI.class);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometry.color.RGB(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.igb.colorproviders.Score(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.igb.colorproviders.MapqScore(), null);
        //bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometry.color.Strand(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.igb.colorproviders.Length(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.igb.colorproviders.Property(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometry.color.Duplicate(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometry.color.Paired(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometry.color.PariedByRunNo(), null);
    }

    private void initFilter(final BundleContext bundleContext) {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, SymmetryFilterI.class);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.WithIntronFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.NoIntronFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.UniqueLocationFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.NotUniqueLocationFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.ReadAlignmentsStrandFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.MismatchFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.MatchFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.QualityScoreFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.MappingQualityFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.ScoreFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.PairedFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.PairedByRunNoFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometry.filter.DuplicateFilter(), null);
    }

    private void addGenericActionListener() {
        //TODO: Probably should implement using extension point listener.
        GenericActionHolder.getInstance().addGenericActionListener(
                new GenericActionListener() {
                    @Override
                    public void onCreateGenericAction(GenericAction genericAction) {
                        if (genericAction.getId() != null) {//genericAction.getValue(javax.swing.Action.NAME)
                            KeyStroke ks = genericAction.getKeyStroke();
                            if (ks != null) {
                                genericAction.putValue(Action.ACCELERATOR_KEY, ks);
                            }

                            IGB.getInstance().addAction(genericAction);

                            boolean isToolbar = genericAction.isToolbarDefault() || PreferenceUtils.getToolbarNode().getBoolean(genericAction.getId(), false);
                            if (isToolbar) {
                                int index = PreferenceUtils.getToolbarNode().getInt(genericAction.getId() + ".index", -1);
                                if (index == -1) {
                                    index = genericAction.getToolbarIndex();
                                }
                                IGB.getInstance().addToolbarAction(genericAction, index);
                            }
                        }
                    }

                    @Override
                    public void notifyGenericAction(GenericAction genericAction) {
                    }
                }
        );
    }

    private void addPrefEditorComponentListener(final BundleContext bundleContext) {
        ExtensionPointHandler<IPrefEditorComponent> preferencesExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, IPrefEditorComponent.class);
        preferencesExtensionPoint.addListener(
                new ExtensionPointListener<IPrefEditorComponent>() {
                    @Override
                    public void removeService(IPrefEditorComponent prefs) {	/*cannot remove*/ }

                    @Override
                    public void addService(IPrefEditorComponent prefs) {
                        PreferencesPanel.getSingleton().addPrefEditorComponent(prefs);
                    }
                }
        );
    }

    private void registerServices(final BundleContext bundleContext) {

        bundleContext.registerService(WindowServiceLifecylceHook.class,
                new WindowServiceLifecylceHook() {
                    @Override
                    public void stop() {
                        IGB.getInstance().saveToolBar();
                    }

                    @Override
                    public void start() { /* Do Nothing */ }
                },
                null
        );
    }

    private void addPopupListener(final BundleContext bundleContext) {
        ExtensionPointHandler<ContextualPopupListener> popupExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ContextualPopupListener.class);
        popupExtensionPoint.addListener(
                new ExtensionPointListener<ContextualPopupListener>() {
                    @Override
                    public void addService(ContextualPopupListener listener) {
                        IGB.getInstance().getMapView().addPopupListener(listener);
                    }

                    @Override
                    public void removeService(ContextualPopupListener listener) {
                        IGB.getInstance().getMapView().removePopupListener(listener);
                    }
                }
        );
    }

    private void addAxisPopupListener(final BundleContext bundleContext) {
        ExtensionPointHandler<AxisPopupListener> popupExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, AxisPopupListener.class);
        popupExtensionPoint.addListener(
                new ExtensionPointListener<AxisPopupListener>() {
                    @Override
                    public void addService(AxisPopupListener listener) {
                        IGB.getInstance().getMapView().addAxisPopupListener(listener);
                    }

                    @Override
                    public void removeService(AxisPopupListener listener) {
                        IGB.getInstance().getMapView().removeAxisPopupListener(listener);
                    }
                }
        );
    }

    private void addScriptListener(final BundleContext bundleContext) {
        ExtensionPointHandler<ScriptProcessor> popupExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ScriptProcessor.class);
        popupExtensionPoint.addListener(
                new ExtensionPointListener<ScriptProcessor>() {
                    @Override
                    public void addService(ScriptProcessor scriptProcessor) {
                        ScriptProcessorHolder.getInstance().addScriptProcessor(scriptProcessor);
                    }

                    @Override
                    public void removeService(ScriptProcessor scriptProcessor) {
                        ScriptProcessorHolder.getInstance().removeScriptProcessor(scriptProcessor);
                    }
                }
        );
    }

    private void addStatusAlertListener(final BundleContext bundleContext) {
        ExtensionPointHandler<StatusAlert> popupExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, StatusAlert.class);
        popupExtensionPoint.addListener(
                new ExtensionPointListener<StatusAlert>() {
                    @Override
                    public void addService(StatusAlert alert) {
                        IGB.getInstance().addStatusAlert(alert);
                    }

                    @Override
                    public void removeService(StatusAlert alert) {
                        IGB.getInstance().removeStatusAlert(alert);
                    }
                }
        );
    }

    private void addSearchListener(final BundleContext bundleContext) {
        ExtensionPointHandler<SearchListener> searchExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, SearchListener.class);
        searchExtensionPoint.addListener(
                new ExtensionPointListener<SearchListener>() {
                    @Override
                    public void addService(SearchListener searchListener) {
                        IGB.getInstance().getMapView().getMapRangeBox().addSearchListener(searchListener);
                    }

                    @Override
                    public void removeService(SearchListener searchListener) {
                        IGB.getInstance().getMapView().getMapRangeBox().removeSearchListener(searchListener);
                    }
                }
        );
    }

    private void addServerInitListener(final BundleContext bundleContext) {
        ExtensionPointHandler<GenericServerInitListener> searchExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, GenericServerInitListener.class);
        searchExtensionPoint.addListener(
                new ExtensionPointListener<GenericServerInitListener>() {
                    @Override
                    public void addService(GenericServerInitListener listener) {
                        ServerList.getServerInstance().addServerInitListener(listener);
                    }

                    @Override
                    public void removeService(GenericServerInitListener listener) {
                        ServerList.getServerInstance().removeServerInitListener(listener);
                    }
                }
        );
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }

    private class ScriptExecutor extends Thread {

        private boolean timeup = false;
        private Timer timer;

        @Override
        public void run() {
            java.awt.event.ActionListener al = e -> {
                timeup = true;
                timer.stop();
            };
            timer = new Timer(10000, al);
            timer.setRepeats(false);
            timer.start();

            while (true) {
                try {
                    sleep(1000);

                    boolean shouldRun = check();
                    if (shouldRun || timeup) {
                        if (shouldRun) {
                            ScriptManager.getInstance().runScript(IGB.commandLineBatchFileStr);
                            IGB.commandLineBatchFileStr = null;
                        }
                        break;
                    }
                } catch (Exception ex) {
                    break;
                }
            }
        }

        private boolean check() {
            return IgbServiceImpl.getInstance().areAllServersInited()
                    && IgbServiceImpl.getInstance().getApplicationFrame().isVisible();
        }
    }
}
