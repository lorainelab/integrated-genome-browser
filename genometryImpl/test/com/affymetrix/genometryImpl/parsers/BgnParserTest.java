package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.span.SimpleSeqSpan;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

import com.affymetrix.genometryImpl.BioSeq;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author jnicol
 */
public class BgnParserTest {
	@Test
		public void TestParseBgn()  throws Exception {
			String filename = "test/data/bgn/mgcGenes.bgn";
			FileInputStream istr = new FileInputStream(filename);
			BufferedInputStream bis =new BufferedInputStream(istr);
			DataInputStream dis = new DataInputStream(bis);
			IndexWriter iWriter = new BgnParser();

			AnnotatedSeqGroup genome = new AnnotatedSeqGroup("testGenome");
			List<SeqSymmetry> results = iWriter.parse(dis, filename, genome);

			assertEquals(6010, results.size());
	}
}
