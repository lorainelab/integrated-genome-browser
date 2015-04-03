package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.general.GenericServer;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.SERVER_URL;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.general.ServerList;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.lorainelab.igb.preferences.IgbPreferencesService;
import com.lorainelab.igb.preferences.model.DataProvider;
import com.lorainelab.igb.preferences.model.IgbPreferences;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

@Component(name = IgbPreferencesLoadingOrchestrator.COMPONENT_NAME, immediate = true)
public class IgbPreferencesLoadingOrchestrator {

    public static final String COMPONENT_NAME = "PrefsLoader";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IgbPreferencesLoadingOrchestrator.class);
    private static final Set<String> retiredServers = ImmutableSet.<String>of("http://bioviz.org/cached/");

    private IgbPreferencesService igbPreferencesService;

    @Activate
    public void activate(BundleContext bundleContext) {
        loadIGBPrefs();
    }

    @Reference(optional = false)
    public void setIgbPreferencesService(IgbPreferencesService igbPreferencesService) {
        this.igbPreferencesService = igbPreferencesService;
    }

    public void loadIGBPrefs() {
        loadDefaultPrefs();

        //Temporary migration step... can be removed after a few release cycles
        loadOldPreferences();

        //Filter deprecated servers from preferences
        filterRetiredServersFromPrefs();

        //Load from persistence api
        loadFromPersistenceStorage();
        DataLoadPrefsView.getSingleton().refreshServers();
    }

    private void loadOldPreferences() {
        Collection<GenericServer> loadedServers = ServerList.getServerInstance().getAllServers();
        try {
            for (String nodeName : PreferenceUtils.getOldServersNode().childrenNames()) {
                Preferences node = PreferenceUtils.getOldServersNode().node(nodeName);
                String url = GeneralUtils.URLDecode(node.get("url", ""));
                boolean nodeRemoved = false;
                for (GenericServer server : loadedServers) {
                    if (server.getUrlString().equals(url)) {
                        node.removeNode();
                        nodeRemoved = true;
                        break;
                    }
                }
                if (!nodeRemoved && isValidUrl(url)) {
                    DataProvider dataProvider = new DataProvider();
                    dataProvider.setUrl(url);
                    dataProvider.setDefault(Boolean.FALSE.toString());
                    dataProvider.setEnabled(node.get("enabled", "false"));
                    dataProvider.setOrder(node.getInt("order", 1));
                    dataProvider.setName(node.get("name", "unknown"));
                    dataProvider.setType(node.get("type", ""));
                    dataProvider.setPassword(node.get("password", ""));
                    ServerList.getServerInstance().addServer(dataProvider);
                }
                node.removeNode();
            }
        } catch (BackingStoreException ex) {
            logger.error("Error migrating old datasource providers to new format", ex);
        } catch (IllegalStateException ex) {
            //do nothing for node already removed illegalstateexceptions
        }

    }

    private boolean isValidUrl(String url) {
        try {
            URL testUrl = new URL(url);
            return true;
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    private void loadDefaultPrefs() {
        Optional<IgbPreferences> igbPreferences = igbPreferencesService.fromDefaultPreferences();
        loadPreferences(igbPreferences);
    }

    private static void loadPreferences(Optional<IgbPreferences> igbPreferences) {
        if (igbPreferences.isPresent()) {
            processDataProviders(igbPreferences.get().getDataProviders());
        }
    }

    private static void processDataProviders(List<DataProvider> dataProviders) {
        //TODO ServerList implementation is suspect and should be replaced
        dataProviders.stream().distinct().forEach(ServerList.getServerInstance()::addServer);
    }

    private void filterRetiredServersFromPrefs() {
        try {
            Arrays.stream(PreferenceUtils.getServersNode().childrenNames())
                    .map(nodeName -> PreferenceUtils.getServersNode().node(nodeName))
                    .filter(node -> retiredServers.contains(GeneralUtils.URLDecode(node.get("url", ""))))
                    .forEach(node -> {
                        try {
                            node.removeNode();
                        } catch (BackingStoreException ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    });

        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void loadFromPersistenceStorage() {
        logger.info("Loading server preferences from the Java preferences subsystem");
        try {
            Arrays.stream(PreferenceUtils.getServersNode().childrenNames())
                    .map(nodeName -> PreferenceUtils.getServersNode().node(nodeName))
                    .filter(node -> Strings.isNullOrEmpty(node.get(SERVER_URL, "")))
                    .forEach(ServerList.getServerInstance()::addServer);
        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        }
        logger.info("Completed loading server preferences from the Java preferences subsystem");
    }

}
