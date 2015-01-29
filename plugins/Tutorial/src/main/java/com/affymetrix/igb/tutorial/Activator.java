package com.affymetrix.igb.tutorial;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import javax.swing.JMenuItem;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.service.api.SimpleServiceRegistrar;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import com.affymetrix.igb.window.service.IWindowService;
import org.slf4j.LoggerFactory;

public class Activator extends SimpleServiceRegistrar implements BundleActivator {

    private static final String DEFAULT_PREFS_TUTORIAL_RESOURCE = "/tutorial_default_prefs.xml";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Activator.class);

    /**
     * Load default prefs from jar (with Preferences API). This will be the
     * standard method soon.
     */
    private void loadDefaultTutorialPrefs() {

        InputStream default_prefs_stream = null;
        try {
            logger.debug("loading default tutorial preferences from: {}",
                    DEFAULT_PREFS_TUTORIAL_RESOURCE);
            default_prefs_stream = Activator.class.getResourceAsStream(DEFAULT_PREFS_TUTORIAL_RESOURCE);
            Preferences.importPreferences(default_prefs_stream);
            //prefs_parser.parse(default_prefs_stream, "", prefs_hash);
        } catch (InvalidPreferencesFormatException ex) {
            logger.error( DEFAULT_PREFS_TUTORIAL_RESOURCE, ex);
        } catch (IOException ex) {
            logger.error( DEFAULT_PREFS_TUTORIAL_RESOURCE, ex);
        } finally {
            GeneralUtils.safeClose(default_prefs_stream);
        }
    }

    private Preferences getTopNode() {
        return Preferences.userRoot().node("/com/affymetrix/igb");
    }

    private Preferences getTutorialsNode() {
        return getTopNode().node("tutorials");
    }

    private void initActions() {
        TweeningZoomAction.getAction();
        VerticalStretchZoomAction.getAction();
    }

    private ServiceRegistration<?>[] getServices(final BundleContext bundleContext,
            final IgbService igbService, final IWindowService windowService) throws Exception {
        final TutorialManager tutorialManager = new TutorialManager(igbService, windowService);
        GenericActionHolder.getInstance().addGenericActionListener(tutorialManager);
        JRPMenu tutorialMenu = new JRPMenu("Tutorial_tutorialMenu", "Tutorials");
        Properties tutorials = new Properties();
        loadDefaultTutorialPrefs();
        Preferences tutorialsNode = getTutorialsNode();

        try {
            for (String key : tutorialsNode.keys()) {
                String tutorialUri = tutorialsNode.get(key, null);
                tutorials.clear();
                tutorials.load(new URL(tutorialUri + "/tutorials.properties").openStream());
                Enumeration<?> tutorialNames = tutorials.propertyNames();
                while (tutorialNames.hasMoreElements()) {
                    String name = (String) tutorialNames.nextElement();
                    String description = (String) tutorials.get(name);
                    RunTutorialAction rta = new RunTutorialAction(tutorialManager, description, tutorialUri + "/" + name);
                    JMenuItem item = new JMenuItem(rta);
                    tutorialMenu.add(item);
                }
            }

            return new ServiceRegistration[]{bundleContext.registerService(AMenuItem.class, new AMenuItem(tutorialMenu, "help"), null)};

        } catch (FileNotFoundException fnfe) {
            logger.error( "Could not find file {0}.\n          coninuing...", fnfe.getMessage());
        } catch (java.net.ConnectException ce) {
            logger.error( "Could not connect: {0}.\n          coninuing...", ce.getMessage());
        } catch (Exception ex) {
            logger.error( "Could not connect: {0}.\n          coninuing...", ex.getMessage());
        }

        return null;
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext) throws Exception {
        initActions();

        XServiceRegistrar<IgbService> igbServiceRegistrar = new XServiceRegistrar<IgbService>(IgbService.class) {

            @Override
            protected ServiceRegistration<?>[] getServices(final BundleContext bundleContext, final IgbService igbService) throws Exception {

                XServiceRegistrar<IWindowService> windowServiceRegistrar = new XServiceRegistrar<IWindowService>(IWindowService.class) {

                    @Override
                    protected ServiceRegistration<?>[] getServices(final BundleContext bundleContext, final IWindowService windowService) throws Exception {
                        return Activator.this.getServices(bundleContext, igbService, windowService);
                    }
                };
                windowServiceRegistrar.start(bundleContext);
                return new ServiceRegistration[]{bundleContext.registerService(BundleActivator.class, windowServiceRegistrar, null)};
            }
        };

        igbServiceRegistrar.start(bundleContext);
        return new ServiceRegistration[]{bundleContext.registerService(BundleActivator.class, igbServiceRegistrar, null)};
    }
}
