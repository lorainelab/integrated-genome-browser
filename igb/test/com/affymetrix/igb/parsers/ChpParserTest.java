package com.affymetrix.igb.parsers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChpParserTest {

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void TestParseFromFile() throws IOException {


		 // unzip this file.
		String zippedFileStr="../genometryImpl/test/data/chp/TisMap_Brain_01_v1_WTGene1.rma-gene-default.chp.gz";
		assertTrue(new File(zippedFileStr).exists());

				/*
		 * Sample code for testing.
		 *
		 * 
		String newFileStr="../genometryImpl/test/data/chp/TisMap_Brain_01_v1_WTGene1.rma-gene-default.chp";
		assertTrue(new File(newFileStr).exists());

		// Delete on exit.
		File newFile = new File(newFileStr);
		newFile.deleteOnExit();

		// read in new file and parse it.
		List<? extends SeqSymmetry> results = ChpParser.parse(newFileStr, false);
		assertNotNull(results);

		assertEquals(10, results.size());  // or whatever it is

		 *
		 */
		
	}
}
