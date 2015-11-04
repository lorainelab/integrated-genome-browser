package com.lorainelab.bed;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.BedParser;
import com.affymetrix.genometry.parsers.IndexWriter;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symloader.BedUtils;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;
import com.lorainelab.synonymlookup.services.impl.ChromosomeSynonymLookupImpl;
import com.lorainelab.synonymlookup.services.impl.GenomeVersionSynonymLookupImpl;
import com.lorainelab.synonymlookup.services.impl.SpeciesSynonymsLookupImpl;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class BedParserTest {

    static GenometryModel gmodel = GenometryModel.getInstance();

    @Test
    public void testParseFromFile() throws Exception {

        String filename = "data/bed/bed_01.bed";
        filename = BedParserTest.class.getClassLoader().getResource(filename).getFile();
        assertTrue(new File(filename).exists());

        InputStream istr = new FileInputStream(filename);
        DataInputStream dis = new DataInputStream(istr);
        assertNotNull(dis);

        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());

        IndexWriter parser = new BedParser();
        List result = parser.parse(dis, filename, genomeVersion);

        testFileResult(result);

        BedSymloader bed = new BedSymloader(new File(filename).toURI(), Optional.empty(), filename, genomeVersion);
        result = bed.getGenome();
        testFileResult(result);
    }

    public void testFileResult(List result) {
        assertEquals(6, result.size());

        UcscBedSym sym = (UcscBedSym) result.get(2);
        assertEquals(1, sym.getSpanCount());
        SeqSpan span = sym.getSpan(0);
        assertEquals(1790361, span.getMax());
        assertEquals(1789140, span.getMin());
        assertEquals(false, span.isForward());
        assertEquals(false, sym.hasCdsSpan());
        assertEquals(null, sym.getCdsSpan());
        assertEquals(2, sym.getChildCount());

        sym = (UcscBedSym) result.get(5);
        assertEquals(sym.hasCdsSpan(), true);
        SeqSpan cds = sym.getCdsSpan();
        assertEquals(1965425, cds.getMin());
        assertEquals(1965460, cds.getMax());
        assertEquals((float) 0, sym.getProperty("score"));
    }

    /**
     * Test of parse method, of class com.affymetrix.igb.parsers.BedParser.
     */
    @Test
    public void testParseFromString() throws Exception {

        String string
                = "591	chr2L	901490	901662	CR31656-RA	0	-	901490	901662	0	1	172,	0,\n"
                + "595	chr2L	1432710	1432921	CR31927-RA	0	+	1432710	1432920	0	1	211,	0,\n"
                + // Next line is line "2": we'll specifically test that it was read correctly
                "598	chr2L	1789140	1790361	CR31930-RA	0	-	1789140	1789140	0	2	153,1010,	0,211,\n"
                + "598	chr2L	1792056	1793268	CR31931-RA	0	-	1792056	1792056	0	2	153,1014,	0,198,\n"
                + "599	chr2L	1938088	1938159	CR31667-RA	0	-	1938088	1938088	0	1	71,	0,\n"
                + // This last line has a CDS: we'll test that it was read correctly as well
                "599	chr2L	1965425	1965498	CR31942-RA	0	+	1965425	1965460	0	1	73,	0,\n";

        InputStream istr = new ByteArrayInputStream(string.getBytes());
        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        boolean annot_seq = true;
        String stream_name = "test_file";
        boolean create_container = true;
        BedParser instance = new BedParser();

        List<SeqSymmetry> result = instance.parse(istr, gmodel, genomeVersion, annot_seq, stream_name, create_container);

        testStringResult(result);

        File tempFile = createFileFromString(string);

        BedSymloader bed = new BedSymloader(tempFile.toURI(), Optional.empty(), tempFile.getName(), genomeVersion);
        result = bed.getGenome();
        testStringResult(result);
    }

    public void testStringResult(List result) {
        assertEquals(6, result.size());

        UcscBedSym sym = (UcscBedSym) result.get(2);
        assertEquals(1, sym.getSpanCount());
        SeqSpan span = sym.getSpan(0);
        assertEquals(1790361, span.getMax());
        assertEquals(1789140, span.getMin());
        assertEquals(false, span.isForward());
        assertEquals(false, sym.hasCdsSpan());
        assertEquals(null, sym.getCdsSpan());
        assertEquals(2, sym.getChildCount());

        sym = (UcscBedSym) result.get(5);
        assertEquals(sym.hasCdsSpan(), true);
        SeqSpan cds = sym.getCdsSpan();
        assertEquals(1965425, cds.getMin());
        assertEquals(1965460, cds.getMax());
        assertEquals((float) 0, sym.getProperty("score"));
    }

    /**
     * Test of parseIntArray method, of class com.affymetrix.igb.parsers.BedParser.
     */
    @Test
    public void testParseIntArray() {

        String int_array = "1,7,8,9,10";

        int[] expResult = new int[]{1, 7, 8, 9, 10};
        int[] result = BedParser.parseIntArray(int_array);
        for (int i = 0; i < expResult.length; i++) {
            assertEquals(expResult[i], result[i]);
        }

        result = BedUtils.parseIntArray(int_array);
        for (int i = 0; i < expResult.length; i++) {
            assertEquals(expResult[i], result[i]);
        }
    }

    @Test
    public void testParseIntArrayWithWhitespace() {

        // the parser doesn't accept whitespace in the integer lists
        // (Maybe it should, but it isn't expected to need to do so.)
        String int_array = "1,7, 8,9,10";

        boolean passed = false;
        try {
            BedParser.parseIntArray(int_array);
        } catch (NumberFormatException nfe) {
            passed = true;
        }
        if (!passed) {
            fail("Expected exception was not thrown");
        }

        passed = false;
        try {
            BedUtils.parseIntArray(int_array);
        } catch (NumberFormatException nfe) {
            passed = true;
        }
        if (!passed) {
            fail("Expected exception was not thrown");
        }
    }

    /**
     * Test of makeBlockMins method, of class com.affymetrix.igb.parsers.BedParser.
     */
    @Test
    public void testMakeBlockMins() {

        int min = 100;
        int[] blockStarts = new int[]{1, 3, 4, 5, 9};

        int[] expResult = new int[]{101, 103, 104, 105, 109};
        int[] result = BedParser.makeBlockMins(min, blockStarts);
        for (int i = 0; i < expResult.length; i++) {
            assertEquals(expResult[i], result[i]);
        }

        result = BedUtils.makeBlockMins(min, blockStarts);
        for (int i = 0; i < expResult.length; i++) {
            assertEquals(expResult[i], result[i]);
        }
    }

    /**
     * Test of makeBlockMaxs method, of class com.affymetrix.igb.parsers.BedParser.
     */
    @Test
    public void testMakeBlockMaxs() {

        int[] blockMins = new int[]{1, 3, 4, 5, 9};
        int[] blockSizes = new int[]{1, 3, 4, 5, 9};

        int[] expResult = new int[]{2, 6, 8, 10, 18};
        int[] result = BedParser.makeBlockMaxs(blockMins, blockSizes);
        for (int i = 0; i < expResult.length; i++) {
            assertEquals(expResult[i], result[i]);
        }

        result = BedUtils.makeBlockMaxs(blockMins, blockSizes);
        for (int i = 0; i < expResult.length; i++) {
            assertEquals(expResult[i], result[i]);
        }
    }

    /**
     * Test of writeSymmetry method, of class com.affymetrix.igb.parsers.BedParser.
     */
    @Test
    public void testWriteBedFormat() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        BioSeq seq = genomeVersion.addSeq("chr12", 500000);
        SeqSpan span = new SimpleSeqSpan(500, 800, seq);
        SeqSpan[] span_array = new SeqSpan[]{span};
        SimpleMutableSeqSymmetry sym = new SimpleMutableSeqSymmetry();
        for (SeqSpan span_in_array : span_array) {
            sym.addSpan(span_in_array);
        }

        BedParser.writeSymmetry(dos, sym, seq);
        assertEquals("chr12\t500\t800\n", baos.toString());

        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);

        BedUtils.writeSymmetry(dos, sym, seq);
        assertEquals("chr12\t500\t800\n", baos.toString());
    }

    /**
     * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.BedParser.
     */
    @Test
    public void testWriteAnnotations() throws Exception {

        String string
                = "chr2L	901490	901662	CR31656-RA	0	-	901490	901662	0	1	172,	0,\n"
                + "chr2L	1432710	1432921	CR31927-RA	0	+	1432710	1432920	0	1	211,	0,\n"
                + "chr2L	1789140	1790361	CR31930-RA	0	-	1789140	1789140	0	2	153,1010,	0,211,\n"
                + "chr2L	1792056	1793268	CR31931-RA	0	-	1792056	1792056	0	2	153,1014,	0,198,\n"
                + "chr2L	1938088	1938159	CR31667-RA	0	-	1938088	1938088	0	1	71,	0,\n"
                + "chr2L	1965425	1965498	CR31942-RA	0	+	1965425	1965460	0	1	73,	0,\n";

        InputStream istr = new ByteArrayInputStream(string.getBytes());
        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        boolean annot_seq = true;
        String stream_name = "test_file";
        boolean create_container = true;
        BedParser instance = new BedParser();

        Collection<SeqSymmetry> syms = null;
        try {
            syms = instance.parse(istr, gmodel, genomeVersion, annot_seq, stream_name, create_container);
        } catch (IOException ioe) {
            fail("Exception: " + ioe);
        }

        BioSeq seq = genomeVersion.getSeq("chr2L");

        testWrite(syms, seq, string);

        File file = createFileFromString(string);

        BedSymloader bed = new BedSymloader(file.toURI(), Optional.empty(), file.getName(), genomeVersion);
        syms = bed.getGenome();

        testWrite(syms, seq, string);
    }

    /**
     * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.BedParser. Validate that if the genes have
     * the same name, then loading and writing them out gives the same information.
     */
    @Test
    public void testWriteAnnotations2() throws Exception {

        String string
                = "chr1	455031	455267	EL049618	0	+	455031	455267	0	3	9,36,26,	0,80,210,\n"
                + "chr1	457618	457865	EL049618	0	+	457618	457865	0	3	9,36,26,	0,97,221,\n";

        InputStream istr = new ByteArrayInputStream(string.getBytes());
        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        boolean annot_seq = true;
        String stream_name = "test_file";
        boolean create_container = true;
        BedParser instance = new BedParser();

        Collection<SeqSymmetry> syms = null;
        try {
            syms = instance.parse(istr, gmodel, genomeVersion, annot_seq, stream_name, create_container);
        } catch (IOException ioe) {
            fail("Exception: " + ioe);
        }

        // Now we have read the data into "syms", so let's try writing it.
        BioSeq seq = genomeVersion.getSeq("chr1");

        testWrite(syms, seq, string);

        File file = createFileFromString(string);

        BedSymloader bed = new BedSymloader(file.toURI(), Optional.empty(), file.getName(), genomeVersion);
        syms = bed.getGenome();

        testWrite(syms, seq, string);
    }

    public void testWrite(Collection<SeqSymmetry> syms, BioSeq seq, String expResult) {
        // Now we have read the data into "syms", so let's try writing it.
        BedParser instance = new BedParser();

        String type = "test_type";
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();

        boolean result = instance.writeAnnotations(syms, seq, type, outstream);
        assertEquals(true, result);
        assertEquals(expResult, outstream.toString());
    }

    /**
     * Test of getMimeType method, of class com.affymetrix.igb.parsers.BedParser.
     */
    @Test
    public void testGetMimeType() {

        BedParser instance = new BedParser();

        String result = instance.getMimeType();
        assertTrue("text/plain".equals(result) || "text/bed".equals(result));
    }

    public File createFileFromString(String string) throws Exception {
        File tempFile = File.createTempFile("tempFile", ".bed");
        tempFile.deleteOnExit();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true))) {
            bw.write(string);
        }
        return tempFile;
    }

    @Test
    public void testBEDParseFromFile() throws Exception {
        String filename = "data/bed/bed_02.bed";
        filename = BedParserTest.class.getClassLoader().getResource(filename).getFile();
        assertTrue(new File(filename).exists());
        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        BioSeq seq = genomeVersion.addSeq("chr2L", 1965498);

        BedSymloader bed = new BedSymloader(new File(filename).toURI(), Optional.empty(), filename, genomeVersion);

        List<BioSeq> allSeq = bed.getChromosomeList();
        assertEquals(4, allSeq.size());

        List result = bed.getChromosome(seq);
        assertEquals(6, result.size());

        UcscBedSym sym = (UcscBedSym) result.get(2);
        assertEquals(1, sym.getSpanCount());
        SeqSpan span = sym.getSpan(0);
        assertEquals(1790361, span.getMax());
        assertEquals(1789140, span.getMin());
        assertEquals(false, span.isForward());
        assertEquals(false, sym.hasCdsSpan());
        assertEquals(null, sym.getCdsSpan());
        assertEquals(2, sym.getChildCount());

        sym = (UcscBedSym) result.get(5);
        assertEquals(sym.hasCdsSpan(), true);
        SeqSpan cds = sym.getCdsSpan();
        assertEquals(1965425, cds.getMin());
        assertEquals(1965460, cds.getMax());
        assertEquals((float) 0, sym.getProperty("score"));
    }

}
