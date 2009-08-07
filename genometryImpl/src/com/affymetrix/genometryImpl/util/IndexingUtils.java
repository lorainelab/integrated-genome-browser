package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 */
public class IndexingUtils {
	private static final boolean DEBUG = false;

	public static class IndexedSyms {
		public File file;
		public int[] min;
		public int[] max;
		public long[] filePos;
		public String typeName;
		public IndexWriter iWriter;

		public IndexedSyms(List<SeqSymmetry> result, File file, String typeName, IndexWriter iWriter) {
			min = new int[result.size()];
			max = new int[result.size()];
			filePos = new long[result.size() + 1];
			this.file = file;
			this.typeName = typeName;
			this.iWriter = iWriter;
		}
	}


	/**
	 * Create a file of annotations, and index its entries.
	 * @param syms -- a sorted list of annotations (on one chromosome)
	 * @param seq -- the chromosome
	 * @param iSyms
	 * @param fos
	 * @return - success or failure
	 */
	public static boolean writeIndexedAnnotations(
			List<SeqSymmetry> syms,
			MutableAnnotatedBioSeq seq,
			IndexedSyms iSyms,
			FileOutputStream fos) {
		if (DEBUG){
			System.out.println("in IndexingUtils.writeIndexedAnnotations()");
		}
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(fos);
			FileChannel fChannel = fos.getChannel();
			int index = 0;
			iSyms.filePos[index] = 0;
			for (SeqSymmetry sym : syms) {
				iSyms.min[index] = iSyms.iWriter.getMin(sym);
				iSyms.max[index] = iSyms.iWriter.getMax(sym);
				iSyms.iWriter.writeSymmetry(sym, seq, dos);
				index++;
				iSyms.filePos[index] = fChannel.position();
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}


	/**
	 * Returns annotations for specific chromosome, sorted by comparator.
	 * Class cannot be generic, since symmetries could be UcscPslSyms or SeqSymmetries.
	 * @param syms - original list of annotations
	 * @param seq - specific chromosome
	 * @param comp - comparator
	 * @return - sorted list of annotations
	 */
	@SuppressWarnings("unchecked")
	public static List<SeqSymmetry> getSortedAnnotationsForChrom(List syms, BioSeq seq, Comparator comp) {
		Collections.sort(syms, comp);

		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for (int i=0;i<syms.size();i++) {
			SeqSymmetry sym = (SeqSymmetry)syms.get(i);
			if (sym instanceof UcscPslSym) {
				// add the lines specifically with Target seq == seq.
				if (((UcscPslSym)sym).getTargetSeq() == seq) {
					results.add(sym);
				}
				continue;
			}
			// sym is instance of SeqSymmetry.
			//TODO
		}
		return results;
	}

	/**
	 * Get "length" annotations from filePosStart
	 * @param fis
	 * @param filePosStart
	 * @param length
	 * @return
	 */
	public static byte[] getIndexedAnnotations(FileInputStream fis, long filePosStart, int length) {
		byte[] contentsOnly = null;
		try {
			FileChannel fc = fis.getChannel();
			MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, filePosStart, length);
			contentsOnly = new byte[length];
			mbb.get(contentsOnly);
		} catch (IOException ex) {
			Logger.getLogger(IndexingUtils.class.getName()).log(Level.SEVERE, null, ex);
		}
		return contentsOnly;
	}

	/**
	 * Find the maximum overlap given a range.
	 * @param insideRange -- an array of length 2, with a start and end coordinate.
	 * @param outputRange -- an outputted array of length 2, with a start position (from min[] array) and an end position (from max[] array).
	 * @param min -- array of SORTED min points.
	 * @param max -- array of max points.
	 */
	public static void findMaxOverlap(int [] overlapRange, int [] outputRange, int [] min, int [] max) {
		int tempPos = findMinimaGreaterOrEqual(min, overlapRange[0]);
		outputRange[0] = tempPos;

		tempPos = findMaximaLessOrEqual(min, overlapRange[1]);
		outputRange[1] = tempPos;
	}


	/**
	 * Find minimum index of min[] array that is >= start range.
	 * @param min
	 * @param elt
	 * @return
	 */
	private static int findMinimaGreaterOrEqual(int[] min, int elt) {
		int tempPos = Arrays.binarySearch(min, elt);
		if (tempPos >= 0) {
			tempPos = backTrack(min, tempPos);
		} else {
			// This means the start element was not found in the array.  Translate back to "insertion point", which is:
			//the index of the first element greater than the key, or min.length, if all elements in the list are less than the specified key.
			tempPos = (-(tempPos + 1));
			// Don't go past array limit.
			tempPos = Math.min(min.length - 1, tempPos);
		}
		return tempPos;
	}

	/**
	 * Find maximum index of min[] array that is <= end range.
	 * @param min
	 * @param elt
	 * @return
	 */
	private static int findMaximaLessOrEqual(int[] min, int elt) {
		int tempPos = Arrays.binarySearch(min, elt);
		if (tempPos >= 0) {
			tempPos = forwardtrack(min, tempPos);
		} else {
			// This means the end element was not found in the array.  Translate back to "insertion point", which is:
			//the index of the first element greater than the key, or min.length, if all elements in the list are less than the specified key.
			tempPos = (-(tempPos + 1));
			// But here, we want to backtrack to the element less than the key.
			if (tempPos > 0) {
				tempPos--;
				tempPos = backTrack(min, tempPos);
			}
			// Don't go past array limit
			tempPos = Math.min(min.length - 1, tempPos);
		}
		return tempPos;
	}

	/**
	 * backtrack if necessary
	 * (since binarySearch is not guaranteed to return lowest index of equal elements)
	 * @param arr
	 * @param pos
	 * @return lowest index of equal elements
	 */
	public static int backTrack(int[] arr, int pos) {
		while (pos > 0) {
			if (arr[pos - 1] == arr[pos]) {
				pos--;
			} else {
				break;
			}
		}
		return pos;
	}

	/**
	 * forward-track if necessary
	 * (since binarySearch is not guaranteed to return highest index of equal elements)
	 * @param arr
	 * @param pos
	 * @return highest index of equal elements
	 */
	public static int forwardtrack(int[] arr, int pos) {
		while (pos < arr.length - 1) {
			if (arr[pos + 1] == arr[pos]) {
				pos++;
			} else {
				break;
			}
		}
		return pos;
	}

}
