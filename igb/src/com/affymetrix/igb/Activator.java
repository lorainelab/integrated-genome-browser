package com.affymetrix.igb;

import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.APP_VERSION_FULL;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.GenericActionListener;
import com.affymetrix.genometryImpl.operator.DepthOperator;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.action.*;
import com.affymetrix.igb.shared.CollapseExpandAction;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.shared.*;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.viewmode.AnnotationGlyphFactory;
import com.affymetrix.igb.viewmode.BaiSemanticZoomGlyphFactory;
import com.affymetrix.igb.viewmode.BarGraphGlyph;
import com.affymetrix.igb.viewmode.DefaultSemanticZoomGlyphFactory;
import com.affymetrix.igb.viewmode.DotGraphGlyph;
import com.affymetrix.igb.viewmode.FillBarGraphGlyph;
import com.affymetrix.igb.viewmode.GraphGlyphFactory;
import com.affymetrix.igb.viewmode.HeatMapGraphGlyph;
import com.affymetrix.igb.viewmode.LineGraphGlyph;
import com.affymetrix.igb.viewmode.MinMaxAvgGraphGlyph;
import com.affymetrix.igb.viewmode.MismatchGlyphFactory;
import com.affymetrix.igb.viewmode.OperatorGlyphFactory;
import com.affymetrix.igb.viewmode.ProbeSetGlyphFactory;
import com.affymetrix.igb.viewmode.ScoredContainerGlyphFactory;
import com.affymetrix.igb.viewmode.SequenceGlyphFactory;
import com.affymetrix.igb.viewmode.StairStepGraphGlyph;
import com.affymetrix.igb.viewmode.TbiSemanticZoomGlyphFactory;
import com.affymetrix.igb.window.service.IWindowService;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;

/**
 * OSGi Activator for igb bundle
 */
