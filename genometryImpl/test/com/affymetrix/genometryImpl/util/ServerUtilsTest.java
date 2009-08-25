package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.comparator.UcscPslComparator;
import com.affymetrix.genometryImpl.parsers.BpsParser;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jnicol
 */
public class ServerUtilsTest {
	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private static String baseDir = "test/data/server/A_thaliana";
	private static String versionString = "A_thaliana_TAIR8";
	AnnotatedSeqGroup genome = null;

	@Before
	public void setUp() {
		InputStream chromstream = null;
		InputStream istr = null;
		try {
			// Load chromosomes
			File chrom_info_file = new File(baseDir + "/" + versionString + "/mod_chromInfo.txt");
			chromstream = new FileInputStream(chrom_info_file);
			ChromInfoParser.parse(chromstream, gmodel, versionString);
			
			// Load genome
			genome = gmodel.getSeqGroup(versionString);
			String stream_name = baseDir + "/" + versionString + "/mRNA1.mm.psl";
			File current_file = new File(stream_name);
			istr = new BufferedInputStream(new FileInputStream(current_file));

			PSLParser parser = new PSLParser();
			parser.setTrackNamePrefix("blah");
			parser.setCreateContainerAnnot(true);
			parser.parse(istr, "mRNA1.sm", null, genome, null, false, true, false);  // annotate target

			// optimize genome by replacing second-level syms with IntervalSearchSyms
			Optimize.Genome(genome);

		} catch (Exception ex) {
			Logger.getLogger(ServerUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} finally {
			GeneralUtils.safeClose(istr);
			GeneralUtils.safeClose(chromstream);
		}
	}

	@Test
	public void testGenome() {
		assertNotNull(genome);
		assertEquals("A_thaliana_TAIR8", genome.getID());
		assertEquals(7, genome.getSeqCount());
		BioSeq seq = genome.getSeq("chr1");
		assertNotNull(seq);
	}
	
	@Test
	public void testOverlapAndInsideSpan() {
		String seqid="chr1";

		String overlap = "90000:11200177";
		SeqSpan overlap_span = ServerUtils.getLocationSpan(seqid, overlap, genome);

		assertNotNull(overlap_span);
		assertEquals(90000,overlap_span.getMin());
		assertEquals(11200177,overlap_span.getMax());

		String query_type="mRNA1.sm";
		List<SeqSymmetry> result = null;
		result = ServerUtils.getOverlappedSymmetries(overlap_span, query_type);
		assertNotNull(result);
		
		List<UcscPslSym> tempResult = new ArrayList<UcscPslSym>(result.size());
		for(SeqSymmetry res : result) {
			tempResult.add((UcscPslSym)res);
		}

		Comparator<UcscPslSym> UCSCCompare = new UcscPslComparator();
		Collections.sort(tempResult,UCSCCompare);
		
		assertEquals(384,tempResult.size());
		assertEquals(136731, tempResult.get(0).getTargetMin());
		assertEquals(137967, tempResult.get(0).getTargetMax());

		String inside = "92000:4600000";
		SeqSpan inside_span = ServerUtils.getLocationSpan(seqid, inside, genome);
		assertNotNull(inside_span);
		assertEquals(92000,inside_span.getMin());
		assertEquals(4600000,inside_span.getMax());
		assertEquals(seqid, inside_span.getBioSeq().getID());

		result = ServerUtils.specifiedInsideSpan(inside_span, result);
		assertEquals(138, result.size());
	}


	@Test
	public void testIndexing() {
		try {
			String filename = "test/data/bps/mRNA1.mm.bps";
			String testFileName = "test/data/bps/mRNA1_test.mm.bps";
			String query_type="mRNA1.sm";
			String seqid = "chr1";
			assertTrue(new File(filename).exists());

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

			List<UcscPslSym> syms = BpsParser.parse(filename, query_type, group);

			BioSeq seq = group.getSeq(seqid);

			IndexWriter iWriter = new BpsParser();
			List<SeqSymmetry> sortedSyms = IndexingUtils.getSortedAnnotationsForChrom(
					syms, seq, iWriter.getComparator(seq));

			File testFile = new File(testFileName);
			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), testFile, query_type, iWriter);

			IndexingUtils.writeIndexedAnnotations(sortedSyms, seq, iSyms, testFileName);

			String overlap = "90000:11200177";
			SeqSpan overlap_span = ServerUtils.getLocationSpan(seqid, overlap, group);
			assertNotNull(overlap_span);
			assertEquals(90000, overlap_span.getMin());
			assertEquals(11200177, overlap_span.getMax());
			assertEquals(overlap_span.getBioSeq(), seq);

			
			List<UcscPslSym> result = ServerUtils.getIndexedOverlappedSymmetries(
					overlap_span, iSyms, "testOUT", group);
			
			assertEquals(384, result.size());
			assertEquals(136731, result.get(0).getTargetMin());
			assertEquals(137967, result.get(0).getTargetMax());

			String inside = "92000:4600000";
			SeqSpan inside_span = ServerUtils.getLocationSpan(seqid, inside, group);
			assertNotNull(inside_span);
			assertEquals(92000, inside_span.getMin());
			assertEquals(4600000, inside_span.getMax());
			assertEquals(inside_span.getBioSeq(), seq);

			result = ServerUtils.specifiedInsideSpan(inside_span, result);
			assertEquals(138, result.size());
			
			if (testFile.exists()) {
				testFile.delete();
			}

		} catch (Exception ex) {
			Logger.getLogger(ServerUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}

}
