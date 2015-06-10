package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderUtils;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.EventService;
import com.affymetrix.igb.general.DataProviderManager;
import com.affymetrix.igb.general.DataProviderManager.DataProviderServiceChangeEvent;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.lorainelab.igb.preferences.IgbPreferencesService;
import com.lorainelab.igb.preferences.model.DataProviderConfig;
import com.lorainelab.igb.preferences.model.IgbPreferences;
import java.awt.event.ActionEvent;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.Timer;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = IgbPreferencesLoadingOrchestrator.COMPONENT_NAME, immediate = true)
public class IgbPreferencesLoadingOrchestrator {

    public static final String COMPONENT_NAME = "PrefsLoader";
    private static final Logger logger = LoggerFactory.getLogger(IgbPreferencesLoadingOrchestrator.class);
    private IgbPreferencesService igbPreferencesService;
    private DataProviderManager dataProviderManager;
    private EventService eventService;
    private EventBus eventBus;

    @Activate
    public void activate(BundleContext bundleContext) {
        eventBus = eventService.getEventBus();
        eventBus.register(this);
        loadIGBPrefs();
    }

    @Reference
    public void setDataProviderManager(DataProviderManager dataProviderManager) {
        this.dataProviderManager = dataProviderManager;
    }

    @Reference(optional = false)
    public void setIgbPreferencesService(IgbPreferencesService igbPreferencesService) {
        this.igbPreferencesService = igbPreferencesService;
    }

    @Reference
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public void loadIGBPrefs() {
        loadDefaultPrefs();
        migrateOldDataProviders();
        //Load from persistence api
        loadFromPersistenceStorage();
        notifyUserOfNotRespondingServers();
    }

    private void loadDefaultPrefs() {
        Optional<IgbPreferences> igbPreferences = igbPreferencesService.fromDefaultPreferences();
        loadPreferences(igbPreferences);

    }

    private void loadPreferences(Optional<IgbPreferences> igbPreferences) {
        if (igbPreferences.isPresent()) {
            processDataProviders(igbPreferences.get().getDataProviders());
        }
    }

    private void processDataProviders(List<DataProviderConfig> dataProviders) {
        //TODO ServerList implementation is suspect and should be replaced
        dataProviders.stream().distinct().forEach(dataProvider -> {
            dataProviderManager.initializeDataProvider(dataProvider);
            String externalFormUrl = DataProviderUtils.toExternalForm(dataProvider.getUrl());
            PreferenceUtils.getDataProviderNode(externalFormUrl).putBoolean(DataProviderPrefKeys.IS_EDITABLE, dataProvider.isEditable());
        });
    }

    private void loadFromPersistenceStorage() {
        logger.info("Loading server preferences from the Java preferences subsystem");
        try {
            Arrays.stream(PreferenceUtils.getDataProvidersNode().childrenNames())
                    .map(nodeName -> PreferenceUtils.getDataProvidersNode().node(nodeName))
                    .forEach(dataProviderManager::initializeDataProvider);
        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        }
        logger.info("Completed loading server preferences from the Java preferences subsystem");
    }

    private void notifyUserOfNotRespondingServers() {
        //wait 2 seconds for all servers to have time to initialize
        Timer timer = new Timer(2000, (ActionEvent e) -> {
            Set<String> notRespondingServerNames = DataProviderManager.getAllServers().stream()
                    .filter(server -> server.getStatus() == LoadUtils.ResourceStatus.NotResponding)
                    .map(server -> server.getName()).collect(Collectors.toSet());
            if (!notRespondingServerNames.isEmpty()) {
                ModalUtils.infoPanel("The following servers are not responding: " + System.lineSeparator() + Joiner.on(System.lineSeparator()).join(notRespondingServerNames));
            }
            DataProviderManager.ALL_SOURCES_INITIALIZED = true;
            eventBus.post(new DataProviderServiceChangeEvent());
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void migrateOldDataProviders() {
        Set<DataProvider> loadedDataProviders = DataProviderManager.getAllServers();
        List<String> URL_IGNORE_LIST = ImmutableList.of("http://www.ensembl.org/das/dsn/", "http://bioviz.org/cached/", "http://genome.cse.ucsc.edu/cgi-bin/das/dsn/");
        try {
            logger.info("Loading old data providers from preferences");
            for (String nodeName : PreferenceUtils.getOldServersNode().childrenNames()) {
                Preferences node = PreferenceUtils.getOldServersNode().node(nodeName);
                String url = addTrailingSlash(GeneralUtils.URLDecode(node.get("url", "")));
                boolean nodeRemoved = false;
                for (DataProvider dataProvider : loadedDataProviders) {
                    String dataProviderUrl = addTrailingSlash(dataProvider.getUrl());

                    if (dataProviderUrl.equals(url)) {
                        node.removeNode();
                        nodeRemoved = true;
                        break;
                    }
                }
                if (!nodeRemoved && !URL_IGNORE_LIST.contains(URLDecoder.decode(url, "UTF-8"))) {
                    Preferences newDataProviderNode = PreferenceUtils.getDataProviderNode(url);
                    newDataProviderNode.put("factoryName", node.get("type", ""));
                    newDataProviderNode.put("loadPriority", node.get("order", "1"));
                    newDataProviderNode.put("name", node.get("name", "unknown"));
                    String enabled = node.get("enabled", "true");
                    if (!Boolean.valueOf(enabled)) {
                        newDataProviderNode.put("status", "Disabled");
                    }
                    newDataProviderNode.put("url", url);
                    node.removeNode();
                }
            }
        } catch (Exception ex) {
        }
    }

    private String addTrailingSlash(String url) {
        return url + (url.endsWith("/") ? "" : "/");
    }
}
