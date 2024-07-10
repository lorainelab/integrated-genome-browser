package org.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.data.BaseDataProvider;
import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.util.LoadUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.lorainelab.igb.ucsc.rest.api.service.model.GenomesData;
import org.lorainelab.igb.ucsc.rest.api.service.model.TrackDetails;
import org.lorainelab.igb.ucsc.rest.api.service.model.UCSCRestTracks;
import org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.*;

@Slf4j
public final class UCSCRestApiDataProvider extends BaseDataProvider implements AssemblyProvider, ReferenceSequenceProvider {

    private final Set<String> availableGenomesSet;
    private static final String linkoutUrl = ResourceBundle.getBundle("igb").getString("linkoutBaseUrl");

    public UCSCRestApiDataProvider(String ucscRestUrl, String name, int loadPriority) {
        super(ucscRestUrl, name, loadPriority);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ucscRestDsnUrl = new URIBuilder(url).build().toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
    }

    public UCSCRestApiDataProvider(String ucscRestUrl, String name, String mirrorUrl, int loadPriority) {
        super(ucscRestUrl, name, mirrorUrl, loadPriority);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ucscRestDsnUrl = new URIBuilder(url).build().toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
    }

    public UCSCRestApiDataProvider(String ucscRestUrl, String name, int loadPriority, String id) {
        super(ucscRestUrl, name, loadPriority, id);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ucscRestDsnUrl = new URIBuilder(url).build().toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
    }

    public UCSCRestApiDataProvider(String ucscRestUrl, String name, String mirrorUrl, int loadPriority, String id) {
        super(ucscRestUrl, name, mirrorUrl, loadPriority, id);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ucscRestDsnUrl = new URIBuilder(url).build().toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
    }

    @Override
    public void initialize() {
        if (status == LoadUtils.ResourceStatus.Disabled) {
            return;
        }
        try {
            log.info("Initializing UCSC Rest Server {}", getUrl());
            Optional<GenomesData> genomeApiResponse = UCSCRestServerUtils.retrieveGenomeResponse(url);
            genomeApiResponse.ifPresent(ds -> ds.getUcscGenomes().forEach((genomeName, genome) -> availableGenomesSet.add(genomeName)));
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
                        UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(url, uri, Optional.empty(), track, trackType, trackDetail, genomeVersion, contextRootkey.get());
                        Map<String, String> featureProps = getFeatureProps(track, trackDetail, contextRootkey);
                        String trackName = trackDetail.getShortLabel() + " (" + track + ")";
                        String datasetName = trackType + "/" + trackName;
                        DataSet dataSet = new DataSet(uri, datasetName, featureProps, dataContainer, ucscRestSymLoader, false);
                        dataSets.add(dataSet);
                    } catch (URISyntaxException ex) {
                        log.error("Invalid URI format for DAS context root: {}, skipping this resource", contextRoot, ex);
                    }
                });
            }
        }
        TreeSet<DataSet> sortedDataSets = new TreeSet<>(Comparator.comparing(
                dataSet -> dataSet.getDataSetName().toLowerCase()
        ));
        sortedDataSets.addAll(dataSets);
        return sortedDataSets;
    }

    private Map<String, String> getFeatureProps(String track, TrackDetails trackDetail, Optional<String> contextRootkey) throws URISyntaxException {
        Map<String, String> featureProps = new HashMap<>();
        featureProps.put("description", trackDetail.getLongLabel());
        featureProps.put("track", track);
        if(!Strings.isNullOrEmpty(linkoutUrl)){
            URIBuilder linkoutUrlBuilder = new URIBuilder(linkoutUrl);
            linkoutUrlBuilder.addParameter("db", contextRootkey.get());
            linkoutUrlBuilder.addParameter("g", track);
            featureProps.put("linkoutUrl", linkoutUrlBuilder.toString());
        }
        return featureProps;
    }

    @Override
    public Map<String, Integer> getAssemblyInfo(GenomeVersion genomeVersion) {
        return UCSCRestServerUtils.getAssemblyInfo(url, genomeVersion, availableGenomesSet);
    }

    @Override
    public Optional<String> getDataSetLinkoutUrl(DataSet dataSet) {
        String linkoutUrl = null;
        if(Objects.nonNull(dataSet.getProperties()))
            linkoutUrl = dataSet.getProperties().getOrDefault("linkoutUrl", null);
        return !Strings.isNullOrEmpty(linkoutUrl) ? Optional.of(linkoutUrl) : Optional.empty();
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
