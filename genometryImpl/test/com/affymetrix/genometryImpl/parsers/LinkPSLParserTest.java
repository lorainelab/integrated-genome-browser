package com.affymetrix.genometryImpl.parsers;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;


public class LinkPSLParserTest {
	/**
	 * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.PSLParser.
	 */
	@Test
	public void testWriteAnnotations() {

			DataInputStream istr = null;
		try {
			String filename = "test/data/psl/RT_U34.link.psl.gz";
			String type = "testType";
			String consensusType = "RT_U34 " + ProbeSetDisplayPlugin.CONSENSUS_TYPE;
			assertTrue(new File(filename).exists());

			istr = new DataInputStream(new FileInputStream(filename));
			GZIPInputStream gzstr = new GZIPInputStream(istr);

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

			BioSeq seq = group.addSeq("chr1",1);

			PSLParser parser = new PSLParser();
			parser.setIsLinkPsl(true);
			parser.enableSharedQueryTarget(true);
			parser.setCreateContainerAnnot(true);

			List result = parser.parse(gzstr, type, null, group, null, false, true, false);
			assertNotNull(result);
			assertEquals(2103,result.size());	// all types of symmetries

			Set<String> keySet = group.getSymmetryIDs();
			List<SeqSymmetry> consensusSyms = new ArrayList<SeqSymmetry>();
			for (String key : keySet) {
				consensusSyms.addAll(group.findSyms(key));
			}
			assertEquals(1131, consensusSyms.size());	// only the consensus symmetries

			ByteArrayOutputStream outstream = null;
			ProbeSetDisplayPlugin parser2 = null;

			outstream = new ByteArrayOutputStream();
			parser2 = new ProbeSetDisplayPlugin();
			parser2.writeAnnotations(consensusSyms, seq, consensusType, outstream);

			// 1131 consensus syms.
			// 972 probeset symmetries, of which 879 are actually used by the consensus symmetries.
			// 2 "track" lines.
			// 1131 + 879 + 2
			assertEquals(1131 + 879 + 2, outstream.toString().split("\n").length);

		} catch (Exception ex) {
			Logger.getLogger(LinkPSLParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} finally {
			try {
				istr.close();
			} catch (IOException ex) {
				Logger.getLogger(LinkPSLParserTest.class.getName()).log(Level.SEVERE, null, ex);
				fail();
			}
		}
	}
}

