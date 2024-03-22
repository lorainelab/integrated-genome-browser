package com.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.google.gson.Gson;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lorainelab.igb.synonymlookup.services.impl.ChromosomeSynonymLookupImpl;
import org.lorainelab.igb.synonymlookup.services.impl.GenomeVersionSynonymLookupImpl;
import org.lorainelab.igb.synonymlookup.services.impl.SpeciesSynonymsLookupImpl;
import org.lorainelab.igb.ucsc.rest.api.service.UCSCRestSymLoader;
import org.lorainelab.igb.ucsc.rest.api.service.model.ChromosomeData;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UCSCRestSymloaderTest {

    private final static String UCSC_REST_URL = "https://api.genome.ucsc.edu";
    private static final String HUMAN_GENOME_ID = "hg38";
    public static final String GENE_PRED = "genePred";
    public static final String PSL = "psl";
    public static final String BED= "bed";
    public static final String BIG_WIG = "bigWig";
    public static final String WIG = "wig";
    public static final String NARROW_PEAK = "narrowPeak";
    private URIBuilder uriBuilder;
    private static GenomeVersion genomeVersion;
    private static SeqSpan span;
    private static ChromosomeData chromosomeData;
    private static String chromosomeURl;
    private final CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
    public static final String genePred_track_test_file = "genePred-track-data.json";
    public static final String bed_track_test_file = "bed-track-data.json";
    public static final String psl_track_test_file = "psl-track-data.json";
    public static final String bigWig_track_test_file = "bigWig-track-data.json";
    public static final String wig_track_test_file = "wig-track-data.json";
    public static final String narrowPeak_track_test_file = "narrowPeak-track-data.json";

    @BeforeAll
    public static void initialise() {
        genomeVersion = new GenomeVersion(HUMAN_GENOME_ID);
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        span = getTestSeqSpan();
        Map<String, Integer> chromosomeMap = new HashMap<>();
        chromosomeMap.put("chr1", 248956422);
        chromosomeData = new ChromosomeData();
        chromosomeData.setChromosomes(chromosomeMap);
        chromosomeURl = "https://api.genome.ucsc.edu/list/chromosomes?genome=hg38";
    }

    @BeforeEach
    public void setUp() throws URISyntaxException {
        uriBuilder = new URIBuilder(UCSC_REST_URL+"/getData/track");
        uriBuilder.addParameter("genome", HUMAN_GENOME_ID);
    }

    @Test
    public void getRegionForGenePredTest() throws Exception {
        try (MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class)) {
            mockedStatic.when(HttpClients::createDefault).thenReturn(mockHttpClient);
            String filename = Objects.requireNonNull(RestApiDataProviderTest.class.getClassLoader().getResource(genePred_track_test_file)).getFile();
            String mockResponse = Files.readString(Paths.get(filename));
            String apiUrl = "https://api.genome.ucsc.edu/getData/track?genome=hg38&track=augustusGene&chrom=1&start=2000&end=200499";
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(apiUrl)), any(ResponseHandler.class)))
                    .thenReturn(mockResponse);
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(chromosomeURl)), any(ResponseHandler.class)))
                    .thenReturn(new Gson().toJson(chromosomeData));
            String trackGenePred = "augustusGene";
            uriBuilder.addParameter("track", trackGenePred);
            UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                    trackGenePred, GENE_PRED, genomeVersion, HUMAN_GENOME_ID);
            List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
            assertFalse(region.isEmpty());
        }
    }

    @Test
    public void getRegionForPslTest() throws Exception {
        try (MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class)) {
            mockedStatic.when(HttpClients::createDefault).thenReturn(mockHttpClient);
            String filename = Objects.requireNonNull(RestApiDataProviderTest.class.getClassLoader().getResource(psl_track_test_file)).getFile();
            String mockResponse = Files.readString(Paths.get(filename));
            String apiUrl = "https://api.genome.ucsc.edu/getData/track?genome=hg38&track=xenoMrna&chrom=1&start=2000&end=200499";
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(apiUrl)), any(ResponseHandler.class)))
                    .thenReturn(mockResponse);
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(chromosomeURl)), any(ResponseHandler.class)))
                    .thenReturn(new Gson().toJson(chromosomeData));
            String trackPsl = "xenoMrna";
            uriBuilder.addParameter("track", trackPsl);
            UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                    trackPsl, PSL, genomeVersion, HUMAN_GENOME_ID);
            List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
            assertFalse(region.isEmpty());
        }
    }

    @Test
    public void getRegionForBedTest() throws Exception {
        try (MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class)) {
            mockedStatic.when(HttpClients::createDefault).thenReturn(mockHttpClient);
            String filename = Objects.requireNonNull(RestApiDataProviderTest.class.getClassLoader().getResource(bed_track_test_file)).getFile();
            String mockResponse = Files.readString(Paths.get(filename));
            String apiUrl = "https://api.genome.ucsc.edu/getData/track?genome=hg38&track=cloneEndRP11&chrom=1&start=2000&end=200499";
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(apiUrl)), any(ResponseHandler.class)))
                    .thenReturn(mockResponse);
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(chromosomeURl)), any(ResponseHandler.class)))
                    .thenReturn(new Gson().toJson(chromosomeData));
            String trackBed = "cloneEndRP11";
            uriBuilder.addParameter("track", trackBed);
            UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                    trackBed, BED, genomeVersion, HUMAN_GENOME_ID);
            List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
            assertFalse(region.isEmpty());
        }
    }

    @Test
    public void getRegionFoBigWigTest() throws Exception {
        try (MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class)) {
            mockedStatic.when(HttpClients::createDefault).thenReturn(mockHttpClient);
            String filename = Objects.requireNonNull(RestApiDataProviderTest.class.getClassLoader().getResource(bigWig_track_test_file)).getFile();
            String mockResponse = Files.readString(Paths.get(filename));
            String apiUrl = "https://api.genome.ucsc.edu/getData/track?genome=hg38&track=ReMapDensity&chrom=1&start=2000&end=200499";
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(apiUrl)), any(ResponseHandler.class)))
                    .thenReturn(mockResponse);
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(chromosomeURl)), any(ResponseHandler.class)))
                    .thenReturn(new Gson().toJson(chromosomeData));
            String trackBigWig = "ReMapDensity";
            uriBuilder.addParameter("track", trackBigWig);
            UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                    trackBigWig, BIG_WIG, genomeVersion, HUMAN_GENOME_ID);
            List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
            assertFalse(region.isEmpty());
        }
    }

    @Test
    public void getRegionForWigTest() throws Exception {
        try (MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class)) {
            mockedStatic.when(HttpClients::createDefault).thenReturn(mockHttpClient);
            String filename = Objects.requireNonNull(RestApiDataProviderTest.class.getClassLoader().getResource(wig_track_test_file)).getFile();
            String mockResponse = Files.readString(Paths.get(filename));
            String apiUrl = "https://api.genome.ucsc.edu/getData/track?genome=hg38&track=phastCons100way&chrom=1&start=2000&end=200499";
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(apiUrl)), any(ResponseHandler.class)))
                    .thenReturn(mockResponse);
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(chromosomeURl)), any(ResponseHandler.class)))
                    .thenReturn(new Gson().toJson(chromosomeData));
            String trackWig = "phastCons100way";
            uriBuilder.addParameter("track", trackWig);
            UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                    trackWig, WIG, genomeVersion, HUMAN_GENOME_ID);
            List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
            assertFalse(region.isEmpty());
        }
    }

    @Test
    public void getRegionForNarrowPeakTest() throws Exception {
        try (MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class)) {
            mockedStatic.when(HttpClients::createDefault).thenReturn(mockHttpClient);
            String filename = Objects.requireNonNull(RestApiDataProviderTest.class.getClassLoader().getResource(narrowPeak_track_test_file)).getFile();
            String mockResponse = Files.readString(Paths.get(filename));
            String apiUrl = "https://api.genome.ucsc.edu/getData/track?genome=hg38&track=encTfChipPkENCFF865QLX&chrom=1&start=2000&end=200499";
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(apiUrl)), any(ResponseHandler.class)))
                    .thenReturn(mockResponse);
            when(mockHttpClient.execute(Mockito.argThat(httpget ->
                    httpget instanceof HttpGet && httpget.getURI().toString().equals(chromosomeURl)), any(ResponseHandler.class)))
                    .thenReturn(new Gson().toJson(chromosomeData));
            String trackNarrowPeak = "encTfChipPkENCFF865QLX";
            uriBuilder.addParameter("track", trackNarrowPeak);
            UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                    trackNarrowPeak, NARROW_PEAK, genomeVersion, HUMAN_GENOME_ID);
            List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
            assertFalse(region.isEmpty());
        }
    }

    private static SeqSpan getTestSeqSpan() {
        return new SeqSpan() {

            @Override
            public int getStart() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public int getEnd() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public int getMin() {
                return 2000;
            }

            @Override
            public int getMax() {
                return 200500;
            }

            @Override
            public int getLength() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean isForward() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BioSeq getBioSeq() {
                return new BioSeq("1", 100000);
            }

            @Override
            public double getStartDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double getEndDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double getMaxDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double getMinDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double getLengthDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean isIntegral() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }
}
