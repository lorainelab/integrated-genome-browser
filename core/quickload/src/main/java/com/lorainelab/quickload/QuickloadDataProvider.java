package com.lorainelab.quickload;

import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.data.DataSetProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceDataSetProvider;
import com.affymetrix.genometry.data.SpeciesInfo;
import com.affymetrix.genometry.general.GenomeVersion;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import static com.lorainelab.quickload.QuickloadConstants.ANNOTS_XML;
import com.lorainelab.quickload.model.annots.QuickloadFile;
import com.lorainelab.quickload.util.QuickloadUtils;
import static com.lorainelab.quickload.util.QuickloadUtils.getGenomeVersionBaseUrl;
import static com.lorainelab.quickload.util.QuickloadUtils.loadGenomeVersionSynonyms;
import static com.lorainelab.quickload.util.QuickloadUtils.loadSpeciesInfo;
import static com.lorainelab.quickload.util.QuickloadUtils.loadSupportedGenomeVersionInfo;
import static com.lorainelab.quickload.util.QuickloadUtils.toExternalForm;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class QuickloadDataProvider implements DataSetProvider, ReferenceSequenceDataSetProvider, AssemblyProvider {

    private static final Logger logger = LoggerFactory.getLogger(QuickloadDataProvider.class);
    private static final int DEFAULT_LOAD_PRIORITY = -1;
    private String name;
    private final String url;
    private String mirrorUrl;
    private ResourceStatus status;
    private int loadPriority;
    private final Set<SpeciesInfo> speciesInfo;
    private final Multimap<String, String> genomeVersionSynonyms;
    private final Map<String, Optional<String>> supportedGenomeVersionInfo;
    private final Map<String, Optional<Multimap<String, String>>> chromosomeSynonymReference;

    public QuickloadDataProvider(String url, String name) {
        this.url = url;
        this.name = name;
        supportedGenomeVersionInfo = Maps.newHashMap();
        speciesInfo = Sets.newHashSet();
        genomeVersionSynonyms = HashMultimap.create();
        chromosomeSynonymReference = Maps.newHashMap();
        loadPriority = DEFAULT_LOAD_PRIORITY;
        loadOptionalQuickloadFiles();
        populateSupportedGenomeVersionInfo();
    }

    private void loadOptionalQuickloadFiles() {
        loadGenomeVersionSynonyms(url, genomeVersionSynonyms);
        loadSpeciesInfo(url, speciesInfo);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Optional<String> getMirrorUrl() {
        return Optional.ofNullable(mirrorUrl);
    }

    @Override
    public void setMirrorUrl(String mirrorUrl) {
        this.mirrorUrl = mirrorUrl;
    }

    @Override
    public ResourceStatus getServerStatus() {
        return status;
    }

    private void populateSupportedGenomeVersionInfo() {
        try {
            loadSupportedGenomeVersionInfo(url, supportedGenomeVersionInfo);
        } catch (IOException ex) {
            logger.warn("Missing required quickload file, this quickloak source will be disabled.", ex);
            status = ResourceStatus.Disabled;
        }
    }

    @Override
    public int getLoadPriority() {
        return loadPriority;
    }

    @Override
    public void setLoadPriority(int loadPriority) {
        this.loadPriority = loadPriority;
    }

    @Override
    public void setServerStatus(ResourceStatus serverStatus) {
        this.status = serverStatus;
    }

    @Override
    public Set<String> getSupportedGenomeVersionNames() {
        return supportedGenomeVersionInfo.keySet();
    }

    @Override
    public Optional<String> getGenomeVersionDescription(String genomeVersionName) {
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
    public Optional<Multimap<String, String>> getChromosomeSynonyms(GenomeVersion genomeVersion) {
        return chromosomeSynonymReference.get(genomeVersion.getVersionName());
    }

    @Override
    public Set<String> getAvailableDataSetUrls(GenomeVersion genomeVersion) {
        final Optional<Set<QuickloadFile>> genomeVersionData = QuickloadUtils.getGenomeVersionData(url, genomeVersion.getVersionName(), supportedGenomeVersionInfo);
        if (genomeVersionData.isPresent()) {
            final Function<String, String> toFullFileUrl = quickloadFilePath -> toExternalForm(url + genomeVersion.getVersionName()) + quickloadFilePath;
            return genomeVersionData.get().stream()
                    .map(quickloadFile -> quickloadFile.getName())
                    .filter(quickloadFilePath -> !Strings.isNullOrEmpty(quickloadFilePath))
                    .map(toFullFileUrl)
                    .collect(Collectors.toSet());
        } else {
            return Sets.newHashSet();
        }
    }

    @Override
    public Map<String, Integer> getAssemblyInfo(GenomeVersion genomeVersion) {
        try {
            final Optional<Map<String, Integer>> assemblyInfo = QuickloadUtils.getAssemblyInfo(url, genomeVersion.getVersionName());
            if (assemblyInfo.isPresent()) {
                return assemblyInfo.get();
            }
        } catch (MalformedURLException ex) {
            logger.error("Missing required {} file for genome version {}, skipping this genome version for quickload site {}", ANNOTS_XML, genomeVersion.getVersionName(), url, ex);
            supportedGenomeVersionInfo.remove(genomeVersion.getVersionName());
        } catch (IOException ex) {
            logger.error("Coulld not read required {} file for genome version {}, skipping this genome version for quickload site {}", ANNOTS_XML, genomeVersion.getVersionName(), url, ex);
            supportedGenomeVersionInfo.remove(genomeVersion.getVersionName());
        }
        return Maps.newHashMap();
    }

    @Override
    public String getSequenceFileUrl(GenomeVersion genomeVersion) {
        return getGenomeVersionBaseUrl(url, genomeVersion.getVersionName()) + genomeVersion.getVersionName() + ".2bit";
    }

}
