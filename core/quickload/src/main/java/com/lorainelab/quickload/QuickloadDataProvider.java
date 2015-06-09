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
import static com.affymetrix.genometry.util.UriUtils.isValidRequest;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import static com.lorainelab.quickload.QuickloadConstants.ANNOTS_XML;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    public QuickloadDataProvider(String url, String name, int loadPriority) {
        super(toExternalForm(url), name, loadPriority);
        supportedGenomeVersionInfo = Maps.newHashMap();
        speciesInfo = Sets.newHashSet();
        genomeVersionSynonyms = HashMultimap.create();
        chromosomeSynonymReference = Maps.newHashMap();
    }

    public QuickloadDataProvider(String url, String name, String mirrorUrl, int loadPriority) {
        super(toExternalForm(url), name, toExternalForm(mirrorUrl), loadPriority);
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
        genomeVersionName = getContextRootKey(genomeVersionName, supportedGenomeVersionInfo.keySet()).orElse(genomeVersionName);
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
        final String genomeVersionName = getContextRootKey(genomeVersion.getName(), supportedGenomeVersionInfo.keySet()).orElse(genomeVersion.getName());
        final Optional<Set<QuickloadFile>> genomeVersionData = QuickloadUtils.getGenomeVersionData(getUrl(), genomeVersionName, supportedGenomeVersionInfo);
        if (genomeVersionData.isPresent()) {
            Set<QuickloadFile> versionFiles = genomeVersionData.get();
            LinkedHashSet<DataSet> dataSets = Sets.newLinkedHashSet();
            versionFiles.stream().forEach((file) -> {
                try {
                    URI uri;
                    if (!file.getName().startsWith("http")) {
                        uri = new URI(getUrl() + genomeVersionName + "/" + file.getName());
                    } else {
                        uri = new URI(file.getName());
                    }
                    DataSet dataSet = new DataSet(uri, file.getProps(), dataContainer);
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
        final String genomeVersionName = getContextRootKey(genomeVersion.getName(), supportedGenomeVersionInfo.keySet()).orElse(genomeVersion.getName());
        try {
            final Optional<Map<String, Integer>> assemblyInfo = QuickloadUtils.getAssemblyInfo(getUrl(), genomeVersionName);
            if (assemblyInfo.isPresent()) {
                return assemblyInfo.get();
            }
        } catch (URISyntaxException ex) {
            logger.error("Missing required {} file for genome version {}, skipping this genome version for quickload site {}", ANNOTS_XML, genomeVersionName, getUrl());
            supportedGenomeVersionInfo.remove(genomeVersionName);
        } catch (IOException ex) {
            logger.error("Coulld not read required {} file for genome version {}, skipping this genome version for quickload site {}", ANNOTS_XML, genomeVersionName, getUrl());
            supportedGenomeVersionInfo.remove(genomeVersionName);
        }
        return Maps.newTreeMap();
    }

    @Override
    public Optional<URI> getSequenceFileUri(GenomeVersion genomeVersion) {
        final String genomeVersionName = getContextRootKey(genomeVersion.getName(), supportedGenomeVersionInfo.keySet()).orElse(genomeVersion.getName());
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
    public Optional<String> getFactoryName() {
        return Optional.of(QUICKLOAD_FACTORY_NAME);
    }
    private static final String QUICKLOAD_FACTORY_NAME = "Quickload";

}
