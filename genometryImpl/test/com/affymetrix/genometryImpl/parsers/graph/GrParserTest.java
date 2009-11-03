package com.affymetrix.genometryImpl.parsers.graph;

import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;

public class GrParserTest {

	@Test
	public void testParseFromFile() throws IOException {

		String filename = "test/data/gr/test1.gr";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		String stream_name = "test_file";
		boolean ensure_unique_id = true;

		BioSeq aseq = group.addSeq(stream_name, 1000);

		GraphSym gr0 = GrParser.parse(istr, aseq, filename, ensure_unique_id);

		assertEquals(stream_name, gr0.getGraphSeq().getID());
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

		String string =
				"948025	0.128646\n" +
				"948026	0.363933\n";
		InputStream istr = new ByteArrayInputStream(string.getBytes());

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		String stream_name = "test_file";
		boolean ensure_unique_id = true;


		BioSeq aseq = group.addSeq(stream_name, 1000);


		GraphSym gr0 = GrParser.parse(istr, aseq, stream_name, ensure_unique_id);

		ByteArrayOutputStream outstream = new ByteArrayOutputStream();

		GrParser.writeGrFormat(gr0, outstream);

		assertEquals(string, outstream.toString());
	}
}

