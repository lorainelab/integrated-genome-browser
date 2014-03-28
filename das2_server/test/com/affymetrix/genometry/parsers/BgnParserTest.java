package com.affymetrix.genometry.parsers;

import com.affymetrix.genometry.util.Das2ServerUtils;
import com.affymetrix.genometry.util.IndexedSyms;
import com.affymetrix.genometry.util.IndexingUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.BgnParser;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author jnicol
 */
public class BgnParserTest {
	String filename = "mgcGenes.bgn";
	String versionString = "genomeVersion";
	AnnotatedSeqGroup genome = null;
	private List<SeqSymmetry> results = null;
	private BgnParser parser = new BgnParser();

	@Before
	public void setUp() {
		InputStream istr = null;
		try {
			istr = BgnParserTest.class.getClassLoader().getResourceAsStream(filename);
			genome = new AnnotatedSeqGroup("testGenome");
			results = parser.parse(istr, filename, genome, true);
		} catch (Exception ex) {
			Logger.getLogger(BgnParserTest.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				istr.close();
			} catch (IOException ex) {
				Logger.getLogger(BgnParserTest.class.getName()).log(Level.SEVERE, null, ex);
				fail();
			}
		}
	}

	@Test
	public void TestParseBgn() throws Exception {
		assertTrue(results != null);
		assertEquals(6010, results.size());
	}

	@Ignore //ignoring since testOUT.bgn does not exist and das2 support is deprecated anyway
	@Test
	public void TestIndexing() {
		try {
			String testFileName = "testOUT.bgn";
			String seqid = "chr1";
			BioSeq seq = genome.getSeq(seqid);
			assertTrue(seq != null);
			assertEquals(267693137, seq.getLength());
			List<SeqSymmetry> sortedSyms = IndexingUtils.getSortedAnnotationsForChrom(
					results, seq, parser.getComparator(seq));
			assertEquals(726, sortedSyms.size());
			SeqSymmetry firstSym = sortedSyms.get(0);
			assertEquals(2150626, firstSym.getSpan(seq).getMin());
			assertEquals(2155593, firstSym.getSpan(seq).getMax());
			SeqSymmetry lastSym = sortedSyms.get(sortedSyms.size() - 1);
			assertEquals(267495490, lastSym.getSpan(seq).getMin());
			assertEquals(267693137, lastSym.getSpan(seq).getMax());

			File testFile = new File(BgnParserTest.class.getClassLoader().getResource(testFileName).getFile());
			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), testFile, "test", "bgn", (IndexWriter) parser);
			IndexingUtils.writeIndexedAnnotations(sortedSyms, seq, genome, iSyms);

			String overlap = "3000000:160000000";
			SeqSpan overlap_span = Das2ServerUtils.getLocationSpan(seqid, overlap, genome);

			List newResults = Das2ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",genome);
			assertEquals(337, newResults.size());

			overlap = "115000000:123000000";
			overlap_span = Das2ServerUtils.getLocationSpan(seqid, overlap, genome);

			newResults = Das2ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",genome);
			assertEquals(6, newResults.size());

			if (testFile.exists()) {
				testFile.delete();
			}

		} catch (Exception ex) {
			Logger.getLogger(BgnParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}

	}
}
