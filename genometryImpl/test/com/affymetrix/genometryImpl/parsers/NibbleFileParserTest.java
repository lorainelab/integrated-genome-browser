
package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hiralv
 */
public class NibbleFileParserTest {

	String input_string = "AAAAAAAAAAACCCCCCCCCGGGGGGGGGTTTT";
	String infile_name = "test/data/bnib/BNIB_chrQ.bnib";
	String outfile_name = "test/data/bnib/bnibTest.bnib";
	String seq_version = "1.0";
	String seq_name = "testseq";
	File infile = new File(infile_name);
	StringBuffer sb;
	InputStream isr;
	int total_residues = input_string.length();

	@Before
	public void setup() throws Exception
	{
			assertTrue(infile.exists());
	}

	@Test
	public void testCases() throws Exception
	{
		testCase(0,input_string.length());
		testCase(4,18);						// even, even
		testCase(6,7);						// even, odd
		testCase(1,4);						// odd , even
		testCase(1,5);						// odd , odd
		testCase(-1,3);
		testCase(11,total_residues+4);
		testCase(-5,total_residues+5);
	}

	public void testCase(int start, int end) throws Exception
	{
		sb = new StringBuffer();
		isr = GeneralUtils.getInputStream(infile, sb);
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		boolean result = NibbleResiduesParser.parse(isr, new AnnotatedSeqGroup("Test"),start,end,outstream);

		start = Math.max(0,start);
		start = Math.min(total_residues, start);

		end = Math.max(0, end);
		end = Math.min(total_residues, end);
	
		assertTrue(result);
		assertEquals(input_string.substring(start, end),outstream.toString());
		outstream.close();
	}

}
