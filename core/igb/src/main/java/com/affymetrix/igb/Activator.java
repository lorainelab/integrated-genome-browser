package com.affymetrix.igb;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.color.ColorProviderI;
import com.affymetrix.genometryImpl.event.AxisPopupListener;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.GenericActionListener;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.StatusAlert;
import com.affymetrix.genoviz.swing.AMenuItem;
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
import com.affymetrix.igb.action.NewGenomeAction;
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
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IWindowRoutine;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.prefs.PrefsLoader;
import com.affymetrix.igb.prefs.WebLinkUtils;
import com.affymetrix.igb.shared.ChangeExpandMaxOptimizeAction;
import com.affymetrix.igb.shared.CollapseExpandAction;
import com.affymetrix.igb.shared.SeqSymmetryPreprocessorI;
import com.affymetrix.igb.shared.IPrefEditorComponent;
import com.affymetrix.igb.shared.ISearchHints;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.LockTierHeightAction;
import com.affymetrix.igb.shared.MapTierGlyphFactoryI;
import com.affymetrix.igb.shared.MapTierTypeHolder;
import com.affymetrix.igb.shared.PreprocessorTypeReference;
import com.affymetrix.igb.shared.SearchListener;
import com.affymetrix.igb.shared.TrackClickListener;
import com.affymetrix.igb.shared.UnlockTierHeightAction;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.ScriptManager;
import com.affymetrix.igb.swing.ScriptProcessor;
import com.affymetrix.igb.swing.ScriptProcessorHolder;
import com.affymetrix.igb.view.factories.AnnotationGlyphFactory;
import com.affymetrix.igb.view.factories.AxisGlyphFactory;
import com.affymetrix.igb.view.factories.GraphGlyphFactory;
import com.affymetrix.igb.view.factories.MismatchGlyphFactory;
import com.affymetrix.igb.view.factories.ProbeSetGlyphFactory;
import com.affymetrix.igb.view.factories.ScoredContainerGlyphFactory;
import com.affymetrix.igb.view.factories.SequenceGlyphFactory;
import com.affymetrix.igb.window.service.IWindowService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * OSGi Activator for igb bundle
 */
public class Activator implements BundleActivator {

    private String commandLineBatchFileStr;
    String[] args;
    private ServiceRegistration<ScriptManager> scriptManagerServiceReference;

    private static final Logger logger = Logger.getLogger(Activator.class.getPackage().getName());

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        args = CommonUtils.getInstance().getArgs(bundleContext);
        if (args != null) {       
            if (CommonUtils.getInstance().isExit(bundleContext)) {
                return;
            }
            scriptManagerServiceReference = bundleContext.registerService(ScriptManager.class, ScriptManager.getInstance(), null);
            commandLineBatchFileStr = CommonUtils.getInstance().getArg(
                    "-" + IGBService.SCRIPTFILETAG, args);
            // Force loading of prefs if hasn't happened yet.
            // Usually, since IGB.main() is called first,
            // prefs will have already been loaded via loadIGBPrefs() call in main().
            // But if for some reason an IGB instance is created without call to main(),
            // will force loading of prefs here...
            PrefsLoader.loadIGBPrefs(args);
        }
        // Verify jidesoft license.
        logger.info("Verifying Jidesoft license");
        
        com.jidesoft.utils.Lm.verifyLicense("Dept. of Bioinformatics and Genomics, UNCC",
                "Integrated Genome Browser", ".HAkVzUi29bDFq2wQ6vt2Rb4bqcMi8i1");
        
        logger.info("Getting IWindowService from ");
        ServiceReference<IWindowService> windowServiceReference
                = bundleContext.getServiceReference(IWindowService.class);

