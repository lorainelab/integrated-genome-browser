package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.comparator.UcscPslComparator;
import com.affymetrix.genometryImpl.parsers.BpsParser;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
		result = ServerUtils.getIntersectedSymmetries(overlap_span, query_type);
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

		result = ServerUtils.SpecifiedInsideSpan(inside_span, result, query_type);
		assertEquals(138, result.size());
	}


	@Test
	public void testIndexing3() {
		//FileInputStream istr = null;
		try {
			String filename = "test/data/bps/mRNA1.mm.bps";
			String testFileName = "test/data/bps/mRNA1_test.mm.bps";
			assertTrue(new File(filename).exists());

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

			List<UcscPslSym> syms = null;
			syms = BpsParser.parse(filename, "stream_test", group);

			BioSeq seq = group.getSeq("chr1");

			BpsParser instance = new BpsParser();
			Comparator<UcscPslSym> USCCCompare = new UcscPslComparator();
			List<UcscPslSym> sortedSyms = instance.getSortedAnnotationsForChrom(syms, seq, USCCCompare);

			int[] min = new int[sortedSyms.size()];
			int[] max = new int[sortedSyms.size()];
			long[] filePos = new long[sortedSyms.size() + 1];
			FileOutputStream fos = null;
			fos = new FileOutputStream(testFileName);
			instance.writeIndexedAnnotations(sortedSyms, fos, min, max, filePos);
			GeneralUtils.safeClose(fos);

			String overlap = "90000:11200177";
			SeqSpan overlap_span = ServerUtils.getLocationSpan("chr1", overlap, group);
			assertNotNull(overlap_span);
			assertEquals(90000, overlap_span.getMin());
			assertEquals(11200177, overlap_span.getMax());

			int[] overlapRange = new int[2];
			int[] outputRange = new int[2];
			overlapRange[0] = overlap_span.getMin();
			overlapRange[1] = overlap_span.getMax();

			IndexingUtils.findMaxOverlap(overlapRange, outputRange, min, max);

			int minPos = outputRange[0];

			int maxPos = outputRange[1]+1;
			

			FileInputStream fis = new FileInputStream(testFileName);

			// We add 1 to the filePos index.
			// Since filePos is recorded at the *beginning* of each line, this allows us to read the last element.
			byte[] bytes = IndexingUtils.getIndexedAnnotations(fis,filePos[minPos], (int)(filePos[maxPos] - filePos[minPos]));
			assertEquals((int)(filePos[maxPos] - filePos[minPos]), bytes.length);
			GeneralUtils.safeClose(fis);

			File testFile = new File(testFileName);
			if (testFile.exists()) {
				testFile.delete();
			}

			InputStream newIstr = new ByteArrayInputStream(bytes);
			DataInputStream dis = new DataInputStream(newIstr);

			List <UcscPslSym> result = BpsParser.parse(dis, "BPS", (AnnotatedSeqGroup) null, group, false, true);

			assertEquals(384, result.size());
			assertEquals(136731, result.get(0).getTargetMin());
			assertEquals(137967, result.get(0).getTargetMax());
		} catch (Exception ex) {
			Logger.getLogger(ServerUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}
}
