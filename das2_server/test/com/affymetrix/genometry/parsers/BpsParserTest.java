package com.affymetrix.genometry.parsers;

import com.affymetrix.genometry.util.ServerUtilsTest;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.BpsParser;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.UcscPslSym;
import com.affymetrix.genometryImpl.util.DasServerUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;


public class BpsParserTest {
	
	@Test
	public void testIndexing() {
		try {
			String filename = "test/data/bps/mRNA1.mm.bps";
			String testFileName = "test/data/bps/mRNA1_test.mm.bps";
			String query_type="mRNA1.sm";
			String seqid = "chr1";
			assertTrue(new File(filename).exists());

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

			List<UcscPslSym> syms = bpsParse(filename, query_type, group);

			BioSeq seq = group.getSeq(seqid);

			IndexWriter iWriter = new BpsParser();
			List<SeqSymmetry> sortedSyms = IndexingUtils.getSortedAnnotationsForChrom(
					syms, seq, iWriter.getComparator(seq));

			File testFile = new File(testFileName);
			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), testFile, query_type, "bps", iWriter);

			IndexingUtils.writeIndexedAnnotations(sortedSyms, seq, group, iSyms);
			
			testIndexing1(seqid, group, seq, iSyms);

			// Overflow conditions.
			testIndexing2("0:30432562", seqid, group, seq, iSyms);
			testIndexing2("0:30432563", seqid, group, seq, iSyms);
			testIndexing2("0:30432564", seqid, group, seq, iSyms);

			testIndexing3(seqid, group, seq, iSyms);
			
			testIndexing4(seqid, group, seq, iSyms);
			
			if (testFile.exists()) {
				testFile.delete();
			}

		} catch (Exception ex) {
			Logger.getLogger(ServerUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}


	private void testIndexing1(String seqid, AnnotatedSeqGroup group, BioSeq seq, IndexedSyms iSyms) {
		String overlap;
		SeqSpan overlap_span;
		List<SeqSymmetry> result;
		overlap = "0:11200177";
		overlap_span = DasServerUtils.getLocationSpan(seqid, overlap, group);
		assertNotNull(overlap_span);
		assertEquals(0, overlap_span.getMin());
		assertEquals(11200177, overlap_span.getMax());
		assertEquals(overlap_span.getBioSeq(), seq);
		result = DasServerUtils.getIndexedOverlappedSymmetries(overlap_span, iSyms, "testOUT", group);
		assertEquals(385, result.size());
		assertEquals(88976, ((UcscPslSym)result.get(0)).getTargetMin());
		assertEquals(89560, ((UcscPslSym)result.get(0)).getTargetMax());
	}

	private void testIndexing2(String overlap, String seqid, AnnotatedSeqGroup group, BioSeq seq, IndexedSyms iSyms) {
		SeqSpan overlap_span = DasServerUtils.getLocationSpan(seqid, overlap, group);
		assertEquals(overlap_span.getBioSeq(), seq);
		List<SeqSymmetry> result = DasServerUtils.getIndexedOverlappedSymmetries(overlap_span, iSyms, "testOUT", group);
		assertEquals(861, result.size());
		assertEquals(88976, ((UcscPslSym)result.get(0)).getTargetMin());
		assertEquals(89560, ((UcscPslSym)result.get(0)).getTargetMax());
		assertEquals(30427075, ((UcscPslSym)result.get(result.size()-1)).getTargetMin());
		assertEquals(30428332, ((UcscPslSym)result.get(result.size()-1)).getTargetMax());
	}

	private void testIndexing3(
			String seqid, AnnotatedSeqGroup group, BioSeq seq, IndexedSyms iSyms) {
		String overlap = "0:11200177";
		SeqSpan overlap_span = DasServerUtils.getLocationSpan(seqid, overlap, group);
		assertNotNull(overlap_span);
		assertEquals(0, overlap_span.getMin());
		assertEquals(11200177, overlap_span.getMax());
		assertEquals(overlap_span.getBioSeq(), seq);
		List <SeqSymmetry> result = DasServerUtils.getIndexedOverlappedSymmetries(overlap_span, iSyms, "testOUT", group);
		assertEquals(385, result.size());
		assertEquals(88976, ((UcscPslSym)result.get(0)).getTargetMin());
		assertEquals(89560, ((UcscPslSym)result.get(0)).getTargetMax());
	}


	private void testIndexing4(
			String seqid, AnnotatedSeqGroup group, BioSeq seq, IndexedSyms iSyms) {
		String overlap = "90000:11200177";
		SeqSpan overlap_span = DasServerUtils.getLocationSpan(seqid, overlap, group);
		assertNotNull(overlap_span);
		assertEquals(90000, overlap_span.getMin());
		assertEquals(11200177, overlap_span.getMax());
		assertEquals(overlap_span.getBioSeq(), seq);
		List <SeqSymmetry> result = DasServerUtils.getIndexedOverlappedSymmetries(overlap_span, iSyms, "testOUT", group);
		assertEquals(384, result.size());
		assertEquals(136731, ((UcscPslSym)result.get(0)).getTargetMin());
		assertEquals(137967, ((UcscPslSym)result.get(0)).getTargetMax());
		String inside = "92000:4600000";
		SeqSpan inside_span = DasServerUtils.getLocationSpan(seqid, inside, group);
		assertNotNull(inside_span);
		assertEquals(92000, inside_span.getMin());
		assertEquals(4600000, inside_span.getMax());
		assertEquals(inside_span.getBioSeq(), seq);
		result = DasServerUtils.specifiedInsideSpan(inside_span, result);
		assertEquals(138, result.size());
	}

	public static List<UcscPslSym> bpsParse(String file_name, String annot_type, AnnotatedSeqGroup seq_group)
			throws IOException {
		Logger.getLogger(BpsParser.class.getName()).log(
							Level.INFO, "loading file: {0}", file_name);
		List<UcscPslSym> results = null;
		FileInputStream fis = null;
		DataInputStream dis = null;
		BufferedInputStream bis = null;
		try {
			File fil = new File(file_name);
			long flength = fil.length();
			fis = new FileInputStream(fil);
			bis = new BufferedInputStream(fis);

			byte[] bytebuf = new byte[(int) flength];
			bis.read(bytebuf);

			ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
			dis = new DataInputStream(bytestream);
			results = BpsParser.parse(dis, annot_type, (AnnotatedSeqGroup) null, seq_group, false, true);
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(fis);
		}
		return results;
	}
}