public class Activator implements BundleActivator {
	protected BundleContext bundleContext;
    String[] args;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		this.bundleContext = _bundleContext;
        args = new String[]{};
        if (bundleContext.getProperty("args") != null) {
        	args = bundleContext.getProperty("args").split("[ ]*,[ ]*");
    		if (CommonUtils.getInstance().getArg("-h", args) != null ||
    			CommonUtils.getInstance().getArg("-help", args) != null) { // display all command options
    			System.out.println(APP_NAME + " " + APP_VERSION_FULL);
				System.out.println("Options:");
				System.out.println("-offline - set the URL caching to offline");
				System.out.println("-scriptfile - load a script file");
				System.out.println("-convert - convert the fasta file to bnib");
				System.out.println("-clrprf - clear the preferences");
				System.out.println("-exit - exit the program after completing above functions");
				System.out.println("-single_instance - exits if a running instance of IGB is found");
				System.out.println("Advanced options:");
				System.out.println("-prefsmode - use the specified preferences mode (default \"igb\")");
				System.out.println("-clrallprf - clear all the preferences for all preferences modes");
				System.out.println("-pntprf - print the preferences for this preferences mode in xml format");
				System.out.println("-pntallprf - print all the preferences for all preferences modes in xml format");
				System.out.println("-install_bundle - install an OSGi bundle (plugin) in the specified .jar file");
				System.out.println("-uninstall_bundle - uninstall an installed OSGi bundle (plugin)");
				System.out.println("-cbc - clear bundle cache and exit - this will ignore all other options");
				System.exit(0);
    		}
    		//single instance?
    		if (CommonUtils.getInstance().getArg("-single_instance", args) != null) {
    			if (isIGBRunning()) {
    				System.out.println("\nPort "+CommonUtils.default_server_port+" is in use! An IGB instance is likely running. Sending command to bring IGB to front. Aborting startup.\n");
    				System.exit(0);
    			}
    		}
    		
    		String prefsMode = CommonUtils.getInstance().getArg("-prefsmode", args);
    		if (prefsMode != null) {
    			PreferenceUtils.setPrefsMode(prefsMode);
    		}
    		if (CommonUtils.getInstance().getArg("-convert", args) != null) {
				String[] runArgs = Arrays.copyOfRange(args, 1, args.length);
				NibbleResiduesParser.main(runArgs);
				System.exit(0);
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
    		if (CommonUtils.getInstance().getArg("-exit", args) != null) {
				System.exit(0);
    		}
        }
		// Verify jidesoft license.
		com.jidesoft.utils.Lm.verifyLicense("Dept. of Bioinformatics and Genomics, UNCC",
			"Integrated Genome Browser", ".HAkVzUi29bDFq2wQ6vt2Rb4bqcMi8i1");
    	ServiceReference<IWindowService> windowServiceReference = bundleContext.getServiceReference(IWindowService.class);

        if (windowServiceReference != null)
        {
        	run(windowServiceReference);
        }
        else
        {
        	ServiceTracker<IWindowService, Object> serviceTracker = new ServiceTracker<IWindowService, Object>(bundleContext, IWindowService.class, null) {
        	    public Object addingService(ServiceReference<IWindowService> windowServiceReference) {
        	    	run(windowServiceReference);
        	        return super.addingService(windowServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
		// redisplay FeatureTreeView when FileTypeHandler added / removed
		ExtensionPointHandler<FileTypeHandler> extensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, FileTypeHandler.class);
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

	
	/**Check to see if port 7085, the default IGB bookmarks port is open.  
	 * If so returns true AND send IGBControl a message to bring IGB's JFrame to the front.
	 * If not returns false.
	 * @author davidnix*/
	public boolean isIGBRunning(){
		Socket sock = null;
		int port = CommonUtils.default_server_port;
		try {
		    sock = new Socket("localhost", port);
		    if (sock.isBound()) {
		    	//try to bring to front
		    	URL toSend = new URL ("http://localhost:"+port+"/IGBControl?bringIGBToFront=true");
		    	HttpURLConnection conn = (HttpURLConnection)toSend.openConnection();
		        conn.getResponseMessage();
		    	return true;
		    }
		} catch (Exception e) {
			//Don't do anything. isBound() throws an error when trying to bind a bound port
		} finally {
			try {
				if (sock != null) sock.close();
			} catch (IOException e) {}
		}
		return false;
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
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, GlyphProcessor.class);
    	GenericActionHolder.getInstance().addGenericActionListener(
    		new GenericActionListener() {
				@Override
				public void onCreateGenericAction(GenericAction genericAction) {
					if (genericAction.getText() != null) {//genericAction.getValue(javax.swing.Action.NAME)
						boolean isToolbar = PreferenceUtils.getToolbarNode().getBoolean(genericAction.getId(), false);
						if (isToolbar) {
							JRPButton button; 
							if(genericAction instanceof ChangeColorActionA){
								button = new JRPColoredButton("Toolbar_" + genericAction.getId(), (ChangeColorActionA)genericAction);
								IGBServiceImpl.getInstance().addListSelectionListener((JRPColoredButton)button);
							}else{
								button = new JRPButton("Toolbar_" + genericAction.getId(), genericAction);
							}
							
							button.setHideActionText(true);
							((IGB)Application.getSingleton()).addToolbarButton(button);
						}
					}
				}
				@Override
				public void notifyGenericAction(GenericAction genericAction) {}
			}
    	);
        IWindowService windowService = bundleContext.getService(windowServiceReference);
        final IGB igb = new IGB();
		
		// To avoid race condition on startup
		initMapViewGlyphFactorys();
		
        igb.init(args);
        final IGBTabPanel[] tabs = igb.setWindowService(windowService);
        // set IGBService
		bundleContext.registerService(IGBService.class, IGBServiceImpl.getInstance(), null);
		// register tabs created in IGB itself - IGBTabPanel is an extension point
		for (IGBTabPanel tab : tabs) {
			bundleContext.registerService(IGBTabPanel.class.getName(), tab, null);
		}
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, TrackClickListener.class);
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeSym.class);
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
				}
			},
			null
		);
		initSeqMapViewActions();
	}

	/**
	 * call getAction on all subclasses of SeqMapViewActionA so that they appear in
	 * the toolbar, must be done after SeqMapView is created and assigned to IGB.map_view
	 */
	private void initSeqMapViewActions() {
		RepackSelectedTiersAction.getAction();
		RepackAllTiersAction.getAction();
		ChangeForegroundColorAction.getAction();
		ChangeBackgroundColorAction.getAction();
		ShowStrandAction.getAction();
		CollapseExpandAction.getAction();
		ZoomInXAction.getAction();
		ZoomOutXAction.getAction();
		ZoomInYAction.getAction();
		ZoomOutYAction.getAction();
		ZoomOutFullyAction.getAction();
		ScrollUpAction.getAction();
		ScrollDownAction.getAction();
		ScrollLeftAction.getAction();
		ScrollRightAction.getAction();
	}

	private void initOperators() {
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
		bundleContext.registerService(Operator.class, new com.affymetrix.igb.view.MismatchOperator(), null);
		bundleContext.registerService(Operator.class, new com.affymetrix.igb.view.MismatchPileupOperator(), null);
	}

	private void initMapViewGlyphFactorys() {
		ExtensionPointHandler<MapViewGlyphFactoryI> mapViewGlyphFactoryExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, MapViewGlyphFactoryI.class);
		mapViewGlyphFactoryExtensionPoint.addListener(
			new ExtensionPointListener<MapViewGlyphFactoryI>() {
				@Override
				public void removeService(MapViewGlyphFactoryI factory) {
					MapViewModeHolder.getInstance().removeViewFactory(factory);
				}
				@Override
				public void addService(MapViewGlyphFactoryI factory) {
					MapViewModeHolder.getInstance().addViewFactory(factory);
				}
			}
		);
		
		// Add annot factories
		AnnotationGlyphFactory annotationGlyphFactory = new AnnotationGlyphFactory(FileTypeCategory.Annotation);
		bundleContext.registerService(MapViewGlyphFactoryI.class, annotationGlyphFactory, null);
		AnnotationGlyphFactory alignmentGlyphFactory = new AnnotationGlyphFactory(FileTypeCategory.Alignment);
		bundleContext.registerService(MapViewGlyphFactoryI.class, alignmentGlyphFactory, null);

		// add sequence factory
		SequenceGlyphFactory sequenceGlyphFactory = new SequenceGlyphFactory();
		bundleContext.registerService(MapViewGlyphFactoryI.class, sequenceGlyphFactory, null);

		// Add graph factories
		GraphGlyphFactory barGraphGlyphFactory = new GraphGlyphFactory(BarGraphGlyph.class);
		bundleContext.registerService(MapViewGlyphFactoryI.class, barGraphGlyphFactory, null);
		GraphGlyphFactory dotGraphGlyphFactory = new GraphGlyphFactory(DotGraphGlyph.class);
		bundleContext.registerService(MapViewGlyphFactoryI.class, dotGraphGlyphFactory, null);
		GraphGlyphFactory fillBarGraphGlyphFactory = new GraphGlyphFactory(FillBarGraphGlyph.class);
		bundleContext.registerService(MapViewGlyphFactoryI.class, fillBarGraphGlyphFactory, null);
		GraphGlyphFactory heatMapGraphGlyphFactory = new GraphGlyphFactory(HeatMapGraphGlyph.class);
		bundleContext.registerService(MapViewGlyphFactoryI.class, heatMapGraphGlyphFactory, null);
		GraphGlyphFactory lineGraphGlyphFactory = new GraphGlyphFactory(LineGraphGlyph.class);
		bundleContext.registerService(MapViewGlyphFactoryI.class, lineGraphGlyphFactory, null);
		GraphGlyphFactory minMaxAvgGraphGlyphFactory = new GraphGlyphFactory(MinMaxAvgGraphGlyph.class);
		bundleContext.registerService(MapViewGlyphFactoryI.class, minMaxAvgGraphGlyphFactory, null);
		GraphGlyphFactory stairStepGraphGlyphFactory = new GraphGlyphFactory(StairStepGraphGlyph.class);
		bundleContext.registerService(MapViewGlyphFactoryI.class, stairStepGraphGlyphFactory, null);
		
		// ProbeSet factory
		ProbeSetGlyphFactory probeSet = new ProbeSetGlyphFactory();
		bundleContext.registerService(MapViewGlyphFactoryI.class, probeSet, null);
		
		// Add ScoredContainer factories
		ScoredContainerGlyphFactory scoredBar = new ScoredContainerGlyphFactory(barGraphGlyphFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, scoredBar, null);
		ScoredContainerGlyphFactory scoredDot = new ScoredContainerGlyphFactory(dotGraphGlyphFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, scoredDot, null);
		ScoredContainerGlyphFactory scoredFillBar = new ScoredContainerGlyphFactory(fillBarGraphGlyphFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, scoredFillBar, null);
		ScoredContainerGlyphFactory scoredHeatMap = new ScoredContainerGlyphFactory(heatMapGraphGlyphFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, scoredHeatMap, null);
		ScoredContainerGlyphFactory scoredLine = new ScoredContainerGlyphFactory(lineGraphGlyphFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, scoredLine, null);
		ScoredContainerGlyphFactory scoredMinMaxAvg = new ScoredContainerGlyphFactory(minMaxAvgGraphGlyphFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, scoredMinMaxAvg, null);
		ScoredContainerGlyphFactory scoredStairStep = new ScoredContainerGlyphFactory(stairStepGraphGlyphFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, scoredStairStep, null);
		
		// Add mismatch factories
		MismatchGlyphFactory mismatchGlyphFactory = new MismatchGlyphFactory();
		bundleContext.registerService(MapViewGlyphFactoryI.class, mismatchGlyphFactory, null);

