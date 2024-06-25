package com.lorainelab.das;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.github.kevinsawicki.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lorainelab.igb.das.DasDataProvider;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 *
 * @author dcnorris
 */
@ExtendWith(MockitoExtension.class)
public class DasDataProviderTest {

    private final static String UCSC_DAS_URL = "https://genome.cse.ucsc.edu/cgi-bin/das";
    private static final String HUMAN_GENOME_ID = "hg38";
    private DasDataProvider dasDataProvider;
    @Mock
    private HttpRequest mockHttpRequest;
    private static GenomeVersion genomeVersion;
    private static DataContainer dataContainer;
    private final String validChromosomeName = "1";

    @BeforeEach
    public void setup() {
        try (MockedStatic<HttpRequest> mockedStatic = Mockito.mockStatic(HttpRequest.class)) {
            mockedStatic.when(() -> HttpRequest.get(UCSC_DAS_URL+"/dsn")).thenReturn(mockHttpRequest);
            when(mockHttpRequest.acceptGzipEncoding()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.uncompress(true)).thenReturn(mockHttpRequest);
            when(mockHttpRequest.trustAllCerts()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.trustAllHosts()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.followRedirects(true)).thenReturn(mockHttpRequest);
            InputStream inputStream = DasDataProviderTest.class.getClassLoader().getResourceAsStream("data/das1/das-dsn-test-data.xml");
            when(mockHttpRequest.buffer()).thenReturn((BufferedInputStream) inputStream);
            dasDataProvider = new DasDataProvider(UCSC_DAS_URL, "UCSC", 1);
            dasDataProvider.initialize();
        }
        genomeVersion = new GenomeVersion(HUMAN_GENOME_ID);
        dataContainer = new DataContainer(genomeVersion, dasDataProvider);
    }

    @Test
    public void retrieveSupportedGenomeVersions() {
        assertTrue(dasDataProvider.getSupportedGenomeVersionNames().contains(HUMAN_GENOME_ID));
    }

    @Test
    public void retrieveAssemblyInfo() {
        try (MockedStatic<HttpRequest> mockedStatic = Mockito.mockStatic(HttpRequest.class)) {
            mockedStatic.when(() -> HttpRequest.get("http://genome.ucsc.edu:80/cgi-bin/das/hg38/entry_points")).thenReturn(mockHttpRequest);
            when(mockHttpRequest.acceptGzipEncoding()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.uncompress(true)).thenReturn(mockHttpRequest);
            when(mockHttpRequest.trustAllCerts()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.trustAllHosts()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.followRedirects(true)).thenReturn(mockHttpRequest);
            InputStream inputStream = DasDataProviderTest.class.getClassLoader().getResourceAsStream("data/das1/das-entry-points-test-data.xml");
            when(mockHttpRequest.buffer()).thenReturn((BufferedInputStream) inputStream);
            assertTrue(dasDataProvider.getAssemblyInfo(genomeVersion).containsKey(validChromosomeName));
        }
    }

    @Test
    public void retrieveSequence() {
        try (MockedStatic<HttpRequest> mockedStatic = Mockito.mockStatic(HttpRequest.class)) {
            SeqSpan seqSpan = new SimpleSeqSpan(10000, 12000, new BioSeq(validChromosomeName, 0));
            String segmentParam = seqSpan.getBioSeq().getId() + ":" + seqSpan.getMin() + "," + seqSpan.getMax();
            mockedStatic.when(() -> HttpRequest.get("http://genome.ucsc.edu:80/cgi-bin/das/hg38/dna", true, "segment", segmentParam)).thenReturn(mockHttpRequest);
            when(mockHttpRequest.acceptGzipEncoding()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.uncompress(true)).thenReturn(mockHttpRequest);
            when(mockHttpRequest.trustAllCerts()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.trustAllHosts()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.followRedirects(true)).thenReturn(mockHttpRequest);
            InputStream inputStream = DasDataProviderTest.class.getClassLoader().getResourceAsStream("data/das1/das-dna-sequence-test-data.xml");
            when(mockHttpRequest.buffer()).thenReturn((BufferedInputStream) inputStream);
            String sequence = dasDataProvider.getSequence(dataContainer, seqSpan);
            assertFalse(sequence.isEmpty());
        }
    }

    @Test
    public void retrieveAvailableDataSets() {
        try (MockedStatic<HttpRequest> mockedStatic = Mockito.mockStatic(HttpRequest.class)) {
            mockedStatic.when(() -> HttpRequest.get("http://genome.ucsc.edu:80/cgi-bin/das/hg38/types")).thenReturn(mockHttpRequest);
            when(mockHttpRequest.acceptGzipEncoding()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.uncompress(true)).thenReturn(mockHttpRequest);
            when(mockHttpRequest.trustAllCerts()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.trustAllHosts()).thenReturn(mockHttpRequest);
            when(mockHttpRequest.followRedirects(true)).thenReturn(mockHttpRequest);
            InputStream inputStream = DasDataProviderTest.class.getClassLoader().getResourceAsStream("data/das1/das-available-datasets-test-data.xml");
            when(mockHttpRequest.buffer()).thenReturn((BufferedInputStream) inputStream);
            Set<DataSet> availableDataSets = dasDataProvider.getAvailableDataSets(dataContainer);
            assertTrue(availableDataSets.stream().anyMatch(dataSet -> dataSet.getDataSetName().equals("augustusGene")));
        }
    }
}
