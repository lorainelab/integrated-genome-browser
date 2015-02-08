package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.weblink.WebLink;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.general.ServerList;
import com.lorainelab.igb.preferences.IgbPreferencesService;
import com.lorainelab.igb.preferences.model.AnnotationUrl;
import com.lorainelab.igb.preferences.model.DataProvider;
import com.lorainelab.igb.preferences.model.IgbPreferences;
import com.lorainelab.igb.preferences.model.PluginRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

@Component(name = IgbPreferencesLoadingOrchestrator.COMPONENT_NAME, immediate = true)
public class IgbPreferencesLoadingOrchestrator {

    public static final String COMPONENT_NAME = "PrefsLoader";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IgbPreferencesLoadingOrchestrator.class);
    private static final String COMMAND_KEY = "meta";
    private static final String CONTROL_KEY = "ctrl";
    private IgbPreferencesService igbPreferencesService;

    @Activate
    public void activate(BundleContext bundleContext) {
//        loadIGBPrefs(CommonUtils.getInstance().getArgs(bundleContext));
        loadIGBPrefs();

    }

    @Reference(optional = false)
    public void setIgbPreferencesService(IgbPreferencesService igbPreferencesService) {
        this.igbPreferencesService = igbPreferencesService;
    }

    public void loadIGBPrefs() {
        loadDefaultPrefs();
        loadDefaultToolbarActionsAndKeystrokeBindings();
        //        String remotePreferencesUrl = getRemoteOverridePreferencesFile(args);
        //Note, this functionality seems to have never worked for two reasons...
        //1. remote version of preferences was being cached, so it would not really have received updatesmail
        //2. Once a server is loaded no settings are overwritten by new configuration
        // loadRemotePreferencesOverrideFile(remotePreferencesUrl);

//Load DataProviders and Plugin Repositories from persistence api
        ServerList.getServerInstance().loadServerPrefs();
        ServerList.getRepositoryInstance().loadServerPrefs();

    }

    private void loadDefaultPrefs() {
        Optional<IgbPreferences> igbPreferences = igbPreferencesService.fromDefaultPreferences();
        loadPreferences(igbPreferences);
    }

    private static void loadPreferences(Optional<IgbPreferences> igbPreferences) {
        if (igbPreferences.isPresent()) {
            processPluginRepositories(igbPreferences.get().getRepository());
            processDataProviders(igbPreferences.get().getDataProviders());
            processAnnotationUrls(igbPreferences.get().getAnnotationUrl());

        }
    }

    private static void processDataProviders(List<DataProvider> dataProviders) {
        //TODO ServerList implementation is suspect and should be replaced
        dataProviders.stream().forEach(dataProvider -> ServerList.getServerInstance().addServer(dataProvider));
    }

    private static void processAnnotationUrls(List<AnnotationUrl> annotationUrls) {
        annotationUrls.stream().map((url) -> new WebLink(url)).forEach((webLink) -> {
            WebLinkUtils.getWebLinkList(webLink.getType()).addWebLink(webLink);
        });

    }

    private static void processPluginRepositories(List<PluginRepository> pluginRepositories) {
        pluginRepositories.stream().forEach(pluginRepository -> ServerList.getRepositoryInstance().addServer(pluginRepository));
    }

    private static void loadDefaultToolbarActionsAndKeystrokeBindings() {
        String fileName = IGBConstants.DEFAULT_PREFS_API_RESOURCE;

        /**
         * load default prefs from jar (with Preferences API). This will be the
         * standard method soon.
         */
        try (InputStream default_prefs_stream = IGB.class.getResourceAsStream(fileName);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
            //Save current preferences
            PreferenceUtils.getTopNode().exportSubtree(outputStream);
            if (default_prefs_stream != null) {
                logger.debug("loading default User preferences from: " + fileName);
                Preferences.importPreferences(default_prefs_stream);

                /**
                 * Use 'command' instead of 'control' in keystrokes for Mac OS.
                 */
                if (IGB.IS_MAC) {
                    String[] keys = PreferenceUtils.getKeystrokesNode().keys();
                    for (int i = 0; i < keys.length; i++) {
                        String action = PreferenceUtils.getKeystrokesNode().keys()[i];
                        String keyStroke = PreferenceUtils.getKeystrokesNode().get(action, "");
                        if (keyStroke.contains(CONTROL_KEY)) {
                            keyStroke = keyStroke.replace(CONTROL_KEY, COMMAND_KEY);
                            PreferenceUtils.getKeystrokesNode().put(action, keyStroke);
                        }
                    }
                }
                //Load back saved preferences
                try (ByteArrayInputStream outputInputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                    Preferences.importPreferences(outputInputStream);
                }
            }
        } catch (Exception ex) {
            logger.debug("Problem parsing prefs from: {}", fileName, ex);
        }
    }

//    private void loadRemotePreferencesOverrideFile(String remotePreferencesUrl) {
//        try (InputStream prefs_url_stream = Resources.asByteSource(new URL(remotePreferencesUrl)).openStream();
//                InputStreamReader reader = new InputStreamReader(prefs_url_stream)) {
//            logger.debug("loading prefs from url: " + remotePreferencesUrl);
//
//            loadPreferences(igbPreferencesService.fromJson(reader));
//        } catch (IOException ex) {
//            logger.error("Problem parsing prefs from url: {}", remotePreferencesUrl, ex);
//        }
//    }
//    private static String getRemoteOverridePreferencesFile(String[] args) {
//        String remotePreferencesUrl = CommonUtils.getInstance().getArg("-prefs", args);
//        return remotePreferencesUrl;
//    }
}
