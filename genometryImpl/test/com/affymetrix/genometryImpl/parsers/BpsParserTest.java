package com.affymetrix.genometryImpl.parsers;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.UcscPslSym;
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


	private void testOutputIndexedSymmetries(int [] min, int [] max, long [] pos){
		int minPos = searchAndBacktrack(min, 2455539);
		System.out.println("val: " + min[minPos] + " " + max[minPos] + " " + pos[minPos]);

		minPos = searchAndBacktrack(min, 2455600);
		System.out.println("position: " + minPos);
	}
	
	/**
	 * Binary search in array, and backtrack if necessary
	 * (since binarySearch is not guaranteed to return lowest index of equal elements)
	 * @param min
	 * @param element
	 * @return new
	 */
	private int searchAndBacktrack(int[] min, int element) {
		int minPos = Arrays.binarySearch(min, element);
		while (minPos > 0) {
			if (min[minPos - 1] == min[minPos]) {
				minPos--;
			} else {
				break;
			}
		}
		return minPos;
	}

	private final class UcscPslSymStartComparator implements Comparator<UcscPslSym> {
		public int compare(UcscPslSym sym1, UcscPslSym sym2) {
			return ((Integer)sym1.getTargetMin()).compareTo(sym2.getTargetMin());
		}
	}

	private final class UcscPslSymEndComparator implements Comparator<UcscPslSym> {
		public int compare(UcscPslSym sym1, UcscPslSym sym2) {
			return ((Integer)sym1.getTargetMax()).compareTo(sym2.getTargetMax());
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
