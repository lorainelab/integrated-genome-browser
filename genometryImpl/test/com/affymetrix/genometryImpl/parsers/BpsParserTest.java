package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.SeqSpan;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils;
import com.affymetrix.genometryImpl.util.ServerUtils;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author jnicol
 */
public class BpsParserTest {

	/**
	 * Verify that converting to a Bps file always works the same.
	 * (This doesn't mean it's correct, just that its behavior hasn't changed.)
	 */
	@Test
	public void testConvertToBps() {
		InputStream istr = null;
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		try {
			String filename = "test/data/psl/test1.psl";
			assertTrue(new File(filename).exists());
			istr = new FileInputStream(filename);
			assertNotNull(istr);
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			boolean annot_seq = true;
			String stream_name = "test_file";

			PSLParser parser = new PSLParser();
			List<SeqSymmetry> syms = parser.parse(istr, stream_name, group, group, annot_seq, true);

			BpsParser instance2 = new BpsParser();
			boolean writeResult = instance2.writeAnnotations(syms, null, "", outstream);
			assertEquals(true, writeResult);


		} catch (Exception ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				istr.close();
			} catch (IOException ex) {
				Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		try {
			String filename = "test/data/bps/test1.bps";
			assertTrue(new File(filename).exists());
			istr = new FileInputStream(filename);
			assertNotNull(istr);

			BufferedInputStream bis = new BufferedInputStream(istr);
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			int result = bis.read();
			while (result != -1) {
				byte b = (byte) result;
				buf.write(b);
				result = bis.read();
			}

			assertEquals(outstream.toString(), buf.toString());
			
		} catch (Exception ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				istr.close();
			} catch (IOException ex) {
				Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

	}
	

	/**
	 * Test indexing code.
	 */
	@Test
	public void testIndexing() {
		String string =
		"70	1	0	0	0	0	2	165	+	EL049618	71	0	71	chr1	30432563	455031	455267	3	9,36,26,	0,9,45,	455031,455111,455241,\n" +
		"71	0	0	0	0	0	2	176	+	EL049618	71	0	71	chr1	30432563	457618	457865	3	9,36,26,	0,9,45,	457618,457715,457839,\n" +
		"70	1	0	0	0	0	2	165	+	EL049618	71	0	71	chr2	30432563	455031	455267	3	9,36,26,	0,9,45,	455031,455111,455241,\n" +
		"71	0	0	0	0	0	2	176	+	EL049500	71	0	71	chr1	30432563	457617	457864	3	9,36,26,	0,9,45,	457617,457714,457838,\n"
				;


		InputStream istr = new ByteArrayInputStream(string.getBytes());
		DataInputStream dis = new DataInputStream(istr);

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;

		List<SeqSymmetry> syms = null;
		try {
			PSLParser parser = new PSLParser();
			syms = parser.parse(istr, "stream_test", group, group, annot_seq, true);
		} catch (IOException ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		assertEquals(4, syms.size());	// precisely 4 symmetries.

		BioSeq seq = group.getSeq("chr1");

		List<UcscPslSym> pslSyms = new ArrayList<UcscPslSym>(syms.size());
		for (SeqSymmetry sym : syms) {
			pslSyms.add((UcscPslSym)sym);
		}

		BpsParser instance = new BpsParser();
		Comparator<UcscPslSym> USCCCompare = new UcscPslSymStartComparator();
		List<UcscPslSym> sortedSyms = instance.getSortedAnnotationsForChrom(pslSyms, seq, USCCCompare);

		assertEquals(3,sortedSyms.size());	// precisely 3 symmetries on chr1.

		assertEquals(457617, sortedSyms.get(1).getTargetMin());	// the middle symmetry (after sorting) should have a start coord of 457617.
		
	}


	/**
	 * Test indexing code.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testIndexing2() {

		FileInputStream istr = null;
		try {
			String filename = "test/data/bps/test1.bps";
			String testFileName = "test/data/bps/testNEW.bps";
			assertTrue(new File(filename).exists());
			
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			
			List<UcscPslSym> syms = null;
			syms = BpsParser.parse(filename, "stream_test", group);

			BioSeq seq = group.getSeq("chr1");
			
			BpsParser instance = new BpsParser();
			Comparator<UcscPslSym> USCCCompare = new UcscPslSymStartComparator();
			List<UcscPslSym> sortedSyms = instance.getSortedAnnotationsForChrom(syms, seq, USCCCompare);

			assertEquals(15,sortedSyms.size());

			int[] min = new int[sortedSyms.size()];
			int[] max = new int[sortedSyms.size()];
			long[] filePos = new long[sortedSyms.size()];
			FileOutputStream fos = null;
			fos = new FileOutputStream(testFileName);
			instance.writeIndexedAnnotations(sortedSyms, fos, min, max, filePos);

			assertEquals(min.length, max.length);
			assertEquals(min.length, filePos.length);
			assertEquals(sortedSyms.size(), min.length);

			System.out.println("First row: " + min[0] + " " + max[0] + " " + filePos[0]);

			System.out.println("Last row:" + min[min.length - 1] + " " + max[max.length - 1] + " " + filePos[filePos.length -1]);

			testOutputIndexedSymmetries(min,max,filePos);

			File testFile = new File(testFileName);
			if (testFile.exists()) {
				testFile.delete();
			}
		} catch (Exception ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}

	@Test
	public void testIndexing3() {
		FileInputStream istr = null;
		try {
			String filename = "test/data/bps/mRNA1.mm.bps";
			String testFileName = "test/data/bps/mRNA1_test.mm.bps";
			assertTrue(new File(filename).exists());

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

			List<UcscPslSym> syms = null;
			syms = BpsParser.parse(filename, "stream_test", group);

			BioSeq seq = group.getSeq("chr1");

			BpsParser instance = new BpsParser();
			Comparator<UcscPslSym> USCCCompare = new UcscPslSymStartComparator();
			List<UcscPslSym> sortedSyms = instance.getSortedAnnotationsForChrom(syms, seq, USCCCompare);

			System.out.println("sortedSyms size:" + sortedSyms.size());

			int[] min = new int[sortedSyms.size()];
			int[] max = new int[sortedSyms.size()];
			long[] filePos = new long[sortedSyms.size()];
			FileOutputStream fos = null;
			fos = new FileOutputStream(testFileName);
			instance.writeIndexedAnnotations(sortedSyms, fos, min, max, filePos);
			GeneralUtils.safeClose(fos);

			String overlap = "27:11200177";
			List<SeqSymmetry> result = null;

			SeqSpan overlap_span = ServerUtils.getLocationSpan("chr1", overlap, group);
			assertNotNull(overlap_span);
			assertEquals(27, overlap_span.getMin());
			assertEquals(11200177, overlap_span.getMax());

			int[] overlapRange = new int[2];
			int[] outputRange = new int[2];
			overlapRange[0] = overlap_span.getMin();
			overlapRange[1] = overlap_span.getMax();

			IndexingUtils.findMaxOverlap(overlapRange, outputRange, min, max);

			System.out.println("First row: " + min[0] + " " + max[0] + " " + filePos[0]);

			System.out.println("Last row:" + min[min.length - 1] + " " + max[max.length - 1] + " " + filePos[filePos.length -1]);

			int minPos = outputRange[0];
			System.out.println("val: " + min[minPos] + " " + max[minPos] + " " + filePos[minPos]);

			int maxPos = outputRange[1];
			System.out.println("val: " + min[maxPos] + " " + max[maxPos] + " " + filePos[maxPos]);


			FileInputStream fis = new FileInputStream(testFileName);
			byte[] bytes = IndexingUtils.getIndexedAnnotations(fis,filePos[minPos], (int)(filePos[maxPos] - filePos[minPos]));
			GeneralUtils.safeClose(fis);
			
			File testFile = new File(testFileName);
			if (testFile.exists()) {
				testFile.delete();
			}

			System.out.println("bytes size: " + bytes.length);

			InputStream newIstr = new ByteArrayInputStream(bytes);
			DataInputStream dis = new DataInputStream(newIstr);

			List <UcscPslSym> results = BpsParser.parse(dis, "BPS", (AnnotatedSeqGroup) null, group, false, true);

			System.out.println("New: results size: " + results.size());
			for (int i=0;i<results.size();i++) {
				if (i<3 || i > (results.size() - 3)) {
					UcscPslSym sym = results.get(i);
					System.out.println("i, " + i + " sym: " + sym.getID() + " min:" + sym.getTargetMin() + " max:" + sym.getTargetMax());
				}
			}
			
			
			// Read from file.
			


		} catch (Exception ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}


	private void testOutputIndexedSymmetries(int [] min, int [] max, long [] pos){
		int [] overlapRange = new int[2];
		int [] outputRange = new int[2];
		overlapRange[0] = 2455539;
		overlapRange[1] = 2455600;
		IndexingUtils.findMaxOverlap(overlapRange, outputRange, min, max);
		
		int minPos = outputRange[0];
		System.out.println("val: " + min[minPos] + " " + max[minPos] + " " + pos[minPos]);

		minPos = outputRange[1];
		System.out.println("position: " + minPos);
	}
	
	

	private static final class UcscPslSymStartComparator implements Comparator<UcscPslSym> {
		public int compare(UcscPslSym sym1, UcscPslSym sym2) {
			int comp = ((Integer)sym1.getTargetMin()).compareTo(sym2.getTargetMin());
			if (comp != 0) {
				return comp;
			}
			return ((Integer)sym1.getTargetMax()).compareTo(sym2.getTargetMax());
		}
	}

	private static final class UcscPslSymEndComparator implements Comparator<UcscPslSym> {
		public int compare(UcscPslSym sym1, UcscPslSym sym2) {
			int comp = ((Integer)sym1.getTargetMax()).compareTo(sym2.getTargetMax());
			if (comp != 0) {
				return comp;
			}
			return ((Integer)sym1.getTargetMin()).compareTo(sym2.getTargetMin());
		}
	}

	/**
	 * Test of getMimeType method.
	 */
	@Test
	public void testGetMimeType() {
		BpsParser instance = new BpsParser();

		String result = instance.getMimeType();
		assertTrue("binary/bps".equals(result));
	}
}
