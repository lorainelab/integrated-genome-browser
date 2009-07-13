package com.affymetrix.genometryImpl.parsers;

import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;


public class PSLParserTest {
	/**
	 * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.PSLParser.
	 */
	@Test
		public void testWriteAnnotations() {
			//System.out.println("writeAnnotations");

			String string =
		"70	1	0	0	0	0	2	165	+	EL049618	71	0	71	chr1	30432563	455031	455267	3	9,36,26,	0,9,45,	455031,455111,455241,\n" +
		"71	0	0	0	0	0	2	176	+	EL049618	71	0	71	chr1	30432563	457618	457865	3	9,36,26,	0,9,45,	457618,457715,457839,\n"
;

			InputStream istr = new ByteArrayInputStream(string.getBytes());
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			boolean annot_query = true;
			boolean annot_target = false;
			String stream_name = "test_file";
			PSLParser instance = new PSLParser();

			Collection<SeqSymmetry> syms = null;
			try {
				syms = instance.parse(istr, stream_name, group, group, annot_query, true);
			} catch (IOException ioe) {
				fail("Exception: " + ioe);
			}

			// Now we have read the data into "syms", so let's try writing it.

			BioSeq seq = group.getSeq("chr1");
			String type = "test_type";
			ByteArrayOutputStream outstream = new ByteArrayOutputStream();

			boolean result = instance.writeAnnotations(syms, seq, type, outstream);
			assertEquals(true, result);
			assertEquals(string, outstream.toString());
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
}