//		bundleContext.registerService(MapViewGlyphFactoryI.class, new OperatorGlyphFactory(new LogTransform(Math.E), new GenericGraphGlyphFactory()));
//		ExpandedAnnotGlyphFactory expandedAnnotGlyphFactory = new ExpandedAnnotGlyphFactory();
//		expandedAnnotGlyphFactory.init(new HashMap<String, Object>());
//		bundleContext.registerService(MapViewGlyphFactoryI.class, expandedAnnotGlyphFactory);
		MapViewGlyphFactoryI alignmentDepthFactory = new OperatorGlyphFactory(new DepthOperator(FileTypeCategory.Alignment), stairStepGraphGlyphFactory);
		MapViewGlyphFactoryI alignmentSemanticZoomGlyphFactory = new DefaultSemanticZoomGlyphFactory(alignmentGlyphFactory, alignmentDepthFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, alignmentSemanticZoomGlyphFactory, null);
		MapViewGlyphFactoryI annotationDepthFactory = new OperatorGlyphFactory(new DepthOperator(FileTypeCategory.Annotation), stairStepGraphGlyphFactory);
		MapViewGlyphFactoryI annotationSemanticZoomGlyphFactory = new DefaultSemanticZoomGlyphFactory(annotationGlyphFactory, annotationDepthFactory);
		bundleContext.registerService(MapViewGlyphFactoryI.class, annotationSemanticZoomGlyphFactory, null);
		bundleContext.registerService(MapViewGlyphFactoryI.class, new BaiSemanticZoomGlyphFactory(alignmentGlyphFactory, stairStepGraphGlyphFactory), null);
		bundleContext.registerService(MapViewGlyphFactoryI.class, new TbiSemanticZoomGlyphFactory(annotationGlyphFactory, stairStepGraphGlyphFactory), null);

		// Add Default factories
		MapViewModeHolder.getInstance().addDefaultFactory(FileTypeCategory.Annotation, annotationGlyphFactory);
		MapViewModeHolder.getInstance().addDefaultFactory(FileTypeCategory.Alignment, alignmentSemanticZoomGlyphFactory);
		MapViewModeHolder.getInstance().addDefaultFactory(FileTypeCategory.Sequence, sequenceGlyphFactory);
		MapViewModeHolder.getInstance().addDefaultFactory(FileTypeCategory.Graph, minMaxAvgGraphGlyphFactory);
		MapViewModeHolder.getInstance().addDefaultFactory(FileTypeCategory.Mismatch, mismatchGlyphFactory);
		MapViewModeHolder.getInstance().addDefaultFactory(FileTypeCategory.ProbeSet, probeSet);
		MapViewModeHolder.getInstance().addDefaultFactory(FileTypeCategory.ScoredContainer, scoredHeatMap);
	}
}
