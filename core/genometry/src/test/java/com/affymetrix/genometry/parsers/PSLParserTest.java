package com.affymetrix.genometry.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.comparator.UcscPslComparator;
import com.affymetrix.genometry.symloader.PSL;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscPslSym;
import com.google.code.externalsorting.ExternalMergeSort;
import com.lorainelab.synonymlookup.services.impl.ChromosomeSynonymLookupImpl;
import com.lorainelab.synonymlookup.services.impl.GenomeVersionSynonymLookupImpl;
import com.lorainelab.synonymlookup.services.impl.SpeciesSynonymsLookupImpl;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class PSLParserTest {

    /**
     * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.PSLParser.
     */
    @Test
    public void testWriteAnnotations() throws Exception {
        //System.out.println("writeAnnotations");

        String string
                = "70	1	0	0	0	0	2	165	+	EL049618	71	0	71	chr1	30432563	455031	455267	3	9,36,26,	0,9,45,	455031,455111,455241,\n"
                + "71	0	0	0	0	0	2	176	+	EL049618	71	0	71	chr1	30432563	457618	457865	3	9,36,26,	0,9,45,	457618,457715,457839,\n";

        InputStream istr = new ByteArrayInputStream(string.getBytes());
        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        boolean annot_query = true;
        String stream_name = "test_file";
        PSLParser instance = new PSLParser();

        List<UcscPslSym> syms = null;
        try {
            syms = instance.parse(istr, stream_name, genomeVersion, genomeVersion, annot_query, true);
        } catch (IOException ioe) {
            fail("Exception: " + ioe);
        }

        Collections.sort(syms, new UcscPslComparator());
        // Now we have read the data into "syms", so let's try writing it.

        BioSeq seq = genomeVersion.getSeq("chr1");
        String type = "test_type";
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();

        boolean result = instance.writeAnnotations(syms, seq, type, outstream);
        assertEquals(true, result);
        assertEquals(string, outstream.toString());

        File file = createFileFromString(string);
        genomeVersion = new GenomeVersion("Test Group");
        PSL psl = new PSL(file.toURI(), Optional.empty(), stream_name, genomeVersion, null, null,
                true, false, false);
        psl.setExternalSortService(new ExternalMergeSort());
        syms = psl.getGenome();
        seq = genomeVersion.getSeq("chrl");
        outstream = new ByteArrayOutputStream();
        result = psl.writeAnnotations(syms, seq, type, outstream);
        assertEquals(true, result);
        assertEquals(string, outstream.toString());

    }

    /**
     * Test of writeAnnotations method, of class com.affymetrix.genometry.symloader.PSL
     */
    public void testPslxFile() throws Exception {
        String pslxString = "117	2	0	0	1	2	1	1	+	query	121	0	121	target	120	0	120	3	57,36,26,	0,57,95,	0,58,94,		"
                + "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT,AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA,CCCCCCCCCCCCCCCCCCCCCCCCCC,";
        String stream_name = "test_file";
        String type = "test_type";

        File file = createFileFromString(pslxString);
        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        PSL psl = new PSL(file.toURI(), Optional.empty(), stream_name, genomeVersion, null, null,
                true, false, false);
        List<UcscPslSym> syms = psl.getGenome();
        BioSeq seq = genomeVersion.getSeq("target");
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        boolean result = psl.writeAnnotations(syms, seq, type, outstream);
        assertEquals(true, result);
        System.out.println(outstream.toString());
        assertEquals(pslxString, outstream.toString());
    }

    @Test
    public void testFiles() throws Exception {
        testFile("data/psl/test1.psl");
        testFile("data/pslx/test.pslx");
    }

    /**
     * @return the symmetries that match the given chromosome.
     */
    public List<SeqSymmetry> filterResultsByChromosome(List<? extends SeqSymmetry> genomeResults, BioSeq seq) {
        List<SeqSymmetry> results = new ArrayList<>();
        for (SeqSymmetry sym : genomeResults) {
            BioSeq seq2 = null;
            if (sym instanceof UcscPslSym) {
                seq2 = ((UcscPslSym) sym).getTargetSeq();
            } else {
                seq2 = sym.getSpanSeq(0);
            }
            if (seq.equals(seq2)) {
                results.add(sym);
            }
        }
        return results;
    }

    private void testFile(String filename) throws Exception {

        InputStream istr = PSLParserTest.class.getClassLoader().getResourceAsStream(filename);
        assertNotNull(istr);
        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        BioSeq seq = null;

        PSLParser instance = new PSLParser();
        List<UcscPslSym> syms = instance.parse(istr, filename, null, genomeVersion, true, true);
        Collections.sort(syms, new UcscPslComparator());

        PSL psl = new PSL(PSLParserTest.class.getClassLoader().getResource(filename).toURI(), Optional.empty(), filename, genomeVersion, null, null,
                true, true, false);
        psl.setExternalSortService(new ExternalMergeSort());
        List<BioSeq> seqs = psl.getChromosomeList();

        List<SeqSymmetry> syms1 = null;
        List<UcscPslSym> syms2 = null;

        for (BioSeq seq1 : seqs) {
            seq = seq1;
            syms1 = filterResultsByChromosome(syms, seq);
            syms2 = psl.getChromosome(seq);
            testSeqSymmetry(syms1, syms2);
        }

    }

    private void testSeqSymmetry(List<? extends SeqSymmetry> syms1, List<? extends SeqSymmetry> syms2) {
        assertEquals(syms1.size(), syms2.size());
        SeqSymmetry sym1, sym2;
        for (int i = 0; i < syms1.size(); i++) {
            sym1 = syms1.get(i);
            sym2 = syms2.get(i);

            assertEquals(sym1.getID(), sym2.getID());
            assertEquals(sym1.getChildCount(), sym2.getChildCount());
            assertEquals(sym1.getSpanCount(), sym2.getSpanCount());

            for (int j = 0; j < sym1.getSpanCount(); j++) {
                SeqSpan span1 = sym1.getSpan(j);
                SeqSpan span2 = sym2.getSpan(j);
                assertEquals(span1.getBioSeq().getId(), span1.getBioSeq().getId());
                assertEquals(span1.getMinDouble(), span2.getMinDouble(), 0);
                assertEquals(span1.getMaxDouble(), span2.getMaxDouble(), 0);
            }
        }
    }

    /**
     * Test of getMimeType method.
     */
    @Test
    public void testGetMimeType() {
        //System.out.println("getMimeType");

        PSLParser instance = new PSLParser();

        String result = instance.getMimeType();
        assertTrue("text/plain".equals(result));
    }

    public static File createFileFromString(String string) throws Exception {
        File tempFile = File.createTempFile("tempFile", ".psl");
        tempFile.deleteOnExit();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true))) {
            bw.write(string);
        }
        return tempFile;
    }
}
