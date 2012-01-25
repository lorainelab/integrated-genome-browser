package com.affymetrix.igb;

import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.APP_VERSION_FULL;

import java.util.Arrays;

import javax.swing.ImageIcon;

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
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperator;
import com.affymetrix.genometryImpl.operator.graph.GraphOperator;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.glyph.MismatchPileupGlyphProcessor;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.window.service.IWindowService;
import com.affymetrix.igb.shared.ExtendedMapViewGlyphFactoryI;
import com.affymetrix.igb.shared.GlyphProcessor;
import com.affymetrix.igb.shared.ISearchModeGlyph;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.TrackClickListener;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;

/**
 * OSGi Activator for igb bundle
 */
public class Activator implements BundleActivator {
	private static final String DEFAULT_ICON_PATH = "toolbarButtonGraphics/general/TipOfTheDay16.gif";
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
		ExtensionPointHandler<GlyphProcessor> glyphProcessorExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, GlyphProcessor.class);
		glyphProcessorExtensionPoint.addExtensionPointImpl(new MismatchPileupGlyphProcessor());
    	GenericActionHolder.getInstance().addGenericActionListener(
    		new GenericActionListener() {
				@Override
				public void onCreateGenericAction(GenericAction genericAction) {
					if (genericAction.getText() != null) {
						boolean isToolbar = PreferenceUtils.getToolbarNode().getBoolean(genericAction.getText(), false);
						if (isToolbar) {
							String iconPath = genericAction.getIconPath();
							if (iconPath == null) {
								iconPath = DEFAULT_ICON_PATH;
							}
							ImageIcon icon = CommonUtils.getInstance().getIcon(iconPath);
							JRPButton button = new JRPButton("Toolbar_" + genericAction.getId(), icon);
							button.addActionListener(genericAction);
							button.setToolTipText(genericAction.getText());
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
        igb.init(args);
        final IGBTabPanel[] tabs = igb.setWindowService(windowService);
        // set IGBService
		bundleContext.registerService(IGBService.class, IGBServiceImpl.getInstance(), null);
		// register tabs created in IGB itself - IGBTabPanel is an extension point
		for (IGBTabPanel tab : tabs) {
			bundleContext.registerService(IGBTabPanel.class.getName(), tab, null);
		}
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, TrackClickListener.class);
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, AnnotationOperator.class);
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ExtendedMapViewGlyphFactoryI.class);
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, GraphOperator.class);
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeSym.class);
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeGlyph.class);
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
	}
}
