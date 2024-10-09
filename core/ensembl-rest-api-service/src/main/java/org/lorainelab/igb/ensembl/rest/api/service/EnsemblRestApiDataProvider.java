package org.lorainelab.igb.ensembl.rest.api.service;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.data.BaseDataProvider;
import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.*;

@Slf4j
public final class EnsemblRestApiDataProvider extends BaseDataProvider implements AssemblyProvider, ReferenceSequenceProvider {

    private final Set<String> availableGenomesSet;

    public EnsemblRestApiDataProvider(String ensemblRestUrl, String name, int loadPriority) {
        super(ensemblRestUrl, name, loadPriority);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ensemblUrl = new URIBuilder(url).build().toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
    }

    public EnsemblRestApiDataProvider(String ensemblRestUrl, String name, String mirrorUrl, int loadPriority) {
        super(ensemblRestUrl, name, mirrorUrl, loadPriority);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ensemblUrl = new URIBuilder(url).build().toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
    }

    public EnsemblRestApiDataProvider(String ensemblRestUrl, String name, int loadPriority, String id) {
        super(ensemblRestUrl, name, loadPriority, id);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ensemblUrl = new URIBuilder(url).build().toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
    }

    public EnsemblRestApiDataProvider(String ensemblRestUrl, String name, String mirrorUrl, int loadPriority, String id) {
        super(ensemblRestUrl, name, mirrorUrl, loadPriority, id);
        availableGenomesSet = Sets.newHashSet();
        try {
            URL ensemblUrl = new URIBuilder(url).build().toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
    }

    @Override
    public void initialize() {
        if (status == Disabled) {
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
        return Sets.newLinkedHashSet();
    }

    @Override
    public Map<String, Integer> getAssemblyInfo(GenomeVersion genomeVersion) {
        return Maps.newHashMap();
    }

    @Override
    public String getSequence(DataContainer dataContainer, SeqSpan span) {
        return "";
    }

    @Override
    public Optional<String> getFactoryName() {
        return Optional.of(EnsemblRestDataProviderFactory.FACTORY_NAME);
    }
}
