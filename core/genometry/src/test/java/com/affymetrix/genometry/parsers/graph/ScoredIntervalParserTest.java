package com.affymetrix.genometry.parsers.graph;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.symmetry.impl.GraphIntervalSym;
import com.affymetrix.genometry.symmetry.impl.ScoredContainerSym;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author auser
 */
public class ScoredIntervalParserTest {

    @Test
    public void testParseFromFile() throws IOException {
        String filename = "data/egr/test1.egr";
        InputStream istr = ScoredIntervalParserTest.class.getClassLoader().getResourceAsStream(filename);
        assertNotNull(istr);
        String stream_name = "chr1";
        GenomeVersion seq_group = GenometryModel.getInstance().addGenomeVersion("Test Seq Group");

        ScoredIntervalParser tester = new ScoredIntervalParser();
        tester.parse(istr, stream_name, seq_group, true);

        //System.out.println("done testing ScoredIntervalParser");
        String unique_container_name = GenomeVersion.getUniqueGraphID(stream_name, seq_group);
        assertEquals("chr1.1", unique_container_name);
    }

    @Ignore
    @Test
    public void testMakeNewSeq() {

        GenomeVersion seq_group = GenometryModel.getInstance().addGenomeVersion("Test Seq Group");
        String seqid = "chr1";

        BioSeq aseq = seq_group.getSeq(seqid);
        ScoredIntervalParser ins = new ScoredIntervalParser();

        aseq = seq_group.addSeq(seqid, 0); // hmm, should a default size be set?
        assertEquals(100208700, aseq.getLength());
        assertEquals("chr1", aseq.getId());
    }

    @Ignore
    @Test
    public void testwriteEgrFormat() throws IOException {
        String string = "# genome_version = H_sapiens_Mar_2006\n"
                + "# score0 = NormDiff\n"
                + "chr1	10015038	10016039	.	25.0\n"
                + "chr1	100004630	100005175	.	6.0\n"
                + "chr1	100087772	100088683	.	13.0\n"
                + "chr1	100207533	100208700	.	230.0\n";

        InputStream istr = new ByteArrayInputStream(string.getBytes());
        GenomeVersion seq_group = GenometryModel.getInstance().addGenomeVersion("Test Seq Group");
        String stream_name = "chr1";
        ScoredIntervalParser tester = new ScoredIntervalParser();
        tester.parse(istr, stream_name, seq_group, true);
        assertEquals(1, seq_group.getSeqCount());
        BioSeq aseq = seq_group.getSeq(0);
        assertEquals("chr1", aseq.getId());
        ScoredContainerSym symI = (ScoredContainerSym) aseq.getAnnotation(0);
        assertEquals("chr1", symI.getID());
        assertEquals(2, aseq.getAnnotationCount());
        GraphIntervalSym result = symI.makeGraphSym("NormDiff", seq_group);
        assertEquals(4, result.getChildCount());
        String genome_version = "H_sapiens_Mar_2006";
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        Boolean out = ScoredIntervalParser.writeEgrFormat(result, genome_version, outstream);
        assertEquals(true, out);
        assertEquals(string, outstream.toString());
    }
}
