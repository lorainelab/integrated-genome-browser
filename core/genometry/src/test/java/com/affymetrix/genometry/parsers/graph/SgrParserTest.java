package com.affymetrix.genometry.parsers.graph;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.symloader.Sgr;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class SgrParserTest {

    @Test
    public void testParseFromFile() throws Exception {

        String filename = "data/sgr/test1.sgr";
        InputStream istr = SgrParserTest.class.getClassLoader().getResourceAsStream(filename);
        assertNotNull(istr);

        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        boolean annot_seq = true;
        String stream_name = "test_file";
        boolean ensure_unique_id = true;

        List<GraphSym> results = SgrParser.parse(istr, stream_name, genomeVersion, annot_seq, ensure_unique_id);

        assertEquals(1, results.size());
        GraphSym gr0 = results.get(0);

        assertEquals("16", gr0.getGraphSeq().getId());
        assertEquals(10, gr0.getPointCount());
        assertEquals(-0.0447924, gr0.getGraphYCoord(2), 0.01);
        assertEquals(0.275948, gr0.getGraphYCoord(3), 0.01);
        assertEquals(948028, gr0.getGraphXCoord(3));
    }

    @Test
    /**
     * Make sure this writes out the same format it reads in.
     */
    public void testWriteFormat() throws IOException {

        String string
                = "16	948025	0.128646\n"
                + "16	948026	0.363933\n";
        InputStream istr = new ByteArrayInputStream(string.getBytes());

        GenomeVersion genomeVersion = new GenomeVersion("Test Group");
        boolean annot_seq = true;
        String stream_name = "test_file";
        boolean ensure_unique_id = true;

        List<GraphSym> results = SgrParser.parse(istr, stream_name, genomeVersion, annot_seq, ensure_unique_id);

        ByteArrayOutputStream outstream = new ByteArrayOutputStream();

        SgrParser.writeSgrFormat(results.get(0), outstream);

        assertEquals(string, outstream.toString());
    }

    @Test
    public void testSgr() throws Exception {
        String filename = "data/sgr/test4.sgr";
        GenomeVersion seq_group = new GenomeVersion("test");
        URL url = SgrParserTest.class.getClassLoader().getResource(filename);
        Sgr sgr = new Sgr(url.toURI(), filename, seq_group);

        List<GraphSym> results = sgr.getGenome();

        assertEquals(4, results.size());
        GraphSym gr0 = results.get(0);

        assertEquals("16", gr0.getGraphSeq().getId());
        assertEquals(5, gr0.getPointCount());
        assertEquals(-0.0447924, gr0.getGraphYCoord(2), 0.01);
        assertEquals(0.275948, gr0.getGraphYCoord(3), 0.01);
        assertEquals(948028, gr0.getGraphXCoord(3));

        GraphSym gr1 = results.get(1);

        assertEquals("17", gr1.getGraphSeq().getId());
        assertEquals(4, gr1.getPointCount());
        assertEquals(0.8384833, gr1.getGraphYCoord(1), 0.01);
        assertEquals(0.4523419, gr1.getGraphYCoord(2), 0.01);
        assertEquals(948030, gr1.getGraphXCoord(2));

        GraphSym gr2 = results.get(2);

        assertEquals("18", gr2.getGraphSeq().getId());
        assertEquals(2, gr2.getPointCount());
        assertEquals(0.9203930, gr2.getGraphYCoord(1), 0.01);
        assertEquals(0.2789456, gr2.getGraphYCoord(0), 0.01);
        assertEquals(948033, gr2.getGraphXCoord(0));

        GraphSym gr3 = results.get(3);

        assertEquals("19", gr3.getGraphSeq().getId());
        assertEquals(8, gr3.getPointCount());
        assertEquals(-0.0447924, gr3.getGraphYCoord(2), 0.01);
        assertEquals(0.275948, gr3.getGraphYCoord(3), 0.01);
        assertEquals(948028, gr3.getGraphXCoord(3));
    }

    @Test
    public void testWriteAnnotation() throws Exception {
        String string
                = "16	948025	0.128646\n"
                + "16	948026	0.363933\n";

        File file = createFileFromString(string);
        GenomeVersion seq_group = new GenomeVersion("test");
        Sgr sgr = new Sgr(file.toURI(), file.getName(), seq_group);
        List<GraphSym> results = sgr.getGenome();

        ByteArrayOutputStream outstream = new ByteArrayOutputStream();

        sgr.writeAnnotations(results, null, null, outstream);

        assertEquals(string, outstream.toString());
    }

    public File createFileFromString(String string) throws Exception {
        File tempFile = File.createTempFile("tempFile", ".sgr");
        tempFile.deleteOnExit();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true))) {
            bw.write(string);
        }
        return tempFile;
    }
}
