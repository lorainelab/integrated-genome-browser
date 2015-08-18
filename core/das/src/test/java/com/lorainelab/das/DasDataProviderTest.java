package com.lorainelab.das;

import com.affymetrix.genometry.GenomeVersion;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class DasDataProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(DasDataProviderTest.class);
    private final static String UCSC_DAS_URL = "http://genome.cse.ucsc.edu/cgi-bin/das/dsn";
    private static final String HUMAN_GENOME_ID = "hg38";
    private static DasDataProvider dasDataProvider;

    @BeforeClass
    public static void setup() {
        GenomeVersion genomeVersion = new GenomeVersion("sample");
        dasDataProvider = new DasDataProvider(UCSC_DAS_URL, "UCSC", 1, genomeVersion.getDefSynLookup());

    }

    @Test
    public void retrieveSupportedGenomeVersions() {
        Assert.assertTrue(dasDataProvider.getSupportedGenomeVersionNames().contains(HUMAN_GENOME_ID));
//        dasDataProvider.getSupportedGenomeVersionNames().forEach(logger::info);
    }

    @Test
    public void retrieveAssemblyInfo() {
        GenomeVersion genomeVersion = new GenomeVersion(HUMAN_GENOME_ID);
        final String validChromosomeName = "1";
        Assert.assertTrue(dasDataProvider.getAssemblyInfo(genomeVersion).containsKey(validChromosomeName));
//        dasDataProvider.getAssemblyInfo(genomeVersion).entrySet().forEach(entry -> logger.info(entry.toString()));
    }
}
