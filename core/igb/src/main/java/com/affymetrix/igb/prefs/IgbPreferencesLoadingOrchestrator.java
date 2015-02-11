package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.igb.general.ServerList;
import com.lorainelab.igb.preferences.IgbPreferencesService;
import com.lorainelab.igb.preferences.model.DataProvider;
import com.lorainelab.igb.preferences.model.IgbPreferences;
import java.util.List;
import java.util.Optional;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

@Component(name = IgbPreferencesLoadingOrchestrator.COMPONENT_NAME, immediate = true)
public class IgbPreferencesLoadingOrchestrator {

    public static final String COMPONENT_NAME = "PrefsLoader";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IgbPreferencesLoadingOrchestrator.class);

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

        //Load DataProviders from persistence api
        ServerList.getServerInstance().loadServerPrefs();

    }

    private void loadDefaultPrefs() {
        Optional<IgbPreferences> igbPreferences = igbPreferencesService.fromDefaultPreferences();
        loadPreferences(igbPreferences);
    }

    private static void loadPreferences(Optional<IgbPreferences> igbPreferences) {
        if (igbPreferences.isPresent()) {
//            processPluginRepositories(igbPreferences.get().getRepository());
            processDataProviders(igbPreferences.get().getDataProviders());
        }
    }

    private static void processDataProviders(List<DataProvider> dataProviders) {
        //TODO ServerList implementation is suspect and should be replaced
        dataProviders.stream().forEach(dataProvider -> ServerList.getServerInstance().addServer(dataProvider));
        DataLoadPrefsView.getSingleton().refreshServers();
    }

}
