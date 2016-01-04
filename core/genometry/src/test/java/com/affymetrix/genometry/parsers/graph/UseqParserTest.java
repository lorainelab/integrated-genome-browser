package com.affymetrix.genometry.parsers.graph;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.useq.USeqArchive;
import com.affymetrix.genometry.parsers.useq.USeqGraphParser;
import com.affymetrix.genometry.parsers.useq.USeqRegionParser;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;
import org.lorainelab.igb.synonymlookup.services.impl.ChromosomeSynonymLookupImpl;
import org.lorainelab.igb.synonymlookup.services.impl.GenomeVersionSynonymLookupImpl;
import org.lorainelab.igb.synonymlookup.services.impl.SpeciesSynonymsLookupImpl;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Nix
 */
public class UseqParserTest {

    @Test
    public void testRegionFileParsing() throws IOException {
        //test region parsing
        String stream_name = "chr17";
        String filename = "data/useq/chr17_H_sapiens_Mar_2006_Region.useq";
        InputStream istr = UseqParserTest.class.getClassLoader().getResourceAsStream(filename);
        assertNotNull(istr);
        GenomeVersion genomeVersion = new GenomeVersion("H_sapiens_Mar_2006");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        USeqRegionParser up = new USeqRegionParser();
        List<SeqSymmetry> results = up.parse(istr, genomeVersion, stream_name, false, null);

        //size of results
        //System.out.println("NumberRegions "+results.size());
        assertEquals(677, results.size());

        UcscBedSym sym = (UcscBedSym) results.get(5);
        SeqSpan span = sym.getSpan(0);

        //span count
        //System.out.println("SpanCount "+sym.getSpanCount());
        assertEquals(1, sym.getSpanCount());

        //start
        //System.out.println("SpanStart "+span.getMin());
        assertEquals(860852, span.getMin());

        //stop
        //System.out.println("SpanStop "+span.getMax());
        assertEquals(861836, span.getMax());

        //strand
        //System.out.println("SpanStrand "+span.isForward());
        assertEquals(true, span.isForward());

        //score
        //System.out.println("SpanScore "+sym.getScore());
        assertEquals(new Float(401.0), new Float(sym.getScore()));
    }

    @Test
    public void testGraphFileParsing() throws IOException {
        //test graph parsing
        String filename = "data/useq/chr17_H_sapiens_Mar_2006_Graph.useq";
        String stream_name = "chr17";
        InputStream istr = UseqParserTest.class.getClassLoader().getResourceAsStream(filename);
        assertNotNull(istr);
        GenometryModel gmodel = GenometryModel.getInstance();
        GenomeVersion genomeVersion = new GenomeVersion("H_sapiens_Mar_2006");
        genomeVersion.setChrSynLookup(new ChromosomeSynonymLookupImpl());
        genomeVersion.setGenomeVersionSynonymLookup(new GenomeVersionSynonymLookupImpl());
        genomeVersion.setSpeciesSynLookup(new SpeciesSynonymsLookupImpl());
        gmodel.addSeqGroup(genomeVersion);
        gmodel.setSelectedGenomeVersion(genomeVersion);

        USeqGraphParser up = new USeqGraphParser();
        List<GraphSym> results = up.parseGraphSyms(istr, gmodel, "test", null);

        //size
        //System.out.println("ResultsSize "+results.size());
        assertEquals(2, results.size());

        //fetch first graph
        GraphSym gr0 = results.get(0);

        //check stream name
        //System.out.println("StreamName "+gr0.getGraphSeq().getName());
        assertEquals(stream_name, gr0.getGraphSeq().getId());

        //check point count
        //System.out.println("PointCount "+gr0.getPointCount());
        assertEquals(20000, gr0.getPointCount());

        //check X coor
        //System.out.println("XCoor "+gr0.getGraphXCoord(3));
        assertEquals(12168484, gr0.getGraphXCoord(3));

        //check Y coor
        //System.out.println("YCoor "+gr0.getGraphYCoord(1));
        assertEquals(new Float(1), new Float(gr0.getGraphYCoord(1)));
    }

    @Test
    public void testWritingRegionsToFile() throws Exception {
        //read in data
        String filename = "data/useq/chr17_H_sapiens_Mar_2006_Region.useq";
        File regionFile = new File(UseqParserTest.class.getClassLoader().getResource(filename).getFile());
        assertTrue(regionFile.exists());
        USeqArchive archive = new USeqArchive(regionFile);

        //write some slices
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        boolean wrote = archive.writeSlicesToStream(out, "chr18", 0, 100000, false);
        assertFalse(wrote);

        wrote = archive.writeSlicesToStream(out, "chr17", 860852, 861836, true);
        assertTrue(wrote);

    }

    @Test
    public void testWritingGraphToFile() throws Exception {
        //read in data
        String filename = "data/useq/chr17_H_sapiens_Mar_2006_Graph.useq";
        File file = new File(UseqParserTest.class.getClassLoader().getResource(filename).getFile());
        assertTrue(file.exists());
        USeqArchive archive = new USeqArchive(file);

        //write some slices
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        boolean wrote = archive.writeSlicesToStream(out, "chr17", 23000000, 24000000, false);
        assertFalse(wrote);

        wrote = archive.writeSlicesToStream(out, "chr17", 13000000, 14000000, true);
        assertTrue(wrote);

    }

}
