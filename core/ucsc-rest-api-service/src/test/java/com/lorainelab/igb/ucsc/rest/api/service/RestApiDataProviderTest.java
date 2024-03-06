package com.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lorainelab.igb.ucsc.rest.api.service.RestApiDataProvider;
import org.lorainelab.igb.ucsc.rest.api.service.model.UCSCRestTracks;
import org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RestApiDataProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(RestApiDataProviderTest.class);
    private final static String UCSC_REST_URL = "https://api.genome.ucsc.edu/";
    private static final String HUMAN_GENOME_ID = "hg38";
    private static RestApiDataProvider restApiDataProvider;
    private static GenomeVersion genomeVersion;
    private static DataContainer dataContainer;
    private final String validChromosomeName = "chr1";

    @BeforeAll
    public static void setup() {
        restApiDataProvider = new RestApiDataProvider(UCSC_REST_URL, "UCSC", 1);
        genomeVersion = new GenomeVersion(HUMAN_GENOME_ID);
        dataContainer = new DataContainer(genomeVersion, restApiDataProvider);
    }

    @Test
    public void retrieveSupportedGenomeVersions() {
        Assertions.assertTrue(restApiDataProvider.getSupportedGenomeVersionNames().contains(HUMAN_GENOME_ID));
        restApiDataProvider.getSupportedGenomeVersionNames().forEach(logger::info);
    }

    @Test
    public void retrieveAssemblyInfo() {
        assertTrue(restApiDataProvider.getAssemblyInfo(genomeVersion).containsKey(validChromosomeName));
    }

    @Test
    public void retrieveTracksResponseTest() {
        Optional<UCSCRestTracks> tracksResponse = UCSCRestServerUtils.retrieveTracksResponse(UCSC_REST_URL, HUMAN_GENOME_ID);
        assertTrue(tracksResponse.isPresent());
    }

    @Test
    public void retrieveSequence() {
        SeqSpan span = new SimpleSeqSpan(1000, 2000, new BioSeq(validChromosomeName, 0));
        String sequence = restApiDataProvider.getSequence(dataContainer, span);
        assertFalse(sequence.isEmpty());
    }

    @Test
    public void retrieveAvailableDataSets() {
        Set<DataSet> availableDataSets = restApiDataProvider.getAvailableDataSets(dataContainer);
        availableDataSets.forEach(dataContainer::addDataSet);
    }
}

