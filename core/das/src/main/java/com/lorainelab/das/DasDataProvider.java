package com.lorainelab.das;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.data.BaseDataProvider;
import com.affymetrix.genometry.data.GenomeVersionProvider;
import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Disabled;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Initialized;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.NotResponding;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lorainelab.das.model.dsn.DasDsn;
import com.lorainelab.das.model.types.DasTypes;
import com.lorainelab.das.model.types.Type;
import com.lorainelab.das.utils.DasServerUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public final class DasDataProvider extends BaseDataProvider implements AssemblyProvider, ReferenceSequenceProvider, GenomeVersionProvider {

    private static final Logger logger = LoggerFactory.getLogger(DasDataProvider.class);
    private Map<String, String> genomeContextRootMap;

    public DasDataProvider(String dasUrl, String name, int loadPriority) {
        super(dasUrl, name, loadPriority);
        genomeContextRootMap = Maps.newLinkedHashMap();
        try {
            URL dasDsnUrl = new URL(url);
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
        if (status != Disabled) {
            initialize();
        }
    }

    public DasDataProvider(String dasUrl, String name, String mirrorUrl, int loadPriority) {
        super(dasUrl, name, mirrorUrl, loadPriority);
        genomeContextRootMap = Maps.newHashMap();
        try {
            URL dasDsnUrl = new URL(dasUrl);
        } catch (MalformedURLException | IllegalArgumentException ex) {
            logger.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
        if (status != Disabled) {
            initialize();
        }
    }

    @Override
    public void initialize() {
        try {
            Optional<DasDsn> dsnResponse = DasServerUtils.retrieveDsnResponse(url);
            dsnResponse.ifPresent(ds -> {
                ds.getDSN().stream().forEach(dsn -> {
                    String mapMaster = dsn.getMapMaster();
                    String sourceId = dsn.getSOURCE().getId();
                    genomeContextRootMap.put(sourceId, mapMaster);
                });
            });
        } catch (HttpRequest.HttpRequestException ex) {
            logger.error("Could not initialize this Das Server, setting status to unavailable for this session.", ex);
            setStatus(NotResponding);
            return;
        }
        setStatus(Initialized);
    }

    @Override
    protected void disable() {
        genomeContextRootMap.clear();
    }

    @Override
    public Set<String> getAvailableGenomeVersionNames() {
        return genomeContextRootMap.keySet();
    }

    @Override
    public Set<DataSet> getAvailableDataSets(DataContainer dataContainer) {
        GenomeVersion genomeVersion = dataContainer.getGenomeVersion();
        final String genomeVersionName = genomeVersion.getName();
        Optional<String> contextRootkey = DasServerUtils.getContextRootKey(genomeVersionName, genomeContextRootMap);
        Set<DataSet> dataSets = Sets.newLinkedHashSet();
        if (contextRootkey.isPresent()) {
            String contextRoot = genomeContextRootMap.get(contextRootkey.get());
            Optional<DasTypes> retrieveDasTypesResponse = DasServerUtils.retrieveDasTypesResponse(contextRoot);
            if (retrieveDasTypesResponse.isPresent()) {
                DasTypes entryPointInfo = retrieveDasTypesResponse.get();
                final List<Type> availableTypes = entryPointInfo.getGFF().getSEGMENT().getTYPE();
                for (Type type : availableTypes) {
                    final String typeId = type.getId();
                    try {
                        DasSymloader dasSymloader = new DasSymloader(new URI(contextRoot + "/" + typeId), typeId, genomeVersion);
                        DataSet dataSet = new DataSet(new URI(contextRoot + "/" + typeId), typeId, null, dataContainer, dasSymloader, false);
                        dataSets.add(dataSet);
                    } catch (URISyntaxException ex) {
                        logger.error("Invalid URI format for DAS context root: {}, skipping this resource", contextRoot, ex);
                    }
                }
            }
        }
        return dataSets;
    }

    @Override
    public Map<String, Integer> getAssemblyInfo(GenomeVersion genomeVersion) {
        return DasServerUtils.getAssemblyInfo(genomeVersion, genomeContextRootMap);
    }

    @Override
    public String getSequence(DataContainer dataContainer, SeqSpan span) {
        GenomeVersion genomeVersion = dataContainer.getGenomeVersion();
        final String genomeVersionName = genomeVersion.getName();
        Optional<String> contextRootkey = DasServerUtils.getContextRootKey(genomeVersionName, genomeContextRootMap);
        if (contextRootkey.isPresent()) {
            String contextRoot = genomeContextRootMap.get(contextRootkey.get());
            return DasServerUtils.retrieveDna(contextRoot, span);
        }
        return "";
    }

    @Override
    public Optional<String> getFactoryName() {
        return Optional.of(DasDataProviderFactory.FACTORY_NAME);
    }

    @Override
    public Set<String> getSupportedGenomeVersionNames() {
        return genomeContextRootMap.keySet();
    }

}
