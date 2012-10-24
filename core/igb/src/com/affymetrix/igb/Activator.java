package com.affymetrix.igb;

import com.affymetrix.igb.view.factories.SequenceGlyphFactory;
import com.affymetrix.igb.view.factories.ScoredContainerGlyphFactory;
import com.affymetrix.igb.view.factories.AnnotationGlyphFactory;
import com.affymetrix.igb.view.factories.GraphGlyphFactory;
import com.affymetrix.igb.view.factories.ProbeSetGlyphFactory;
import com.affymetrix.igb.view.factories.MismatchGlyphFactory;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.GenericActionListener;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.action.*;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.prefs.PrefsLoader;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.shared.*;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.window.service.IWindowService;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * OSGi Activator for igb bundle
 */
public class Activator implements BundleActivator {
	protected BundleContext bundleContext;
    private String commandLineBatchFileStr;
	String[] args;

	private static final Logger ourLogger = Logger.getLogger(Activator.class.getPackage().getName());
	
	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		this.bundleContext = _bundleContext;
        args = CommonUtils.getInstance().getArgs(bundleContext);
        if (args != null) {
    		if (CommonUtils.getInstance().isHelp(bundleContext)) { // display all command options
				System.out.println("-offline - set the URL caching to offline");
				System.out.println("-" + IGBService.SCRIPTFILETAG + " - load a script file");
				System.out.println("-convert - convert the fasta file to bnib");
				System.out.println("-clrprf - clear the preferences");
				System.out.println("-prefsmode - use the specified preferences mode (default \"igb\")");
				System.out.println("-clrallprf - clear all the preferences for all preferences modes");
				System.out.println("-pntprf - print the preferences for this preferences mode in xml format");
				System.out.println("-pntallprf - print all the preferences for all preferences modes in xml format");
				return;
    		}

    		String prefsMode = CommonUtils.getInstance().getArg("-prefsmode", args);
    		if (prefsMode != null) {
    			PreferenceUtils.setPrefsMode(prefsMode);
    		}
    		if (CommonUtils.getInstance().getArg("-convert", args) != null) {
				String[] runArgs = Arrays.copyOfRange(args, 1, args.length);
				NibbleResiduesParser.main(runArgs);
				return;
    		}
    		if (CommonUtils.getInstance().getArg("-clrprf", args) != null) {
				PreferenceUtils.clearPreferences();
				XmlStylesheetParser.removeUserStylesheetFile();
				System.out.println("preferences cleared");
    		}
    		if (CommonUtils.getInstance().getArg("-clrallprf", args) != null) {
				PreferenceUtils.clearAllPreferences();
				XmlStylesheetParser.removeUserStylesheetFile();
				System.out.println("all preferences cleared");
    		}
    		if (CommonUtils.getInstance().getArg("-pntprf", args) != null) {
				PreferenceUtils.printPreferences();
    		}
    		if (CommonUtils.getInstance().getArg("-pntallprf", args) != null) {
				PreferenceUtils.printAllPreferences();
    		}
			if (CommonUtils.getInstance().getArg("-updateAvailable", args) != null) {
				CommonUtils.getInstance().setUpdateAvailable(true);
    		}
			String offline = CommonUtils.getInstance().getArg("-offline", args);
			if (offline != null) {
				LocalUrlCacher.setOffLine("true".equals(offline));
			}
			if (CommonUtils.getInstance().isExit(bundleContext)) {
	    		return;
	    	}
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
		com.jidesoft.utils.Lm.verifyLicense("Dept. of Bioinformatics and Genomics, UNCC",
			"Integrated Genome Browser", ".HAkVzUi29bDFq2wQ6vt2Rb4bqcMi8i1");
    	ServiceReference<IWindowService> windowServiceReference
				= bundleContext.getServiceReference(IWindowService.class);

        if (windowServiceReference != null) {
        	run(windowServiceReference);
        }
        else {
        	ServiceTracker<IWindowService, Object> serviceTracker
					= new ServiceTracker<IWindowService, Object>(
					bundleContext, IWindowService.class, null) {
				@Override
        	    public Object addingService(
						ServiceReference<IWindowService> windowServiceReference) {
        	    	run(windowServiceReference);
        	        return super.addingService(windowServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
		// Redisplay FeatureTreeView when FileTypeHandler added or removed.
		ExtensionPointHandler<FileTypeHandler> extensionPoint
				= ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, FileTypeHandler.class);
		extensionPoint.addListener(new ExtensionPointListener<FileTypeHandler>() {
			// note - the FileTypeHolder calls may happen before or after
			// these, but the refreshTreeView() is a separate thread
			@Override
			public void removeService(FileTypeHandler fileTypeHandler) {
				GeneralLoadView.getLoadView().refreshTreeView();
			}
			
			@Override
			public void addService(FileTypeHandler fileTypeHandler) {
				GeneralLoadView.getLoadView().refreshTreeView();
			}
		});
		initOperators();
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}

	/**
	 * method to start IGB, called when the window service is available,
	 * creates and initializes IGB and registers the IGBService
	 * add any extension points handling here
	 * @param windowServiceReference - the OSGi ServiceReference for the window service
	 */
	private void run(ServiceReference<IWindowService> windowServiceReference) {
        IWindowService windowService = bundleContext.getService(windowServiceReference);
        final IGB igb = new IGB();
        IGB.commandLineBatchFileStr = commandLineBatchFileStr;

		GenericActionHolder.getInstance().addGenericActionListener(
    		new GenericActionListener() {
				@Override
				public void onCreateGenericAction(GenericAction genericAction) {
					if (genericAction.getText() != null) {//genericAction.getValue(javax.swing.Action.NAME)
						boolean isToolbar = PreferenceUtils.getToolbarNode().getBoolean(genericAction.getId(), false);
						if (isToolbar) {
//							JRPButton button = new JRPButton("Toolbar_" + genericAction.getId(), genericAction);
							Preferences p = PreferenceUtils.getKeystrokesNode();
							if (null != p) {
								String ak = p.get(genericAction.getId(), "");
								if (null != ak & 0 < ak.length()) {
									KeyStroke ks = KeyStroke.getKeyStroke(ak);
									genericAction.putValue(Action.ACCELERATOR_KEY, ks);
								}
							}
							int index = PreferenceUtils.getToolbarNode().getInt(genericAction.getId()+".index", -1);
							if(index == -1){
								((IGB)Application.getSingleton()).addToolbarAction(genericAction);
							}else{
								((IGB)Application.getSingleton()).addToolbarAction(genericAction, index);
							}
						}
					}
				}
				@Override
				public void notifyGenericAction(GenericAction genericAction) {}
			}
    	);
			
		// To avoid race condition on startup
		initMapViewGlyphFactorys();
		
        igb.init(args);
        final IGBTabPanel[] tabs = igb.setWindowService(windowService);
        // set IGBService
		bundleContext.registerService(IGBService.class, IGBServiceImpl.getInstance(), null);
		bundleContext.registerService(GeneralLoadView.class, GeneralLoadView.getLoadView(), null);
		// register tabs created in IGB itself - IGBTabPanel is an extension point
		for (IGBTabPanel tab : tabs) {
			bundleContext.registerService(IGBTabPanel.class.getName(), tab, null);
		}
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, TrackClickListener.class);
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeSym.class);
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchHints.class);
		ExtensionPointHandler<IStopRoutine> stopRoutineExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, IStopRoutine.class);
		stopRoutineExtensionPoint.addListener(
			new ExtensionPointListener<IStopRoutine>() {
				@Override
				public void removeService(IStopRoutine routine) {	/*cannot remove*/ }
				@Override
				public void addService(IStopRoutine routine) {
					igb.addStopRoutine(routine);
				}
			}
		);
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
		bundleContext.registerService(IStopRoutine.class, 
			new IStopRoutine() {
				@Override
				public void stop() {
					WebLink.autoSave();
					((IGB)Application.getSingleton()).saveToolBar();
				}
			},
			null
		);
		initSeqMapViewActions();
		addShortcuts();
	}

	private void addShortcuts() {
		JFrame frm = Application.getSingleton().getFrame();
		JPanel panel = (JPanel) frm.getContentPane();
		Preferences p = PreferenceUtils.getKeystrokesNode();
		try {
			for (String k : p.keys()) {
				String preferredKeyStroke = p.get(k, "");
				if (preferredKeyStroke.equals("")) { // then this ain't our concern.
					continue;
				}
				GenericActionHolder h = GenericActionHolder.getInstance();
				GenericAction a = h.getGenericAction(k);
				if (null == a) { // A keystroke in the preferences has no known action.
					String message = "key stroke \"" + k
							+ "\" is not among our generic actions.";
					ourLogger.config(message);
					try { // to load the missing class.
						ClassLoader l = this.getClass().getClassLoader();
						Class<?> type = l.loadClass(k);
						if (type.isAssignableFrom(GenericAction.class)) {
							Class<? extends GenericAction> c = type.asSubclass(GenericAction.class);
							// Now what?
						}
						continue;
					}
					catch (ClassNotFoundException cnfe) {
						message = "Class " + cnfe.getMessage() + " not found.";
						ourLogger.config(message);
						continue; // Skip this one.
					}
					finally {
						message = "Keyboard shortcut " + preferredKeyStroke + " not set.";
						ourLogger.config(message);
					}
				}
				InputMap im = panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
				ActionMap am = panel.getActionMap();
				String actionIdentifier = a.getId();
				KeyStroke ks = KeyStroke.getKeyStroke(preferredKeyStroke);
				if (null == ks) { // nothing we can do.
					String message = "Could not find preferred key stroke: "
							+ preferredKeyStroke;
					ourLogger.config(message);
					continue; // Skip this one.
				}
				im.put(ks, actionIdentifier);
				am.put(actionIdentifier, a);
			}
		}
		catch (BackingStoreException bse) {
			ourLogger.config(bse.getMessage());
			ourLogger.config("Some keyboard shortcuts may not be set.");
		}
	}

	/**
	 * Add actions to the tool bar.
	 * Call getAction on all subclasses of SeqMapViewActionA
	 * so that they appear in the tool bar.
	 * Must be done after SeqMapView is created and assigned to IGB.map_view.
	 */
	private void initSeqMapViewActions() {
		RepackSelectedTiersAction.getAction();
		RepackAllTiersAction.getAction();
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
		//UCSCViewAction.getAction(); // external class. no singleton.
	}

	private void initOperators() {
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
		bundleContext.registerService(Operator.class, new com.affymetrix.igb.view.MismatchOperator(), null);
		bundleContext.registerService(Operator.class, new com.affymetrix.igb.view.MismatchPileupOperator(), null);
	}
	
	private void initMapViewGlyphFactorys() {
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

		// Set Default factory
		MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Annotation, annotationGlyphFactory);
		MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Alignment, annotationGlyphFactory);
		MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Sequence, sequenceGlyphFactory);
		MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Graph, graphGlyphFactory);
		MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.Mismatch, mismatchGlyphFactory);
		MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.ProbeSet, probeSetGlyphFactory);
		MapTierTypeHolder.getInstance().addDefaultFactory(FileTypeCategory.ScoredContainer, scoredMinMaxAvg);
	}
}
