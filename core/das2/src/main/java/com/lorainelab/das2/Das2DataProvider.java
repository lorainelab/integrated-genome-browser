package com.lorainelab.das2;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.data.BaseDataProvider;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Initialized;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.NotResponding;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lorainelab.das2.model.segments.Segments;
import com.lorainelab.das2.model.sources.Capability;
import com.lorainelab.das2.model.sources.Sources;
import com.lorainelab.das2.model.sources.Version;
import com.lorainelab.das2.model.types.Types;
import com.lorainelab.das2.model.types.Types.TYPE;
import com.lorainelab.das2.utils.Das2ServerUtils;
import static com.lorainelab.das2.utils.Das2ServerUtils.SEGMENTS;
import static com.lorainelab.das2.utils.Das2ServerUtils.retrieveSegmentsResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
public final class Das2DataProvider extends BaseDataProvider implements DataProvider, AssemblyProvider {

    private static final Logger logger = LoggerFactory.getLogger(Das2DataProvider.class);
    public LinkedHashSet<String> availableGenomeVersionNames;
    private final Set<Version> versionInfo;
    private boolean editable = true;

    public Das2DataProvider(String dasUrl, String name, int loadPriority, boolean editable) {
        super(dasUrl, name, loadPriority);
        this.editable = editable;
        versionInfo = Sets.newLinkedHashSet();
        initialize();
    }

    public Das2DataProvider(String dasUrl, String name, String mirrorUrl, int loadPriority, boolean editable) {
        super(dasUrl, name, mirrorUrl, loadPriority);
        this.editable = editable;
        versionInfo = Sets.newLinkedHashSet();
        initialize();
    }

    @Override
    public void initialize() {
        try {
            Optional<Sources> sourcesResponse = Das2ServerUtils.retrieveSourcesResponse(url);
            if (sourcesResponse.isPresent()) {
                sourcesResponse.get().getSource().stream()
                        .flatMap(source -> source.getVersion().stream())
                        .filter(version -> !version.getCapability().isEmpty())
                        .forEach(versionInfo::add);
            } else {
                throw new IllegalStateException("No sources found on this Das 2 server.");
            }
        } catch (Exception ex) {
            logger.error("Could not initialize this Das Server, setting status to unavailable for this session.", ex);
            setStatus(NotResponding);
            return;
        }
        setStatus(Initialized);
    }

    @Override
    protected void disable() {
        versionInfo.clear();
        availableGenomeVersionNames.clear();
    }

    @Override
    public Set<DataSet> getAvailableDataSets(DataContainer dataContainer) {
        Set<DataSet> dataSets = Sets.newLinkedHashSet();
        try {
            final String genomeVersionName = dataContainer.getGenomeVersion().getName();
            Optional<Types> retrieveSegmentsResponse = Das2ServerUtils.retrieveTypesResponse(url, genomeVersionName);
            if (retrieveSegmentsResponse.isPresent()) {
                Types types = retrieveSegmentsResponse.get();
                for (TYPE type : types.getTYPE()) {
                    String extension = type.getFORMAT().getName();
                    HashMap<String, String> props = Maps.newHashMap();
                    props.put(type.getPROP().getKey(), type.getPROP().getValueAttribute());
                    String dataSetName = type.getTitle();
                    String genomeVersionContextUrl = Das2ServerUtils.getGenomeVersionContextUrl(url, genomeVersionName);
                    //type=http%3A%2F%2Fhci-bio-app.hci.utah.edu%3A8080%2FDAS2DB%2Fgenome%2FH_sapiens_Feb_2009%2Frefseq;format=brs
                    HttpRequest remoteHttpRequest = HttpRequest.get(genomeVersionContextUrl, true, "type", genomeVersionContextUrl + type.getUri());
                    final String typeParam = remoteHttpRequest.url().getQuery() + ";format=" + extension;
                    URI contextUri = new URI(genomeVersionContextUrl + type.getUri());
                    Das2Symloader das2Symloader = new Das2Symloader(new URI(genomeVersionContextUrl), dataSetName, extension, typeParam.substring(5), dataContainer.getGenomeVersion());
                    DataSet dataSet = new DataSet(contextUri, dataSetName, props, dataContainer, das2Symloader, false);
                    dataSet.setSupportsAvailabilityCheck(false);
                    dataSets.add(dataSet);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return dataSets;
    }

    @Override
    public Set<String> getSupportedGenomeVersionNames() {
        if (availableGenomeVersionNames == null) {
            availableGenomeVersionNames = versionInfo.stream().map(version -> version.getTitle()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return availableGenomeVersionNames;
    }

    @Override
    public Map<String, Integer> getAssemblyInfo(GenomeVersion genomeVersion) {
        Map<String, Integer> assemblyInfo = Maps.newLinkedHashMap();
        try {
            Optional<String> matchingGenomeVersionName = Das2ServerUtils.getMatchingGenomeVersionName(genomeVersion.getName(), getSupportedGenomeVersionNames());
            if (matchingGenomeVersionName.isPresent()) {
                String genomeVersionName = matchingGenomeVersionName.get();
                Optional<Version> matchingVersion = versionInfo.stream().filter(version -> version.getTitle().equals(genomeVersionName)).findFirst();
                if (matchingVersion.isPresent()) {
                    Optional<Capability> segments = matchingVersion.get().getCapability().stream().filter(capability -> capability.getType().equals(SEGMENTS)).findFirst();
                    if (segments.isPresent()) {
                        String queryUri = segments.get().getQueryUri();
                        Optional<Segments> retrieveSegmentsResponse = retrieveSegmentsResponse(url, queryUri);
                        retrieveSegmentsResponse.ifPresent(segmentResponse -> {
                            segmentResponse.getSEGMENT().stream().forEach(segment -> {
                                assemblyInfo.put(segment.getTitle(), segment.getLength());
                            });
                        });
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not retrieve Assembly Info from Das 2 source", ex);
        }
        return assemblyInfo;
    }

    @Override
    public Optional<String> getFactoryName() {
        return Optional.of(Das2DataProviderFactory.FACTORY_NAME);
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

}
