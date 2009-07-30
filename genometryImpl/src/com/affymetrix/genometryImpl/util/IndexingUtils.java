package com.affymetrix.genometryImpl.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 */
public class IndexingUtils {
	/**
	 * Get "length" annotations from filePosStart
	 * @param fis
	 * @param filePosStart
	 * @param length
	 * @return
	 */
	public static byte[] getIndexedAnnotations(FileInputStream fis, long filePosStart, int length) {
		System.out.println("Opening buffer with " + filePosStart + " " + length);
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
		// Find minimum index of min[] array that is >= start range.
		int tempPos = findMinimaGreaterOrEqual(min, overlapRange[0]);
		outputRange[0] = tempPos;

		// Find maximum index of min[] array that is <= end range.
		tempPos = findMaximaLessOrEqual(min, overlapRange[1]);
		System.out.println("val: " + min[tempPos] + " " + max[tempPos]);
		outputRange[1] = tempPos;
	}



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
