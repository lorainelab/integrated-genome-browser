package com.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lorainelab.igb.synonymlookup.services.impl.ChromosomeSynonymLookupImpl;
import org.lorainelab.igb.synonymlookup.services.impl.GenomeVersionSynonymLookupImpl;
import org.lorainelab.igb.synonymlookup.services.impl.SpeciesSynonymsLookupImpl;
import org.lorainelab.igb.ucsc.rest.api.service.UCSCRestSymLoader;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class UCSCRestSymloaderTest {

    private final static String UCSC_REST_URL = "https://api.genome.ucsc.edu/";
    private static final String HUMAN_GENOME_ID = "hg38";
    public static final String GENE_PRED = "genePred";
    public static final String PSL = "psl";
    public static final String BED= "bed";
    public static final String BIG_WIG = "bigWig";
    public static final String WIG = "wig";
    private URIBuilder uriBuilder;
    private GenomeVersion genomeVersion;
    private SeqSpan span;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        genomeVersion = new GenomeVersion(HUMAN_GENOME_ID);
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        span = getTestSeqSpan();
        uriBuilder = new URIBuilder(UCSC_REST_URL+"/getData/track");
        uriBuilder.addParameter("genome", HUMAN_GENOME_ID);
    }

    @Test
    public void getRegionForGenePredTest() throws Exception {
        String trackGenePred = "augustusGene";
        uriBuilder.addParameter("track", trackGenePred);
        UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                trackGenePred, GENE_PRED, genomeVersion, HUMAN_GENOME_ID);
        List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
        assertFalse(region.isEmpty());
    }

    @Test
    public void getRegionForPslTest() throws Exception {
        String trackPsl = "xenoMrna";
        uriBuilder.addParameter("track", trackPsl);
        UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                trackPsl, PSL, genomeVersion, HUMAN_GENOME_ID);
        List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
        assertFalse(region.isEmpty());
    }

    @Test
    public void getRegionForBedTest() throws Exception {
        String trackBed = "cloneEndRP11";
        uriBuilder.addParameter("track", trackBed);
        UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                trackBed, BED, genomeVersion, HUMAN_GENOME_ID);
        List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
        assertFalse(region.isEmpty());
    }

    @Test
    public void getRegionFoBigWigTest() throws Exception {
        String trackBigWig = "ReMapDensity";
        uriBuilder.addParameter("track", trackBigWig);
        UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                trackBigWig, BIG_WIG, genomeVersion, HUMAN_GENOME_ID);
        List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
        assertFalse(region.isEmpty());
    }

    @Test
    public void getRegionForWigTest() throws Exception {
        String trackWig = "phastCons100way";
        uriBuilder.addParameter("track", trackWig);
        UCSCRestSymLoader ucscRestSymLoader = new UCSCRestSymLoader(UCSC_REST_URL, uriBuilder.build(), Optional.empty(),
                trackWig, WIG, genomeVersion, HUMAN_GENOME_ID);
        List<? extends SeqSymmetry> region = ucscRestSymLoader.getRegion(span);
        assertFalse(region.isEmpty());
    }

    private SeqSpan getTestSeqSpan() {
        SeqSpan seqSpan = new SeqSpan() {

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
        return seqSpan;
    }
}
