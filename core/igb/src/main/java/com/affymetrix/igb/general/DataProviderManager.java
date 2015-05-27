package com.affymetrix.igb.general;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.data.DataProviderFactoryManager;
import com.affymetrix.genometry.data.GenomeVersionProvider;
import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceResource;
import com.affymetrix.genometry.general.DataContainer;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.FACTORY_NAME;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOAD_PRIORITY;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOGIN;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.MIRROR_URL;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PASSWORD;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PRIMARY_URL;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PROVIDER_NAME;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.STATUS;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Disabled;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Initialized;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.NotResponding;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.SpeciesLookup;
import com.affymetrix.genometry.util.StringEncrypter;
import static com.affymetrix.genometry.util.StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
import com.affymetrix.genometry.util.SynonymLookup;
import com.affymetrix.igb.EventService;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.lorainelab.igb.preferences.model.DataProviderConfig;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
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
    public static boolean ALL_SOURCES_INITIALIZED = false;
    private static final Logger logger = LoggerFactory.getLogger(DataProviderManager.class);
    private static final Set<DataProvider> dataProviders = Sets.newConcurrentHashSet();
    private static final Set<AssemblyProvider> assemblyProviders = Sets.newConcurrentHashSet();
    private static final Set<ReferenceSequenceResource> referenceSequenceResources = Sets.newConcurrentHashSet();
    private static final GenometryModel gmodel = GenometryModel.getInstance();
    private DataProviderFactoryManager dataProviderFactoryManager;
    private BundleContext bundleContext;
    private final StringEncrypter encrypter;
    private final Map<String, ServiceReference> dataProviderServiceReferences;
    private EventService eventService;
    private GeneralLoadView loadView;
    private EventBus eventBus;

    public DataProviderManager() {
        loadView = GeneralLoadView.getLoadView();
        dataProviderServiceReferences = Maps.newConcurrentMap();
        encrypter = new StringEncrypter(DESEDE_ENCRYPTION_SCHEME);
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        eventBus = eventService.getEventBus();
        eventBus.register(this);
    }

    @Reference
    public void setDataProviderManager(DataProviderFactoryManager dataProviderManager) {
        this.dataProviderFactoryManager = dataProviderManager;
    }

    @Reference
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    //TODO remove when possible to remove static from dataProviders
    public static Optional<DataProvider> getServerFromUrlStatic(String searchUrl) {
        SortedMap<Integer, DataProvider> bestMatchMap = Maps.newTreeMap();
        dataProviders.stream().forEach(dataProvider -> {
            bestMatchMap.put(longestSubstr(dataProvider.getUrl(), searchUrl), dataProvider);
        });
        int maxKey = bestMatchMap.lastKey();
        if (maxKey == 0) {
            return Optional.empty();
        } else {
            DataProvider bestMatch = bestMatchMap.get(maxKey);
            String host = null;
            String bestMatchHost = null;
            try {
                URL url = new URL(searchUrl);
                URL bestMatchUrl = new URL(bestMatch.getUrl());
                host = url.getHost();
                bestMatchHost = bestMatchUrl.getHost();
                if (host.equals(bestMatchHost)) {
                    return Optional.of(bestMatchMap.get(maxKey));
                }
            } catch (MalformedURLException ex) {
            }
            return Optional.empty();

        }
    }

    public Optional<DataProvider> getServerFromUrl(String url) {
        return dataProviders.stream().filter(dp -> dp.getUrl().equals(url)).findFirst();
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
        return ImmutableSet.copyOf(dataProviders);
    }

    public static Set<AssemblyProvider> getAllAssemblyProviders() {
        return assemblyProviders;
    }

    public static Set<ReferenceSequenceResource> getAllReferenceSequenceResources() {
        return referenceSequenceResources;
    }

    //TODO this node parsing should be pushed up to factories to allow more flexibility and more isolation of responsibility
    public void initializeDataProvider(Preferences node) {
        String url = node.get(PRIMARY_URL, null);
        String name = node.get(PROVIDER_NAME, null);
        String factoryName = node.get(FACTORY_NAME, null);
        String login = node.get(LOGIN, null);
        String password = node.get(PASSWORD, null);
        String mirrorUrl = node.get(MIRROR_URL, null);
        String status = node.get(STATUS, null);
        int loadPriority = node.getInt(LOAD_PRIORITY, -1);

        if (isValidNonDuplicate(url, factoryName, name, mirrorUrl)) {
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
                    dataProvider.setStatus(ResourceStatus.fromName(status).get());
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

    private boolean isValidNonDuplicate(String url, String factoryName, String name, String mirrorUrl) {
        if (Strings.isNullOrEmpty(url) || Strings.isNullOrEmpty(factoryName) || Strings.isNullOrEmpty(name)) {
            return false;
        }
        if (Strings.isNullOrEmpty(mirrorUrl)) {
            return !dataProviderServiceReferences.containsKey(url);
        } else {
            return !dataProviderServiceReferences.containsKey(url) && !dataProviderServiceReferences.containsKey(mirrorUrl);
        }
    }

    public void initializeDataProvider(DataProviderConfig config) {
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

    @Reference(optional = true, multiple = true, dynamic = true, unbind = "removeDataProvider")
    public void addDataProvider(DataProvider dataProvider) {
        dataProviders.add(dataProvider);
        DataProviderManager.this.initializeDataProvider(dataProvider);
        if (ALL_SOURCES_INITIALIZED) {
            eventBus.post(new DataProviderServiceChangeEvent());
        }
    }

    private void initializeDataProvider(DataProvider dataProvider) {
        dataProvider.initialize();
        if (dataProvider.getStatus() == Initialized) {
            //TODO don't assume GenomeVersionProvider instances are all derrived from DataProvider instances... a separate whiteboard/service listener would improve design.
            if (dataProvider instanceof GenomeVersionProvider) {
                GenomeVersionProvider genomeVersionProvider = GenomeVersionProvider.class.cast(dataProvider);
                loadGenomeVersionSynonyms(genomeVersionProvider);
                loadSpeciesInfo(genomeVersionProvider);
                loadSupportedGenomeVersions(genomeVersionProvider);
            }
            //TODO don't assume AssemblyProvider instances are all derrived from DataProvider instances, but skip this detail for now since it can wait for feature parity with old code
            if (dataProvider instanceof AssemblyProvider) {
                assemblyProviders.add((AssemblyProvider) dataProvider);
                loadAssemblyData((AssemblyProvider) dataProvider);
            }
            //TODO don't assume ReferenceSequenceResource instances are all derrived from DataProvider instances, but skip this detail for now since it can wait for feature parity with old code
            if (dataProvider instanceof ReferenceSequenceResource) {
                referenceSequenceResources.add((ReferenceSequenceResource) dataProvider);
                loadReferenceSequenceData((ReferenceSequenceResource) dataProvider);
            }
            dataProviders.stream().filter(dp -> !(dp instanceof GenomeVersionProvider)).forEach(this::createSupportingDataContainers);
        }
    }

    public void removeDataProvider(DataProvider dataProvider) {
        try {
            dataProviders.remove(dataProvider);
            //TODO don't assume AssemblyProvider instances are all derrived from DataProvider instances, but skip this detail for now since it can wait for feature parity with old code
            if (dataProvider instanceof AssemblyProvider) {
                assemblyProviders.remove((AssemblyProvider) dataProvider);
            }
            //TODO don't assume ReferenceSequenceResource instances are all derrived from DataProvider instances, but skip this detail for now since it can wait for feature parity with old code
            if (dataProvider instanceof ReferenceSequenceResource) {
                referenceSequenceResources.remove((ReferenceSequenceResource) dataProvider);
            }
            handleSinglePatternCausedRaceCondition();
            GeneralLoadUtils.getAllDataSets().stream()
                    .filter(ds -> ds.getDataContainer().getDataProvider() == dataProvider)
                    .forEach(ds -> {
                        loadView.removeDataSet(ds, true);
                        ds.getDataContainer().getGenomeVersion().removeDataContainer(ds.getDataContainer());
                    });

            PreferenceUtils.getDataProviderNode(dataProvider.getUrl()).removeNode();
        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        }
        eventBus.post(new DataProviderServiceChangeEvent());
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

    private void loadAssemblyData(AssemblyProvider assemblyProvider) {

    }

    private void loadReferenceSequenceData(ReferenceSequenceResource referenceSequenceResource) {
//ReferenceSequenceProvider
        //ReferenceSequenceDataSetProvider
    }

    private void loadSpeciesInfo(GenomeVersionProvider genomeVersionProvider) {
        genomeVersionProvider.getSpeciesInfo().ifPresent(speciesInfo -> {
            speciesInfo.stream().forEach(SpeciesLookup::load);
        });
    }

    private void loadGenomeVersionSynonyms(GenomeVersionProvider genomeVersionProvider) {
        genomeVersionProvider.getGenomeVersionSynonyms().ifPresent(genomeVersionSynonyms -> {
            genomeVersionSynonyms.keySet().stream().forEach(key -> {
                SynonymLookup.getDefaultLookup().getPreferredNames().add(key);
                SynonymLookup.getDefaultLookup().addSynonyms(Sets.newConcurrentHashSet(genomeVersionSynonyms.get(key)));
            });
        }
        );
    }

    private void loadSupportedGenomeVersions(GenomeVersionProvider genomeVersionProvider) {
        for (String genomeVersionName : genomeVersionProvider.getSupportedGenomeVersionNames()) {
            String genomeName = SynonymLookup.getDefaultLookup().findMatchingSynonym(gmodel.getSeqGroupNames(), genomeVersionName);
            String versionName, speciesName;
            GenomeVersion genomeVersion;
            genomeVersion = gmodel.addGenomeVersion(genomeName);
            Optional<String> genomeVersionDescription = ((GenomeVersionProvider) genomeVersionProvider).getGenomeVersionDescription(genomeVersionName);
            genomeVersionDescription.ifPresent(description -> genomeVersion.setDescription(description));
            Set<DataContainer> availableContainers = genomeVersion.getAvailableDataContainers();
            if (!availableContainers.isEmpty()) {
                versionName = GeneralUtils.getPreferredVersionName(availableContainers);
                speciesName = GeneralLoadUtils.getVersionName2Species().get(versionName);
            } else {
                versionName = genomeName;
                speciesName = SpeciesLookup.getSpeciesName(genomeName);
            }
            GeneralLoadUtils.retrieveDataContainer((DataProvider) genomeVersionProvider, speciesName, versionName);
        }
    }

    private void createSupportingDataContainers(DataProvider dataProvider) {
        for (String genomeVersionName : dataProvider.getAvailableGenomeVersionNames()) {
            String genomeName = SynonymLookup.getDefaultLookup().findMatchingSynonym(gmodel.getSeqGroupNames(), genomeVersionName);
            String versionName, speciesName;
            GenomeVersion genomeVersion = gmodel.getSeqGroup(genomeName);
            if (genomeVersion == null) {
                continue;
            }
            Set<DataContainer> availableContainers = genomeVersion.getAvailableDataContainers();
            if (!availableContainers.isEmpty()) {
                versionName = GeneralUtils.getPreferredVersionName(availableContainers);
                speciesName = GeneralLoadUtils.getVersionName2Species().get(versionName);
            } else {
                versionName = genomeName;
                speciesName = SpeciesLookup.getSpeciesName(genomeName);
            }
            GeneralLoadUtils.retrieveDataContainer(dataProvider, speciesName, versionName);
        }
    }

    public void disableDataProvider(DataProvider dataProvider) {
        final Set<DataSet> allDataSets = GeneralLoadUtils.getAllDataSets();
        handleSinglePatternCausedRaceCondition();
        Set<DataContainer> allAssociatedDataContainers = Sets.newHashSet();
        //remove all data sets
        allDataSets.stream()
                .filter(ds -> ds.getDataContainer().getDataProvider() == dataProvider)
                .forEach(ds -> {
                    allAssociatedDataContainers.add(ds.getDataContainer());
                    loadView.removeDataSet(ds, true);
                });
        allAssociatedDataContainers.forEach(dc -> dc.setIsInitialized(false));
        dataProvider.setStatus(ResourceStatus.Disabled);
    }

    public void enableDataProvider(DataProvider dataProvider) {
        dataProvider.setStatus(ResourceStatus.NotInitialized);
        initializeDataProvider(dataProvider);
        final Optional<GenomeVersion> selectedGenomeVersion = Optional.ofNullable(gmodel.getSelectedGenomeVersion());
        if (selectedGenomeVersion.isPresent()) {
            GeneralLoadUtils.initVersionAndSeq(selectedGenomeVersion.get().getName());
            GenometryModel.getInstance().refreshCurrentGenome();
            if (PreferenceUtils.getBooleanParam(PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load)) {
                GeneralLoadView.loadWholeRangeFeatures(dataProvider);
            }
            handleSinglePatternCausedRaceCondition();
            loadView.refreshTreeView();
        }
    }

    //TODO an obvious hack which will not be needed once the singleton randomly intialized by convention is removed
    private void handleSinglePatternCausedRaceCondition() {
        if (loadView == null) {
            loadView = GeneralLoadView.getLoadView();
        }
    }

    public static class DataProviderServiceChangeEvent {
        //just a signal type
    }

    public static int longestSubstr(String first, String second) {
        if (first == null || second == null || first.length() == 0 || second.length() == 0) {
            return 0;
        }

        int maxLen = 0;
        int fl = first.length();
        int sl = second.length();
        int[][] table = new int[fl + 1][sl + 1];

        for (int s = 0; s <= sl; s++) {
            table[0][s] = 0;
        }
        for (int f = 0; f <= fl; f++) {
            table[f][0] = 0;
        }

        for (int i = 1; i <= fl; i++) {
            for (int j = 1; j <= sl; j++) {
                if (first.charAt(i - 1) == second.charAt(j - 1)) {
                    if (i == 1 || j == 1) {
                        table[i][j] = 1;
                    } else {
                        table[i][j] = table[i - 1][j - 1] + 1;
                    }
                    if (table[i][j] > maxLen) {
                        maxLen = table[i][j];
                    }
                }
            }
        }
        return maxLen;
    }
}
