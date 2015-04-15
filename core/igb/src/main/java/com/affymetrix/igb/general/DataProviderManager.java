package com.affymetrix.igb.general;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.data.DataProviderFactoryManager;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.FACTORY_NAME;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOAD_PRIORITY;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOGIN;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.MIRROR_URL;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PASSWORD;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PRIMARY_URL;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PROVIDER_NAME;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.STATUS;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Disabled;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.NotResponding;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.StringEncrypter;
import com.affymetrix.igb.EventService;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.lorainelab.igb.preferences.model.DataProviderConfig;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = DataProviderManager.COMPONENT_NAME, immediate = true, provide = DataProviderManager.class)
public class DataProviderManager {

    public static final String COMPONENT_NAME = "DataProviderManager";
    private static final Logger logger = LoggerFactory.getLogger(DataProviderManager.class);
    private static final Set<DataProvider> dataProviders = Sets.newConcurrentHashSet();
    private DataProviderFactoryManager dataProviderFactoryManager;
    private BundleContext bundleContext;
    private StringEncrypter encrypter;
    private final Map<String, ServiceReference> dataProviderServiceReferences;
    private EventService eventService;
    private EventBus eventBus;

    public DataProviderManager() {
        dataProviderServiceReferences = Maps.newConcurrentMap();
        initStringEncrypter();
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private void initStringEncrypter() {
        try {
            encrypter = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
        } catch (StringEncrypter.EncryptionException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Reference
    public void setDataProviderManager(DataProviderFactoryManager dataProviderManager) {
        this.dataProviderFactoryManager = dataProviderManager;
    }

    @Reference
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
        eventBus = eventService.getEventBus();
        eventBus.register(this);
    }

    //TODO remove when possible to remove static from dataProviders
    public static Optional<DataProvider> getServerFromUrlStatic(String url) {
        return dataProviders.stream().filter(dp -> dp.getUrl().equals(url)).findFirst();
    }

    public Optional<DataProvider> getServerFromUrl(String url) {
        return dataProviders.stream().filter(dp -> dp.getUrl().equals(url)).findFirst();
    }

    public boolean areAllServersInited() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static Set<DataProvider> getEnabledDataProviders() {
        return dataProviders.stream()
                .filter(gv -> gv.getStatus() == Disabled)
                .filter(gv -> gv.getStatus() == NotResponding)
                .collect(Collectors.toSet());
    }

    public static Set<String> getDataProvidersSupportingUserInstances() {
        return dataProviders.stream()
                .filter(dataProvider -> dataProvider instanceof DataProviderFactory)
                .map(dataProvider -> dataProvider.getClass().getName())
                .collect(Collectors.toSet());
    }

    public static Set<DataProvider> getEnabledServers() {
        return getEnabledDataProviders();
    }

    public static Set<DataProvider> getAllServers() {
        return dataProviders;
    }

    //TODO this node parsing should be pushed up to factories to allow more flexibility and more isolation of responsibility
    public void addServer(Preferences node) {
        String url = node.get(PRIMARY_URL, null);
        String name = node.get(PROVIDER_NAME, null);
        String factoryName = node.get(FACTORY_NAME, null);
        String login = node.get(LOGIN, null);
        String password = node.get(PASSWORD, null);
        String mirrorUrl = node.get(MIRROR_URL, null);
        String status = node.get(STATUS, null);
        int loadPriority = node.getInt(LOAD_PRIORITY, -1);

        if (!Strings.isNullOrEmpty(url) && !Strings.isNullOrEmpty(factoryName) && !Strings.isNullOrEmpty(name) && !dataProviderServiceReferences.containsKey(url)) {
            Optional<DataProviderFactory> dataProviderFactory = dataProviderFactoryManager.findFactoryByName(factoryName);
            dataProviderFactory.ifPresent(factory -> {
                DataProvider dataProvider;
                if (Strings.isNullOrEmpty(mirrorUrl)) {
                    dataProvider = factory.createDataProvider(url, name, loadPriority);
                } else {
                    dataProvider = factory.createDataProvider(url, name, mirrorUrl, loadPriority);
                }
                if (!Strings.isNullOrEmpty(login)) {
                    dataProvider.setLogin(login);
                }
                if (!Strings.isNullOrEmpty(password)) {
                    dataProvider.setPassword(encrypter.decrypt(password));
                }
                if (!Strings.isNullOrEmpty(status)) {
                    dataProvider.setStatus(ResourceStatus.valueOf(status));
                }
                if (loadPriority != -1) {
                    dataProvider.setLoadPriority(loadPriority);
                }
                ServiceRegistration<DataProvider> registerService = bundleContext.registerService(DataProvider.class, dataProvider, null);
                dataProviderServiceReferences.put(dataProvider.getUrl(), registerService.getReference());
            });
        }
        eventBus.post(new DataProviderServiceChangeEvent());
    }

    public void addServer(DataProviderConfig config) {
        String factoryName = config.getFactoryName();
        Optional<DataProviderFactory> dataProviderFactory = dataProviderFactoryManager.findFactoryByName(factoryName);
        dataProviderFactory.ifPresent(factory -> {
            DataProvider dataProvider;
            if (Strings.isNullOrEmpty(config.getMirror())) {
                dataProvider = factory.createDataProvider(config.getUrl(), config.getName(), config.getLoadPriority());
            } else {
                dataProvider = factory.createDataProvider(config.getUrl(), config.getName(), config.getMirror(), config.getLoadPriority());
            }
            ServiceRegistration<DataProvider> registerService = bundleContext.registerService(DataProvider.class, dataProvider, null);
            dataProviderServiceReferences.put(dataProvider.getUrl(), registerService.getReference());
        });
        eventBus.post(new DataProviderServiceChangeEvent());
    }

    //TODO move to service tracker
    @Reference(optional = true, multiple = true, dynamic = true, unbind = "removeDataProvider")
    public void addDataProvider(DataProvider dataProvider) {
        dataProviders.add(dataProvider);
        eventBus.post(new DataProviderServiceChangeEvent());
    }

    public void removeDataProvider(DataProvider dataProvider) {
        try {
            dataProviders.remove(dataProvider);
            eventBus.post(new DataProviderServiceChangeEvent());
            PreferenceUtils.getDataProviderNode(dataProvider.getUrl()).removeNode();

        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void removeServer(String url) {
        bundleContext.ungetService(dataProviderServiceReferences.remove(url));
    }

    public void setServerOrder(String url, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Comparator getServerOrderComparator() {
        Comparator t = (Comparator<DataProvider>) (DataProvider o1, DataProvider o2) -> {
            return Integer.compare(o1.getLoadPriority(), o2.getLoadPriority());
        };
        return t;
    }

    public static class DataProviderServiceChangeEvent {
    }
}
