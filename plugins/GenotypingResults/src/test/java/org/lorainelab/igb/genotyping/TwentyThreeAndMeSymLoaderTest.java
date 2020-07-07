/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.genotyping;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.LoadUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lorainelab.igb.synonymlookup.services.impl.ChromosomeSynonymLookupImpl;
import org.lorainelab.igb.synonymlookup.services.impl.GenomeVersionSynonymLookupImpl;
import org.lorainelab.igb.synonymlookup.services.impl.SpeciesSynonymsLookupImpl;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import org.apache.commons.lang3.reflect.TypeUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.logging.Logger;

/**
 *
 * @author noorzahara
 */
public class TwentyThreeAndMeSymLoaderTest {

    private static TwentyThreeAndMeSymLoader twentyThreeAndMeSymLoaderUnderTest;
    private static GenomeVersion mockGenomeVersion;
    public static final String TEST_FILE_NAME = TwentyThreeAndMeSymLoaderTest.class.getClassLoader().getResource("test.23andMe").getFile();
    public static BioSeq seq;
    Logger log = Logger.getLogger(TwentyThreeAndMeSymLoaderTest.class.getName());

    @BeforeClass
    public static void setUpClass() {
        assertTrue(new File(TEST_FILE_NAME).exists());
        mockGenomeVersion = new GenomeVersion("Human_Dec_2013");
        mockGenomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        mockGenomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        mockGenomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        seq = mockGenomeVersion.addSeq("chr1", Integer.MAX_VALUE);
        twentyThreeAndMeSymLoaderUnderTest = new TwentyThreeAndMeSymLoader(new File(TEST_FILE_NAME).toURI(), Optional.empty(), "sample23andMe.23andMe", mockGenomeVersion);
    }

    /**
     * Test of getChromosomeList method, of class TwentyThreeAndMeSymLoader.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetChromosomeList() throws Exception {
        log.log(Level.INFO, "getChromosomeList");
        List<BioSeq> expResult = Arrays.asList(seq);
        List<BioSeq> result = twentyThreeAndMeSymLoaderUnderTest.getChromosomeList();
        assertEquals(expResult, result);
    }

    /**
     * Test of getLoadChoices method, of class TwentyThreeAndMeSymLoader.
     */
    @Test
    public void testGetLoadChoices() {
        log.log(Level.INFO, "getLoadChoices");
        List<LoadUtils.LoadStrategy> expResult = Arrays.asList(LoadUtils.LoadStrategy.NO_LOAD, LoadUtils.LoadStrategy.VISIBLE,
                LoadUtils.LoadStrategy.GENOME);
        List<LoadUtils.LoadStrategy> result = twentyThreeAndMeSymLoaderUnderTest.getLoadChoices();
        assertEquals(expResult, result);
    }

    /**
     * Test of parse method, of class TwentyThreeAndMeSymLoader.
     */
    @Test
    public void testParse() {
        log.log(Level.INFO, "parse");
        List<SeqSymmetry> result = twentyThreeAndMeSymLoaderUnderTest.parse(seq, 0, Integer.MAX_VALUE);
        assertEquals(1, result.size());
        assertTrue(TypeUtils.isInstance(result.get(0), TwentyThreeAndMeVariationSym.class));
        TwentyThreeAndMeVariationSym twentyThreeAndMeVariationSym = (TwentyThreeAndMeVariationSym) result.get(0);
        assertEquals(seq, twentyThreeAndMeVariationSym.getBioSeq());
        assertEquals(82154, twentyThreeAndMeVariationSym.getBlockMaxs()[0]);
        assertEquals(82152, twentyThreeAndMeVariationSym.getBlockMins()[0]);
        Map<String, Object> props = twentyThreeAndMeVariationSym.getProperties();
        assertNotNull(props);
        assertEquals("rs4477212", props.get("name"));
        assertEquals("1", props.get("chrom"));
        assertEquals("82154", props.get("seq_name"));
        assertEquals(82152, props.get("chromStart"));
        assertEquals(82154, props.get("chromEnd"));
        assertEquals("AA", props.get("genotype"));
    }

    /**
     * Test of parseLineToDataModel method, of class TwentyThreeAndMeSymLoader.
     */
    @Test
    public void testParseLineToDataModel() {
        log.log(Level.INFO, "parseLineToDataModel");
        List<String> fields = Arrays.asList("rs4477212", "1", "82154", "AA");
        assertTrue(twentyThreeAndMeSymLoaderUnderTest.parseLineToDataModel(fields, 0, Integer.MAX_VALUE, new ArrayList<>()));
    }

    /**
     * Test of isMultiThreadOK method, of class TwentyThreeAndMeSymLoader.
     */
    @Test
    public void testIsMultiThreadOK() {
        log.log(Level.INFO, "isMultiThreadOK");
        boolean result = twentyThreeAndMeSymLoaderUnderTest.isMultiThreadOK();
        assertTrue(result);
    }

}
