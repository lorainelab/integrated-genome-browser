package org.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.data.BaseDataProvider;
import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.util.LoadUtils;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.lorainelab.igb.ucsc.rest.api.service.model.GenomesData;
import org.lorainelab.igb.ucsc.rest.api.service.model.UCSCRestTracks;
import org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.*;

@Slf4j
public final class RestApiDataProvider extends BaseDataProvider implements AssemblyProvider, ReferenceSequenceProvider {

    private final Set<String> availableGenomesSet;
    public static final int READ_TIMEOUT = 60000;

    public RestApiDataProvider(String ucscRestUrl, String name, int loadPriority) {
        super(ucscRestUrl, name, loadPriority);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ucscRestDsnUrl = new URL(url);
        } catch (MalformedURLException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
        if (status != Disabled) {
            initialize();
        }
    }

    public RestApiDataProvider(String ucscRestUrl, String name, String mirrorUrl, int loadPriority) {
        super(ucscRestUrl, name, mirrorUrl, loadPriority);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ucscRestDsnUrl = new URL(ucscRestUrl);
        } catch (MalformedURLException | IllegalArgumentException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
        if (status != Disabled) {
            initialize();
        }
    }

    public RestApiDataProvider(String ucscRestUrl, String name, int loadPriority, String id) {
        super(ucscRestUrl, name, loadPriority, id);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ucscRestDsnUrl = new URL(url);
        } catch (MalformedURLException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
        if (status != Disabled) {
            initialize();
        }
    }

    public RestApiDataProvider(String ucscRestUrl, String name, String mirrorUrl, int loadPriority, String id) {
        super(ucscRestUrl, name, mirrorUrl, loadPriority, id);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ucscRestDsnUrl = new URL(ucscRestUrl);
        } catch (MalformedURLException | IllegalArgumentException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
        if (status != Disabled) {
            initialize();
        }
    }

    @Override
    public void initialize() {
        if (status == LoadUtils.ResourceStatus.Disabled) {
            return;
        }
        try {
            Optional<GenomesData> genomoeApiResponse = UCSCRestServerUtils.retrieveDsnResponse(url);
            genomoeApiResponse.ifPresent(ds -> {
                ds.getUcscGenomes().forEach((genomoeName, genome) -> {
                   availableGenomesSet.add(genomoeName);
                });
            });
        } catch (IOException ex) {
            log.error("Could not initialize this UCSC Rest Server, setting status to unavailable for this session.", ex);
            setStatus(NotResponding);
            return;
        }
        setStatus(Initialized);
    }

    @Override
    protected void disable() {
        availableGenomesSet.clear();
    }

    @Override
    public Set<String> getSupportedGenomeVersionNames() {
        return availableGenomesSet;
    }

    @Override
    public Set<DataSet> getAvailableDataSets(DataContainer dataContainer) {
        GenomeVersion genomeVersion = dataContainer.getGenomeVersion();
        final String genomeVersionName = genomeVersion.getName();
        Optional<String> contextRootkey = UCSCRestServerUtils.getContextRootKey(genomeVersionName, availableGenomesSet, genomeVersion.getGenomeVersionSynonymLookup());
        Set<DataSet> dataSets = Sets.newLinkedHashSet();
        if (contextRootkey.isPresent()) {
            String contextRoot = url;
            Optional<UCSCRestTracks> tracksResponse = UCSCRestServerUtils.retrieveTracksResponse(contextRoot, contextRootkey.get());
            if (tracksResponse.isPresent()) {
                UCSCRestTracks ucscRestTracks = tracksResponse.get();
                ucscRestTracks.getTracks().forEach((track, trackDetail) -> {
                    try {
                        URIBuilder uriBuilder = new URIBuilder(contextRoot + "/getData/track");
                        uriBuilder.addParameter("genome", contextRootkey.get());
                        uriBuilder.addParameter("track", track);
                        URI uri = uriBuilder.build();
                        String trackType = trackDetail.getType().split(" ")[0];
                        UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(url, uri, Optional.empty(), track, trackType, genomeVersion, contextRootkey.get());
                        DataSet dataSet = new DataSet(uri, track, null, dataContainer, ucscRestSymLoader, false);
                        dataSets.add(dataSet);
                    } catch (URISyntaxException ex) {
                        log.error("Invalid URI format for DAS context root: {}, skipping this resource", contextRoot, ex);
                    }
                });
            }
        }
        return dataSets;
    }

    @Override
    public Map<String, Integer> getAssemblyInfo(GenomeVersion genomeVersion) {
        return UCSCRestServerUtils.getAssemblyInfo(url, genomeVersion, availableGenomesSet);
    }

    @Override
    public String getSequence(DataContainer dataContainer, SeqSpan span) {
        GenomeVersion genomeVersion = dataContainer.getGenomeVersion();
        final String genomeVersionName = genomeVersion.getName();
        if (!genomeVersionName.isEmpty()) {
            return UCSCRestServerUtils.retrieveDna(url, span, genomeVersionName);
        }
        return "";
    }

    @Override
    public Optional<String> getFactoryName() {
        return Optional.of(UCSCRestDataProviderFactory.FACTORY_NAME);
    }
}