        if (windowServiceReference != null) {
            logger.info("Starting IGB");
            run(bundleContext, windowServiceReference);
            logger.info("IGB Started");
        } else {
            logger.info("Getting serviceTracker");
            ServiceTracker<IWindowService, Object> serviceTracker
                    = new ServiceTracker<IWindowService, Object>(
                            bundleContext, IWindowService.class, null) {

                                @Override
                                public Object addingService(ServiceReference<IWindowService> windowServiceReference) {
                                    logger.info("Starting IGB from  addingService()");
                                    run(bundleContext, windowServiceReference);
                                    logger.info("IGB Started");
                                    return super.addingService(windowServiceReference);
                                }
                            };
                    serviceTracker.open();
        }
        initOperators(bundleContext);
        initColorProvider(bundleContext);
        initFilter(bundleContext);
    }   

    @Override
    public void stop(BundleContext _bundleContext) throws Exception {
        if (scriptManagerServiceReference != null) {
            scriptManagerServiceReference.unregister();
            scriptManagerServiceReference = null;
        }
    }

    /**
     * method to start IGB, called when the window service is available, creates
     * and initializes IGB and registers the IGBService add any extension points
     * handling here
     *
     * @param windowServiceReference - the OSGi ServiceReference for the window
     * service
     */
    private void run(final BundleContext bundleContext, final ServiceReference<IWindowService> windowServiceReference) {
        final IGB igb = new IGB();
        IGB.commandLineBatchFileStr = commandLineBatchFileStr;

        // To avoid race condition on startup
        initAnnotationGlyphPreprocessors(bundleContext);
        initMapViewGlyphFactorys(bundleContext);

        igb.init(args);

        addGenericActionListener();
        registerServices(bundleContext, windowServiceReference, igb);

        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, TrackClickListener.class);
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeSym.class);
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchHints.class);

        addMenuItemListener(bundleContext);
        addPopupListener(bundleContext);
        addAxisPopupListener(bundleContext);
        addScriptListener(bundleContext);
        addPrefEditorComponentListener(bundleContext);
        initSeqMapViewActions();
        addShortcuts();
        addStatusAlertListener(bundleContext);
        addSearchListener(bundleContext);
        addServerInitListener(bundleContext);

        if (IGB.commandLineBatchFileStr != null && IGB.commandLineBatchFileStr.length() > 0) {
            ScriptExecutor se = new ScriptExecutor();
            se.start();
        }
        igb.getFrame().setVisible(true);
    }

    private void addShortcuts() {
        JFrame frm = Application.getSingleton().getFrame();
        JPanel panel = (JPanel) frm.getContentPane();
        Preferences p = PreferenceUtils.getKeystrokesNode();
        try {
            for (String k : p.keys()) {
                String preferredKeyStroke = p.get(k, "");
                if (preferredKeyStroke.length() == 0) { // then this ain't our concern.
                    continue;
                }
                GenericActionHolder h = GenericActionHolder.getInstance();
                GenericAction a = h.getGenericAction(k);
                if (null == a) { // A keystroke in the preferences has no known action.
                    String message = "key stroke \"" + k
                            + "\" is not among our generic actions.";
                    logger.config(message);
                    try { // to load the missing class.
                        ClassLoader l = this.getClass().getClassLoader();
                        Class<?> type = l.loadClass(k);
                        if (type.isAssignableFrom(GenericAction.class)) {
                            Class<? extends GenericAction> c = type.asSubclass(GenericAction.class);
                            // Now what?
                        }
                        continue;
                    } catch (ClassNotFoundException cnfe) {
                        message = "Class " + cnfe.getMessage() + " not found.";
                        logger.config(message);
                        continue; // Skip this one.
                    } finally {
                        message = "Keyboard shortcut " + preferredKeyStroke + " not set.";
                        logger.config(message);
                    }
                }
                InputMap im = panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
                ActionMap am = panel.getActionMap();
                String actionIdentifier = a.getId();
                KeyStroke ks = KeyStroke.getKeyStroke(preferredKeyStroke);
                if (null == ks) { // nothing we can do.
                    String message = "Could not find preferred key stroke: "
                            + preferredKeyStroke;
                    logger.config(message);
                    continue; // Skip this one.
                }
                im.put(ks, actionIdentifier);
                am.put(actionIdentifier, a);
            }
        } catch (BackingStoreException bse) {
            logger.config(bse.getMessage());
            logger.config("Some keyboard shortcuts may not be set.");
        }
    }

    /**
     * Add actions to the tool bar. Call getAction on all subclasses of
     * SeqMapViewActionA so that they appear in the tool bar. Must be done after
     * SeqMapView is created and assigned to IGB.map_view.
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
        NewGenomeAction.getAction();
    }

    private void initOperators(final BundleContext bundleContext) {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
        bundleContext.registerService(Operator.class, new com.affymetrix.igb.view.MismatchOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.igb.view.MismatchPileupOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.igb.view.NewFindJunctionOperator(false), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.igb.view.NewFindJunctionOperator(true), null);

    }

    private void initColorProvider(final BundleContext bundleContext) {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ColorProviderI.class);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometryImpl.color.RGB(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.igb.colorproviders.Score(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.igb.colorproviders.MapqScore(), null);
        //bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometryImpl.color.Strand(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.igb.colorproviders.Length(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.igb.colorproviders.Property(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometryImpl.color.Duplicate(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometryImpl.color.Paired(), null);
        bundleContext.registerService(ColorProviderI.class, new com.affymetrix.genometryImpl.color.PariedByRunNo(), null);
    }

    private void initFilter(final BundleContext bundleContext) {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, SymmetryFilterI.class);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.WithIntronFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.NoIntronFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.UniqueLocationFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.NotUniqueLocationFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.ReadAlignmentsStrandFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.MismatchFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.MatchFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.QualityScoreFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.MappingQualityFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.ScoreFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.PairedFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.PairedByRunNoFilter(), null);
        bundleContext.registerService(SymmetryFilterI.class, new com.affymetrix.genometryImpl.filter.DuplicateFilter(), null);
    }

    private void initAnnotationGlyphPreprocessors(final BundleContext bundleContext) {
        ExtensionPointHandler<SeqSymmetryPreprocessorI> annotationSeqSymmetryPreprocessorExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, SeqSymmetryPreprocessorI.class);

        annotationSeqSymmetryPreprocessorExtensionPoint.addListener(new ExtensionPointListener<SeqSymmetryPreprocessorI>() {
                    @Override
                    public void removeService(SeqSymmetryPreprocessorI factory) {
                        PreprocessorTypeReference.getInstance().removePreprocessor(factory, FileTypeCategory.Annotation);
                    }

                    @Override
                    public void addService(SeqSymmetryPreprocessorI factory) {
                        PreprocessorTypeReference.getInstance().addPreprocessor(FileTypeCategory.Annotation, factory);
                    }
                }
        );
    }

    private void initMapViewGlyphFactorys(final BundleContext bundleContext) {
        ExtensionPointHandler<MapTierGlyphFactoryI> mapViewGlyphFactoryExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, MapTierGlyphFactoryI.class);
        mapViewGlyphFactoryExtensionPoint.addListener(
                new ExtensionPointListener<MapTierGlyphFactoryI>() {
                    @Override
                    public void removeService(MapTierGlyphFactoryI factory) {
                        MapTierTypeHolder.getInstance().removeViewFactory(factory);
                    }

                    @Override
                    public void addService(MapTierGlyphFactoryI factory) {
                        MapTierTypeHolder.getInstance().addViewFactory(factory);
                    }
                }
        );

        // Add Annotation/Alignment factory
        AnnotationGlyphFactory annotationGlyphFactory = new AnnotationGlyphFactory();
        bundleContext.registerService(MapTierGlyphFactoryI.class, annotationGlyphFactory, null);

        // Add Sequence factory
        SequenceGlyphFactory sequenceGlyphFactory = new SequenceGlyphFactory();
        bundleContext.registerService(MapTierGlyphFactoryI.class, sequenceGlyphFactory, null);

        // Add Graph factories
        GraphGlyphFactory graphGlyphFactory = new GraphGlyphFactory();
        bundleContext.registerService(MapTierGlyphFactoryI.class, graphGlyphFactory, null);

        // Add ProbeSet factory
        ProbeSetGlyphFactory probeSetGlyphFactory = new ProbeSetGlyphFactory();
        bundleContext.registerService(MapTierGlyphFactoryI.class, probeSetGlyphFactory, null);

        // Add ScoredContainer factory
        ScoredContainerGlyphFactory scoredMinMaxAvg = new ScoredContainerGlyphFactory();
        bundleContext.registerService(MapTierGlyphFactoryI.class, scoredMinMaxAvg, null);

        // Add Mismatch factory
        MismatchGlyphFactory mismatchGlyphFactory = new MismatchGlyphFactory();
        bundleContext.registerService(MapTierGlyphFactoryI.class, mismatchGlyphFactory, null);

        // Add Axis factory
        AxisGlyphFactory axisGlyphFactory = new AxisGlyphFactory();
        bundleContext.registerService(MapTierGlyphFactoryI.class, axisGlyphFactory, null);

//		// Add Cytoband factory
//		CytoBandGlyphFactory cytoBandGlyphFactory = new CytoBandGlyphFactory();
//		bundleContext.registerService(MapTierGlyphFactoryI.class, cytoBandGlyphFactory, null);
        // Set Default factory
        MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Annotation, annotationGlyphFactory);
        MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Alignment, annotationGlyphFactory);
        MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Sequence, sequenceGlyphFactory);
        MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Graph, graphGlyphFactory);
        MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Mismatch, mismatchGlyphFactory);
        MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.ProbeSet, probeSetGlyphFactory);
        MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.ScoredContainer, scoredMinMaxAvg);
        MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Axis, axisGlyphFactory);
//		MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Cytoband, cytoBandGlyphFactory);
    }

    private void addGenericActionListener() {
        //TODO: Probably should implement using extension point listener.
        GenericActionHolder.getInstance().addGenericActionListener(
                new GenericActionListener() {
                    @Override
                    public void onCreateGenericAction(GenericAction genericAction) {
                        if (genericAction.getId() != null) {//genericAction.getValue(javax.swing.Action.NAME)
                            Preferences p = PreferenceUtils.getKeystrokesNode();
                            if (null != p) {
                                String ak = p.get(genericAction.getId(), "");
                                if (null != ak & 0 < ak.length()) {
                                    KeyStroke ks = KeyStroke.getKeyStroke(ak);
                                    genericAction.putValue(Action.ACCELERATOR_KEY, ks);
                                }
                            }

                            ((IGB) Application.getSingleton()).addAction(genericAction);

                            boolean isToolbar = PreferenceUtils.getToolbarNode().getBoolean(genericAction.getId(), false);
                            if (isToolbar) {
//							JRPButton button = new JRPButton("Toolbar_" + genericAction.getId(), genericAction);
                                int index = PreferenceUtils.getToolbarNode().getInt(genericAction.getId() + ".index", -1);
                                if (index == -1) {
                                    ((IGB) Application.getSingleton()).addToolbarAction(genericAction);
                                } else {
                                    ((IGB) Application.getSingleton()).addToolbarAction(genericAction, index);
                                }
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

    private void registerServices(final BundleContext bundleContext, final ServiceReference<IWindowService> windowServiceReference, final IGB igb) {
        IWindowService windowService = bundleContext.getService(windowServiceReference);
        final IGBTabPanel[] tabs = igb.setWindowService(windowService);
        // set IGBService
        bundleContext.registerService(IGBService.class, IGBServiceImpl.getInstance(), null);
        // register tabs created in IGB itself - IGBTabPanel is an extension point
        for (IGBTabPanel tab : tabs) {
            bundleContext.registerService(IGBTabPanel.class.getName(), tab, null);
        }

//		bundleContext.registerService(GeneralLoadView.class, GeneralLoadView.getLoadView(), null);
//		// Redisplay FeatureTreeView when FileTypeHandler added or removed.
//		ExtensionPointHandler<FileTypeHandler> extensionPoint
//				= ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, FileTypeHandler.class);
//		extensionPoint.addListener(new ExtensionPointListener<FileTypeHandler>() {
//			// note - the FileTypeHolder calls may happen before or after
//			// these, but the refreshTreeView() is a separate thread
//			@Override
//			public void removeService(FileTypeHandler fileTypeHandler) {
//				GeneralLoadView.getLoadView().refreshTreeView();
//			}
//			
//			@Override
//			public void addService(FileTypeHandler fileTypeHandler) {
//				GeneralLoadView.getLoadView().refreshTreeView();
//			}
//		});
        bundleContext.registerService(IWindowRoutine.class,
                new IWindowRoutine() {
                    @Override
                    public void stop() {
                        WebLinkUtils.autoSave();
                        ((IGB) Application.getSingleton()).saveToolBar();
                    }

                    @Override
                    public void start() { /* Do Nothing */ }
                },
                null
        );
    }

    private void addMenuItemListener(final BundleContext bundleContext) {
        ExtensionPointHandler<AMenuItem> menuExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, AMenuItem.class);

        menuExtensionPoint.addListener(new ExtensionPointListener<AMenuItem>() {
            @Override
            public void addService(AMenuItem amenuItem) {
                JMenu parent = ((IGB) Application.getSingleton()).getMenu(amenuItem.getParentMenu());
                if (parent == null) {
                    logger.log(Level.WARNING, "No menu found with name {0}. {1} is not added.", new Object[]{amenuItem.getParentMenu(), amenuItem.getMenuItem()});
                    return;
                }
                if (amenuItem.getLocation() == -1) {
                    MenuUtil.addToMenu(parent, amenuItem.getMenuItem());
                } else {
                    MenuUtil.insertIntoMenu(parent, amenuItem.getMenuItem(), amenuItem.getLocation());
                }

            }

            @Override
            public void removeService(AMenuItem amenuItem) {
                JMenu parent = ((IGB) Application.getSingleton()).getMenu(amenuItem.getParentMenu());
                if (parent == null) {
                    logger.log(Level.WARNING, "No menu found with name {0}. {1} is cannot be removed.", new Object[]{amenuItem.getParentMenu(), amenuItem.getMenuItem()});
                    return;
                }
                MenuUtil.removeFromMenu(parent, amenuItem.getMenuItem());
            }
        }
        );
    }

    private void addPopupListener(final BundleContext bundleContext) {
        ExtensionPointHandler<ContextualPopupListener> popupExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ContextualPopupListener.class);
        popupExtensionPoint.addListener(
                new ExtensionPointListener<ContextualPopupListener>() {
                    @Override
                    public void addService(ContextualPopupListener listener) {
                        Application.getSingleton().getMapView().addPopupListener(listener);
                    }

                    @Override
                    public void removeService(ContextualPopupListener listener) {
                        Application.getSingleton().getMapView().removePopupListener(listener);
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
                        Application.getSingleton().getMapView().addAxisPopupListener(listener);
                    }

                    @Override
                    public void removeService(AxisPopupListener listener) {
                        Application.getSingleton().getMapView().removeAxisPopupListener(listener);
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
                        Application.getSingleton().addStatusAlert(alert);
                    }

                    @Override
                    public void removeService(StatusAlert alert) {
                        Application.getSingleton().removeStatusAlert(alert);
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
                        Application.getSingleton().getMapView().getMapRangeBox().addSearchListener(searchListener);
                    }

                    @Override
                    public void removeService(SearchListener searchListener) {
                        Application.getSingleton().getMapView().getMapRangeBox().removeSearchListener(searchListener);
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

    private class ScriptExecutor extends Thread {

        private boolean timeup = false;
        private Timer timer;

        @Override
        public void run() {
            java.awt.event.ActionListener al = new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    timeup = true;
                    timer.stop();
                }
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
            return IGBServiceImpl.getInstance().areAllServersInited()
                    && IGBServiceImpl.getInstance().getFrame().isVisible();
        }
    }
}
