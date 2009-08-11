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
public class BrsParserTest {
	@Test
		public void TestParseBrs()  throws Exception {
			String filename = "test/data/brs/refseq.brs";
			FileInputStream istr = new FileInputStream(filename);
			BufferedInputStream bis =new BufferedInputStream(istr);
			DataInputStream dis = new DataInputStream(bis);
			BrsParser parser = new BrsParser();

			AnnotatedSeqGroup genome = new AnnotatedSeqGroup("testGenome");
			List<SeqSymmetry> results = parser.parse(dis, filename, genome);

			assertEquals(944, results.size());
	}
}