package com.affymetrix.genometryImpl.parsers;

import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.symloader.PSL;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;


public class PSLParserTest {
	/**
	 * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.PSLParser.
	 */
	@Test
		public void testWriteAnnotations() throws Exception {
			//System.out.println("writeAnnotations");

			String string =
		"70	1	0	0	0	0	2	165	+	EL049618	71	0	71	chr1	30432563	455031	455267	3	9,36,26,	0,9,45,	455031,455111,455241,\n" +
		"71	0	0	0	0	0	2	176	+	EL049618	71	0	71	chr1	30432563	457618	457865	3	9,36,26,	0,9,45,	457618,457715,457839,\n"
;

			InputStream istr = new ByteArrayInputStream(string.getBytes());
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			boolean annot_query = true;
			String stream_name = "test_file";
			PSLParser instance = new PSLParser();

			Collection<UcscPslSym> syms = null;
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

//			File file = createFileFromString(string);
//			group = new AnnotatedSeqGroup("Test Group");
//			PSL psl = new PSL(file.toURI(), stream_name, group, null, null,
//				true, false, false);
//			syms = psl.getGenome();
//			seq = group.getSeq("chrl");
//			outstream = new ByteArrayOutputStream();
//			result = psl.writeAnnotations(syms, seq, type, outstream);
//			assertEquals(true, result);
//			assertEquals(string, outstream.toString());

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
	
	public static File createFileFromString(String string) throws Exception{
		File tempFile = File.createTempFile("tempFile", ".psl");
		tempFile.deleteOnExit();
		BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true));
		bw.write(string);
		bw.close();
		return tempFile;
	}
}
