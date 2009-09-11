package com.affymetrix.genometryImpl.parsers.graph;

import com.affymetrix.genometryImpl.parsers.graph.SgrParser;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;

import java.io.*;
import java.util.*;

public class SgrParserTest {

	@Test
	public void testParseFromFile() throws IOException {

		String filename = "test/data/sgr/test1.sgr";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		String stream_name = "test_file";
		boolean ensure_unique_id = true;

		List<GraphSym> results = SgrParser.parse(istr, stream_name, group, annot_seq, ensure_unique_id);

		assertEquals(1, results.size());
		GraphSym gr0 = results.get(0);

		assertEquals("16", gr0.getGraphSeq().getID());
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
						"16	948025	0.128646\n" +
						"16	948026	0.363933\n";
		InputStream istr = new ByteArrayInputStream(string.getBytes());

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		String stream_name = "test_file";
		boolean ensure_unique_id = true;

		List<GraphSym> results = SgrParser.parse(istr, stream_name, group, annot_seq, ensure_unique_id);

		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		
		SgrParser.writeSgrFormat(results.get(0), outstream);

		assertEquals(string, outstream.toString());
	}
}
