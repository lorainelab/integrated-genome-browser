package com.lorainelab.quickload;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.data.BaseDataProvider;
import com.affymetrix.genometry.data.SpeciesInfo;
import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceDataSetProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Initialized;
import com.affymetrix.genometry.util.ModalUtils;
import static com.affymetrix.genometry.util.UriUtils.isValidRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.lorainelab.synonymlookup.services.DefaultSynonymLookup;
import static com.lorainelab.quickload.QuickloadConstants.GENOME_TXT;
import com.lorainelab.quickload.model.annots.QuickloadFile;
import com.lorainelab.quickload.util.QuickloadUtils;
import static com.lorainelab.quickload.util.QuickloadUtils.getContextRootKey;
import static com.lorainelab.quickload.util.QuickloadUtils.getGenomeVersionBaseUrl;
import static com.lorainelab.quickload.util.QuickloadUtils.loadGenomeVersionSynonyms;
import static com.lorainelab.quickload.util.QuickloadUtils.loadSpeciesInfo;
import static com.lorainelab.quickload.util.QuickloadUtils.loadSupportedGenomeVersionInfo;
import static com.lorainelab.quickload.util.QuickloadUtils.toExternalForm;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class QuickloadDataProvider extends BaseDataProvider implements ReferenceSequenceDataSetProvider, AssemblyProvider {

    private static final Logger logger = LoggerFactory.getLogger(QuickloadDataProvider.class);

    private final Set<SpeciesInfo> speciesInfo;
    private final SetMultimap<String, String> genomeVersionSynonyms;
    private final Map<String, Optional<String>> supportedGenomeVersionInfo;
    private final Map<String, Optional<Multimap<String, String>>> chromosomeSynonymReference;
    private final DefaultSynonymLookup defaultSynonymLookup;

    public QuickloadDataProvider(String url, String name, int loadPriority, DefaultSynonymLookup defaultSynonymLookup) {
        super(toExternalForm(url), name, loadPriority);
        this.defaultSynonymLookup = defaultSynonymLookup;
        supportedGenomeVersionInfo = Maps.newConcurrentMap();
        speciesInfo = Sets.newHashSet();
        genomeVersionSynonyms = HashMultimap.create();
        chromosomeSynonymReference = Maps.newHashMap();
    }

    public QuickloadDataProvider(String url, String name, String mirrorUrl, int loadPriority, DefaultSynonymLookup defaultSynonymLookup) {
        super(toExternalForm(url), name, toExternalForm(mirrorUrl), loadPriority);
        this.defaultSynonymLookup = defaultSynonymLookup;
        supportedGenomeVersionInfo = Maps.newHashMap();
        speciesInfo = Sets.newHashSet();
        genomeVersionSynonyms = HashMultimap.create();
        chromosomeSynonymReference = Maps.newHashMap();
    }

    @Override
    public void initialize() {
        if (status == ResourceStatus.Disabled) {
            return;
        }
        logger.info("Initializing Quickload Server {}", getUrl());
        populateSupportedGenomeVersionInfo();
        loadOptionalQuickloadFiles();
        if (status != ResourceStatus.NotResponding) {
            setStatus(Initialized);
        }
    }

    @Override
    protected void disable() {
        supportedGenomeVersionInfo.clear();
        speciesInfo.clear();
        genomeVersionSynonyms.clear();
        chromosomeSynonymReference.clear();
    }

    private void loadOptionalQuickloadFiles() {
        loadGenomeVersionSynonyms(getUrl(), genomeVersionSynonyms);
        loadSpeciesInfo(getUrl(), speciesInfo);
    }

    private void populateSupportedGenomeVersionInfo() {
        try {
            loadSupportedGenomeVersionInfo(getUrl(), supportedGenomeVersionInfo);
            Thread validationThread = new Thread() {
                @Override
                public void run() {
                    validateAssemblyInformationIsAvailable(); //expensive, but according to quickload specification, this is required
                }
            };
            validationThread.start();
        } catch (IOException | URISyntaxException ex) {
            if (!useMirror && getMirrorUrl().isPresent()) {
                useMirror = true;
                initialize();
            } else {
                logger.error("Missing required quickload file, or could not reach source. This quickloak source will be disabled for this session.");
                status = ResourceStatus.NotResponding;
                useMirror = false; //reset to default url since mirror may have been tried
            }
        }
    }

    @Override
    public Set<String> getSupportedGenomeVersionNames() {
        return supportedGenomeVersionInfo.keySet();
    }

    @Override
    public Optional<String> getGenomeVersionDescription(String genomeVersionName) {
        genomeVersionName = getContextRootKey(genomeVersionName, supportedGenomeVersionInfo.keySet(), defaultSynonymLookup).orElse(genomeVersionName);
        if (supportedGenomeVersionInfo.containsKey(genomeVersionName)) {
            return supportedGenomeVersionInfo.get(genomeVersionName);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Set<SpeciesInfo>> getSpeciesInfo() {
        return Optional.ofNullable(speciesInfo);
    }

    @Override
    public Optional<SetMultimap<String, String>> getGenomeVersionSynonyms() {
        return Optional.of(genomeVersionSynonyms);
    }

    @Override
    public Optional<Multimap<String, String>> getChromosomeSynonyms(DataContainer dataContainer) {
        return Optional.empty();//TODO fix this add support
//        return chromosomeSynonymReference.get(genomeVersion.getName());
    }

    @Override
    public Set<DataSet> getAvailableDataSets(DataContainer dataContainer) {
        final GenomeVersion genomeVersion = dataContainer.getGenomeVersion();
        final String genomeVersionName = getContextRootKey(genomeVersion.getName(), supportedGenomeVersionInfo.keySet(), defaultSynonymLookup).orElse(genomeVersion.getName());
        final Optional<Set<QuickloadFile>> genomeVersionData = QuickloadUtils.getGenomeVersionData(getUrl(), genomeVersionName, supportedGenomeVersionInfo, defaultSynonymLookup);
        if (genomeVersionData.isPresent()) {
            Set<QuickloadFile> versionFiles = genomeVersionData.get();
            LinkedHashSet<DataSet> dataSets = Sets.newLinkedHashSet();

            List<QuickloadFile> missingNameAttribute = versionFiles.stream().filter(file -> Strings.isNullOrEmpty(file.getName())).collect(Collectors.toList());
            if (!missingNameAttribute.isEmpty()) {
                ModalUtils.errorPanel("The " + genomeVersionName + " genome contains some missing name attributes in its annots.xml file on the quickload site (" + getUrl() + ")");
            }

            versionFiles.stream().filter(file -> !Strings.isNullOrEmpty(file.getName())).forEach((file) -> {
                try {
                    URI uri;
                    if (!file.getName().startsWith("http")) {
                        uri = new URI(getUrl() + genomeVersionName + "/" + file.getName());
                    } else {
                        uri = new URI(file.getName());
                    }
                    DataSet dataSet = new DataSet(uri, file.getProps(), dataContainer);
                    dataSet.setSupportsAvailabilityCheck(true);
                    dataSets.add(dataSet);
                } catch (URISyntaxException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            });
            return dataSets;
        } else {
            return Sets.newLinkedHashSet();
        }
    }

    @Override
    public Map<String, Integer> getAssemblyInfo(GenomeVersion genomeVersion) {
        final String genomeVersionName = getContextRootKey(genomeVersion.getName(), supportedGenomeVersionInfo.keySet(), defaultSynonymLookup).orElse(genomeVersion.getName());
        try {
            final Optional<Map<String, Integer>> assemblyInfo = QuickloadUtils.getAssemblyInfo(getUrl(), genomeVersionName);
            if (assemblyInfo.isPresent()) {
                return assemblyInfo.get();
            }
        } catch (URISyntaxException ex) {
            logger.error("Missing required {} file for genome version {}, skipping this genome version for quickload site {}", GENOME_TXT, genomeVersionName, getUrl());
        } catch (IOException ex) {
            logger.error("Coulld not read required {} file for genome version {}, skipping this genome version for quickload site {}", GENOME_TXT, genomeVersionName, getUrl());
        }
        return Maps.newTreeMap();
    }

    @Override
    public Optional<URI> getSequenceFileUri(GenomeVersion genomeVersion) {
        final String genomeVersionName = getContextRootKey(genomeVersion.getName(), supportedGenomeVersionInfo.keySet(), defaultSynonymLookup).orElse(genomeVersion.getName());
        final String sequenceFileLocation = getGenomeVersionBaseUrl(getUrl(), genomeVersionName) + genomeVersionName + ".2bit";
        URI uri = null;
        try {
            uri = new URI(sequenceFileLocation);
            if (isValidRequest(uri)) {
                return Optional.of(uri);
            }
        } catch (URISyntaxException | IOException ex) {
            //do nothing
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getPrimaryLinkoutUrl() {
        return Optional.of(url);
    }

    @Override
    public Optional<String> getDataSetLinkoutUrl(DataSet dataSet) {
        if (dataSet.getProperties() != null && dataSet.getProperties().containsKey("url")) {
            String linkoutUrl = dataSet.getProperties().get("url");
            if (!Strings.isNullOrEmpty(linkoutUrl)) {
                if (linkoutUrl.startsWith("http")) {
                    return Optional.<String>of(linkoutUrl);
                } else {
                    return Optional.<String>of(toExternalForm(url) + linkoutUrl);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getFactoryName() {
        return Optional.of(QUICKLOAD_FACTORY_NAME);
    }
    private static final String QUICKLOAD_FACTORY_NAME = "Quickload";

    private synchronized void validateAssemblyInformationIsAvailable() {
        List<String> genomesMissingGenomeTxt = Lists.newArrayList();
        supportedGenomeVersionInfo.keySet().forEach(genomeVersionName -> {
            try {
                final Optional<Map<String, Integer>> assemblyInfo = QuickloadUtils.getAssemblyInfo(getUrl(), genomeVersionName);
                if (!assemblyInfo.isPresent()) {
                    genomesMissingGenomeTxt.add(genomeVersionName);
                }
            } catch (URISyntaxException ex) {
                logger.error("Missing required {} file for genome version {}, skipping this genome version for quickload site {}", GENOME_TXT, genomeVersionName, getUrl());
                genomesMissingGenomeTxt.add(genomeVersionName);
            } catch (IOException ex) {
                logger.error("Coulld not read required {} file for genome version {}, skipping this genome version for quickload site {}", GENOME_TXT, genomeVersionName, getUrl());
                genomesMissingGenomeTxt.add(genomeVersionName);
            }
        });
        genomesMissingGenomeTxt.forEach(supportedGenomeVersionInfo::remove);
        if (!genomesMissingGenomeTxt.isEmpty()) {
            ModalUtils.errorPanel("The following genome versions for quickload site (" + getUrl() + ") are missing a " + GENOME_TXT + " file: " + System.lineSeparator() + Joiner.on(System.lineSeparator()).join(genomesMissingGenomeTxt));
        }
    }

}
