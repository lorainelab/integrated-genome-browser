/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author aloraine
 */
public class NibbleFileParserTest {

	String input_string = "AAAAAAAAAAACCCCCCCCCGGGGGGGGGTTTT";
	String infile_name = "test/data/bnib/BNIB_chrQ.bnib";
	String outfile_name = "bnipoutput.txt";
	String seq_version = "1.0";
	String seq_name = "testseq";
	File infil = new File(infile_name);
	StringBuffer sb;
	InputStream isr;

	@Before
	public void setup() throws Exception
	{
			assertTrue(infil.exists());
			sb = new StringBuffer();
			isr = GeneralUtils.getInputStream(infil, sb);
	}

	@Test
	public void fileLoadingAndFileWriting() throws Exception
	{
		BioSeq seq = NibbleResiduesParser.parse(isr, new AnnotatedSeqGroup("Test File"));
		assertTrue(seq.getResidues().equals(input_string));
		System.out.println(input_string + " == " + seq.getResidues());
	}

	@Test
	public void randomLoadTest1() throws Exception
	{
		BioSeq seq = NibbleResiduesParser.parse(isr, new AnnotatedSeqGroup("Test File"),4,18);
		assertTrue(seq.getResidues().equals(input_string.substring(4, 18)));
		System.out.println(input_string.substring(4, 18) + " == " + seq.getResidues());
	}

	@Test
	public void randomLoadTest2() throws Exception
	{
		BioSeq seq = NibbleResiduesParser.parse(isr, new AnnotatedSeqGroup("Test File"),6,7);
		assertTrue(seq.getResidues().equals(input_string.substring(6, 7)));
		System.out.println(input_string.substring(6, 7) + " == " + seq.getResidues());
	}

	@Test
	public void randomLoadTest3() throws Exception
	{
		BioSeq seq = NibbleResiduesParser.parse(isr, new AnnotatedSeqGroup("Test File"),1,2);
		assertTrue(seq.getResidues().equals(input_string.substring(1, 2)));
		System.out.println(input_string.substring(1, 2) + " == " + seq.getResidues());

	}

	@Test
	public void randomLoadTest4() throws Exception
	{
		BioSeq seq = NibbleResiduesParser.parse(isr, new AnnotatedSeqGroup("Test File"),1,3);
		assertTrue(seq.getResidues().equals(input_string.substring(1, 3)));
		System.out.println(input_string.substring(1, 3) + " == " + seq.getResidues());

	}


}
