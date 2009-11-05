package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.SeqSpan;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import com.affymetrix.genometryImpl.util.Optimize;
import com.affymetrix.genometryImpl.util.ServerUtils;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;


public class LinkPSLParserTest {
	/**
	 * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.PSLParser.
	 */
	@Test
	public void testWriteAnnotations() {

			DataInputStream istr = null;
		try {
			String filename = "test/data/psl/RT_U34.link.psl.gz";
			String type = "testType";
			String consensusType = "RT_U34 " + ProbeSetDisplayPlugin.CONSENSUS_TYPE;
			assertTrue(new File(filename).exists());

			istr = new DataInputStream(new FileInputStream(filename));
			GZIPInputStream gzstr = new GZIPInputStream(istr);

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

			BioSeq seq = group.addSeq("chr1",1);

			PSLParser parser = new PSLParser();
			parser.setIsLinkPsl(true);
			parser.enableSharedQueryTarget(true);
			parser.setCreateContainerAnnot(true);

			List result = parser.parse(gzstr, type, null, group, null, false, true, false);
			assertNotNull(result);
			assertEquals(2103,result.size());	// all types of symmetries

			List<SeqSymmetry> consensusSyms = group.findSyms(Pattern.compile(".*"));
			assertEquals(1131, consensusSyms.size());	// only the consensus symmetries

			ByteArrayOutputStream outstream = null;
			ProbeSetDisplayPlugin parser2 = null;

			outstream = new ByteArrayOutputStream();
			parser2 = new ProbeSetDisplayPlugin();
			parser2.writeAnnotations(consensusSyms, seq, consensusType, outstream);

			// 1131 consensus syms.
			// 972 probeset symmetries, of which 879 are actually used by the consensus symmetries.
			// 2 "track" lines.
			// 1131 + 879 + 2
			assertEquals(1131 + 879 + 2, outstream.toString().split("\n").length);

		} catch (Exception ex) {
			Logger.getLogger(LinkPSLParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} finally {
			GeneralUtils.safeClose(istr);
		}
	}

	@Test
	public void testSplittingByChromosome() {
		DataInputStream istr = null;
		try {
			String filename = "test/data/psl/RT_U34.link.psl.gz";
			String type = "testType";
			String consensusType = "RT_U34 " + ProbeSetDisplayPlugin.CONSENSUS_TYPE;
			assertTrue(new File(filename).exists());

			istr = new DataInputStream(new FileInputStream(filename));
			GZIPInputStream gzstr = new GZIPInputStream(istr);

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

			BioSeq seq = group.addSeq("chr1", 1);

			PSLParser parser = new PSLParser();
			parser.setIsLinkPsl(true);
			parser.enableSharedQueryTarget(true);
			parser.setCreateContainerAnnot(true);

			List result = parser.parse(gzstr, type, null, group, null, false, true, false);
			assertNotNull(result);
			assertEquals(2103, result.size());	// all types of symmetries
			@SuppressWarnings("unchecked")
			Comparator<UcscPslSym> USCCCompare = parser.getComparator(seq);
			List<SeqSymmetry> sortedSyms = IndexingUtils.getSortedAnnotationsForChrom(
					result, seq, USCCCompare);	// the syms on chr1, sorted.
			assertNotNull(sortedSyms);
			assertEquals(168, sortedSyms.size());
			assertEquals(12722644, ((UcscPslSym) sortedSyms.get(0)).getTargetMin());
			assertEquals(267887419, ((UcscPslSym) sortedSyms.get(sortedSyms.size() - 1)).getTargetMin());

			// Now that we have the sorted consensus for chr1, verify its probesets.
			ByteArrayOutputStream outstream = null;
			ProbeSetDisplayPlugin parser2 = null;

			outstream = new ByteArrayOutputStream();
			parser2 = new ProbeSetDisplayPlugin();
			parser2.writeAnnotations(sortedSyms, seq, consensusType, outstream);

			// 168 consensus syms on chr1.
			// 143 probesets correspond to those consensus syms.
			// 2 "track" lines.
			// 168 + 143 + 2
			assertEquals(168 + 143 + 2, outstream.toString().split("\n").length);

		} catch (Exception ex) {
			Logger.getLogger(LinkPSLParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}


	@Test
	public void TestIndexing() {
		DataInputStream istr = null;
		try {
			String filename = "test/data/psl/RT_U34.link.psl.gz";
			String type = "testType";
			String consensusType = "RT_U34 " + ProbeSetDisplayPlugin.CONSENSUS_TYPE;
			assertTrue(new File(filename).exists());

			istr = new DataInputStream(new FileInputStream(filename));
			GZIPInputStream gzstr = new GZIPInputStream(istr);

			String seqid = "chr1";
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			BioSeq seq = group.addSeq("chr1", 267910886);
			assertTrue(seq != null);
			assertEquals(267910886, seq.getLength());

			PSLParser parser = new PSLParser();
			parser.setIsLinkPsl(true);
			parser.enableSharedQueryTarget(true);
			parser.setCreateContainerAnnot(true);

			List result = parser.parse(gzstr, type, null, group, null, false, true, false);

			// optimize genome by replacing second-level syms with IntervalSearchSyms
			Optimize.Genome(group);

			assertNotNull(result);
			assertEquals(2103, result.size());	// all types of symmetries
			@SuppressWarnings("unchecked")

			Comparator<UcscPslSym> USCCCompare = parser.getComparator(seq);

			List<SeqSymmetry> sortedSyms = IndexingUtils.getSortedAnnotationsForChrom(
					result, seq, USCCCompare);	// the syms on chr1, sorted.
			assertNotNull(sortedSyms);
			assertEquals(168, sortedSyms.size());
			assertEquals(12722644, ((UcscPslSym) sortedSyms.get(0)).getTargetMin());
			assertEquals(267887419, ((UcscPslSym) sortedSyms.get(sortedSyms.size() - 1)).getTargetMin());

			String testFileName = "test/data/psl/RT_U34TEST.link.psl";
			File testFile = new File(testFileName);
			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), testFile, "RT_U34", (IndexWriter) parser);
			IndexingUtils.writeIndexedAnnotations(sortedSyms, seq, group, iSyms, testFileName);

			String overlap = "3000000:160000000";
			SeqSpan overlap_span = ServerUtils.getLocationSpan(seqid, overlap, group);
			List<SeqSymmetry>newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",group);
			assertEquals(96, newResults.size());	// first 96 lines of file are < 160000000

			overlap = "160000000:160254000";
			overlap_span = ServerUtils.getLocationSpan(seqid, overlap, group);
			newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",group);
			assertEquals(0, newResults.size()); // no spans here

			overlap = "160254000:160254500";
			overlap_span = ServerUtils.getLocationSpan(seqid, overlap, group);
			newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",group);
			assertEquals(1, newResults.size());
			assertEquals("RT-U34:S78284_S_AT", newResults.get(0).getID());	// only feature in this span.

			overlap = "160254500:160254600";
			overlap_span = ServerUtils.getLocationSpan(seqid, overlap, group);
			newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",group);
			assertEquals(1, newResults.size());
			assertEquals("RT-U34:S78284_S_AT", newResults.get(0).getID());	// only feature in this span.

			overlap = "160254600:160254700";
			overlap_span = ServerUtils.getLocationSpan(seqid, overlap, group);
			newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",group);
			assertEquals(0, newResults.size()); // no spans here

			overlap = "122000000:123000000";
			overlap_span = ServerUtils.getLocationSpan(seqid, overlap, group);
			newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",group);
			assertEquals(1, newResults.size());
			assertEquals("RT-U34:L29232_AT", newResults.get(0).getID()); // only feature in this span
			
			overlap = "12722772:12723220";
			overlap_span = ServerUtils.getLocationSpan(seqid, overlap, group);
			newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,"testOUT",group);
			assertEquals(3, newResults.size());	// first three in the file

			if (testFile.exists()) {
				testFile.delete();
			}
			String testFileName2 = "test/data/psl/RT_U34TEST.link2.psl";
			File testFile2 = new File(testFileName2);
			if (testFile2.exists()) {
				testFile2.delete();
			}

		} catch (Exception ex) {
			Logger.getLogger(BgnParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}

	}
}

