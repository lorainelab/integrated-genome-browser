package com.affymetrix.genometry.util;

import com.affymetrix.genometry.Das2AnnotatedSeqGroup;
import com.affymetrix.genometry.Das2BioSeq;
import com.affymetrix.genometry.parsers.ProbeSetDisplayPlugin;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.symloader.PSL;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.UcscPslSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jnicol
 */
public final class IndexingUtils {
	private static final boolean DEBUG = false;

	// filename of indexed annotations.
	static String indexedFileName(String dataRoot, File file, String annot_name, AnnotatedSeqGroup genome, BioSeq seq) {
		String retVal = indexedDirName(dataRoot, genome, seq) + "/";
		
		// Change the file path to use forward slash (for Windows OS)
		String fullPath = file.getPath().replace("\\", "/");
		
		String fullDirName = dataRoot + genomeDirName(genome);
		if (!fullDirName.endsWith("/")) {
			fullDirName += "/";
		}
		
		if (fullPath.indexOf(fullDirName) >= 0) {
			String shortenedPath = fullPath.replace(fullDirName, "");
			return retVal + shortenedPath;			
		} else {
			return retVal + annot_name + "_indexed";
		}
	}
	
	static String indexedDirName(String dataRoot, AnnotatedSeqGroup genome, BioSeq seq) {
		return indexedGenomeDirName(dataRoot, genome) + "/" + seq.getID();
	}
	
	static String genomeDirName(AnnotatedSeqGroup genome) {
		return genome.getOrganism() + "/" + genome.getID();
	}
	
	public static String indexedGenomeDirName(String dataRoot, AnnotatedSeqGroup genome) {
		String optimizedDirectory = dataRoot + ".indexed";
		return optimizedDirectory + "/" + genomeDirName(genome);
	}


	
	/**
	 *
	 * @param originalGenome
	 * @param tempGenome
	 * @param dataRoot
	 * @param file
	 * @param loadedSyms
	 * @param iWriter
	 * @param typeName
	 * @param returnTypeName
	 * @throws java.io.IOException
	 */
	@SuppressWarnings("unused")
	public static void determineIndexes(
			AnnotatedSeqGroup originalGenome, AnnotatedSeqGroup tempGenome,
			String dataRoot, File file, List<? extends SeqSymmetry> loadedSyms, IndexWriter iWriter, String typeName, String returnTypeName, String ext) throws IOException {

		for (BioSeq originalSeq : originalGenome.getSeqList()) {
			BioSeq tempSeq = tempGenome.getSeq(originalSeq.getID());
			if (tempSeq == null) {
				continue;	// ignore; this is a seq that was added during parsing.
			}

			if (DEBUG) Logger.getLogger(IndexingUtils.class.getName()).log(Level.INFO,
					"Determining indexes for {0}, {1}", new Object[]{tempGenome.getID(), tempSeq.getID()});

			// Sort symmetries for this specific chromosome.
			List<SeqSymmetry> sortedSyms =
					IndexingUtils.getSortedAnnotationsForChrom(loadedSyms, tempSeq, iWriter.getComparator(tempSeq));
			if (DEBUG && sortedSyms.isEmpty()) {
				Logger.getLogger(IndexingUtils.class.getName()).log(Level.WARNING,
						"No annotations found for file: {0} on chromosome:{1}", new Object[]{file.getName(), tempSeq.getID()});
				continue;
			}

			String indexedAnnotationsFileName = IndexingUtils.indexedFileName(dataRoot, file, typeName, tempGenome, tempSeq);
			String dirName = indexedAnnotationsFileName.substring(0,indexedAnnotationsFileName.lastIndexOf('/'));
			Das2ServerUtils.createDirIfNecessary(dirName);

			File indexedAnnotationsFile = new File(indexedAnnotationsFileName);
			indexedAnnotationsFile.deleteOnExit();

			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), indexedAnnotationsFile, typeName, ext, iWriter);

			// add indexed symmetries to the chromosome (used by types request)
			((Das2BioSeq)originalSeq).addIndexedSyms(returnTypeName, iSyms);

			// Write the annotations out to a file.
			IndexingUtils.writeIndexedAnnotations(sortedSyms, tempSeq, tempGenome, iSyms);
		}
	}

	
	/**
	 * Find symmetries that have IDs or titles matching regex.  Return no more than resultLimit symmetries.
	 * @param genome
	 * @param regex
	 * @return list of Seq symmetries
	 */
	public static Set<SeqSymmetry> findSymsByName(AnnotatedSeqGroup genome, Pattern regex) {
		final Matcher matcher = regex.matcher("");
		Set<SeqSymmetry> results = new HashSet<SeqSymmetry>(100000);

		int resultCount = 0;

		// label for breaking out of loop
		SEARCHSYMS:
		for (BioSeq seq : genome.getSeqList()) {
			for (String type : ((Das2BioSeq)seq).getIndexedTypeList()) {
				IndexedSyms iSyms = ((Das2BioSeq)seq).getIndexedSym(type);
				if (iSyms == null) {
					continue;
				}
				if (findSymByName(iSyms, matcher, seq, type, results, resultCount)) {
					break SEARCHSYMS;
				}
			}
		}

		return results;
	}

	private static boolean findSymByName(
			IndexedSyms iSyms, final Matcher matcher, BioSeq seq, String type, Set<SeqSymmetry> results, int resultCount) {
		int symSize = iSyms.min.length;
		for (int i = 0; i < symSize; i++) {
			// test against various IDs
			byte[][] ids = iSyms.id[i];
			boolean foundID = false;
			int idLength = (ids == null) ? 0 : ids.length;
			for (int j=0;j<idLength;j++) {
				String id = new String(ids[j]);
				matcher.reset(id);
				if (matcher.matches()) {
					foundID = true;
					break;
				}
			}
			if (!foundID) {
				continue;
			}

			// found a match
			SimpleSymWithProps sym = iSyms.convertToSymWithProps(i, seq, type);
			results.add(sym);
			resultCount++;
			/*if (resultCount == resultLimit) {
				return true;
			}*/
		}
		return false;
	}

	public static Set<SeqSymmetry> findNameInGenome(String name, AnnotatedSeqGroup genome) {
		//int resultLimit = 1000000;

		boolean glob_start = name.startsWith("*");
		boolean glob_end = name.endsWith("*");

		Set<SeqSymmetry> result = null;
		Pattern name_pattern = null;
		String name_regex = name;
		if (glob_start || glob_end) {
			//name_regex = name.toLowerCase();
			if (glob_start) {
				// do replacement of first "*" with ".*" ?
				name_regex = ".*" + name_regex.substring(1);
			}
			if (glob_end) {
				// do replacement of last "*" with ".*" ?
				name_regex = name_regex.substring(0, name_regex.length() - 1) + ".*";
			}

		} else {
			// ABC -- field exactly matches "ABC"
			name_regex = "^" + name.toLowerCase() + "$";
			//result = genome.findSyms(name);
		}
		Logger.getLogger(IndexingUtils.class.getName()).log(Level.INFO,
				"name arg: {0},  regex to use for pattern-matching: {1}", new Object[]{name, name_regex});

		name_pattern = Pattern.compile(name_regex, Pattern.CASE_INSENSITIVE);
		result = ((Das2AnnotatedSeqGroup)genome).findSyms(name_pattern);

		Logger.getLogger(IndexingUtils.class.getName()).log(Level.INFO,
				"non-indexed regex matches: {0}", result.size());

		Set<SeqSymmetry> result2 = findSymsByName(genome, name_pattern);
		Logger.getLogger(IndexingUtils.class.getName()).log(Level.INFO,
				"indexed regex matches: {0}", result2.size());

		result.addAll(result2);

		return result;
	}
	
	/**
	 * Create a file of annotations, and index its entries.
	 * @param syms -- a sorted list of annotations (on one chromosome)
	 * @param seq -- the chromosome
	 * @param group -- the group (used to determine IDs for each sym)
	 * @throws java.io.IOException
	 */
	public static void writeIndexedAnnotations(
			List<SeqSymmetry> syms,
			BioSeq seq,
			AnnotatedSeqGroup group,
			IndexedSyms iSyms) throws IOException {
		if (DEBUG) {
			System.out.println("in IndexingUtils.writeIndexedAnnotations()");
		}

		createIndexArray(iSyms, syms, seq, group);
		writeIndex(iSyms, syms, seq);
	}

	/**
	 * Determine file positions and create iSyms array.
	 * @throws IOException
	 */
	private static void createIndexArray(
			IndexedSyms iSyms,
			List<SeqSymmetry> syms,
			BioSeq seq,
			AnnotatedSeqGroup group) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int index = 0;
		long currentFilePos = 0;
		IndexWriter iWriter = iSyms.iWriter;
		iSyms.filePos[0] = 0;
		for (SeqSymmetry sym : syms) {
			// Determine symmetry's byte size
			iWriter.writeSymmetry(sym, seq, baos);
			baos.flush();
			byte[] buf = baos.toByteArray();
			baos.reset();

			// add to iSyms, and advance index.
			iSyms.setIDs(group, sym.getID(), index);
			iSyms.min[index] = iWriter.getMin(sym, seq);
			iSyms.max[index] = iWriter.getMax(sym, seq);
			iSyms.forward.set(index,sym.getSpan(seq).isForward());
			currentFilePos += buf.length;
			index++;
			iSyms.filePos[index] = currentFilePos;
			
		}
		GeneralUtils.safeClose(baos);
	}


	/**
	 * Write out indexes to files (for one chromosome).
	 * @param syms -- symmetries to write out
	 * @param seq -- chromosome
	 * @throws IOException
	 */
	private static void writeIndex(
			IndexedSyms iSyms,
			List<SeqSymmetry> syms,
			BioSeq seq) throws IOException {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			fos = new FileOutputStream(iSyms.file.getAbsoluteFile());
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			IndexWriter iSymWriter = iSyms.iWriter;
			for (SeqSymmetry sym : syms) {
				iSymWriter.writeSymmetry(sym, seq, dos);	// write out interval<->symmetries
			}
			if ((iSymWriter instanceof PSLParser || iSymWriter instanceof PSL) && iSyms.ext.equalsIgnoreCase("link.psl")) {
				writeAdditionalLinkPSLIndex(iSyms.file.getAbsolutePath(), syms, seq, iSyms.typeName);
			}
		} finally {
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(fos);
		}
	}


	/**
	 * if it's a link.psl file, there is special-casing.
	 * We need to write out the remainder of the annotations as a special file ("...link2.psl")
	 *
	 * @param indexesFileName
	 * @param syms
	 * @param seq
	 * @param typeName
	 * @throws FileNotFoundException
	 */
	private static void writeAdditionalLinkPSLIndex(
			String indexesFileName, List<SeqSymmetry> syms, BioSeq seq, String typeName) throws FileNotFoundException {
		if (DEBUG) {
			System.out.println("in IndexingUtils.writeAdditionalLinkPSLIndex()");
		}

		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		String secondIndexesFileName = indexesFileName + ".link2.psl";
		try {
			fos = new FileOutputStream(secondIndexesFileName);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			// Write everything but the consensus sequence.
			ProbeSetDisplayPlugin.collectAndWriteAnnotations(syms, false, seq, typeName, dos);
		} finally {
			GeneralUtils.safeClose(fos);
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
		}
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
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>(10000);
		int symSize = syms.size();
		for (int i = 0; i < symSize; i++) {
			SeqSymmetry sym = (SeqSymmetry) syms.get(i);
			if (sym instanceof UcscPslSym) {
				// add the lines specifically with Target seq == seq.
				if (((UcscPslSym)sym).getTargetSeq() == seq) {
					results.add(sym);
				}
				continue;
			}
			// sym is instance of SeqSymmetry.
			if (sym.getSpan(seq) != null) {
				// add the lines specifically with seq.
				results.add(sym);
			}
		}

		Collections.sort(results, comp);

		return results;
	}


	/**
	 * Get "length" bytes starting at filePosStart
	 * @param file
	 * @param filePosStart
	 * @param length
	 * @return byte array 
	 */
	public static byte[] readBytesFromFile(File file, long filePosStart, int length) {
		byte[] contentsOnly = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			if (file.length() < length) {
				System.out.println("WARNING: filesize " + file.length() + " was less than argument " + length);
				length = (int)file.length();
			}
			FileChannel fc = fis.getChannel();
			MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, filePosStart, length);
			contentsOnly = new byte[length];
			mbb.get(contentsOnly);
		} catch (IOException ex) {
			Logger.getLogger(IndexingUtils.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(fis);
		}
		return contentsOnly;
	}

	/**
	 * special case for link.psl files
	 * we need to append the track name, and the probesets
	 * @param indexesFileName
	 * @param annot_type
	 * @param bytes1 - consensus symmetries in a byte array
	 * @return Byte array of the input stream
	 * @throws IOException
	 */
	public static ByteArrayInputStream readAdditionalLinkPSLIndex(
			String indexesFileName, String annot_type, byte[] bytes1) throws IOException {
		String secondIndexesFileName = indexesFileName + ".link2.psl";

		File secondIndexesFile = new File(secondIndexesFileName);
		int bytes2Len = (int) secondIndexesFile.length();
		byte[] bytes0 = PSLParser.trackLine(annot_type, "Consensus Sequences").getBytes();
		// Determine overall length
		int bytes0Len = bytes0.length;
		int bytes1Len = bytes1.length;
		byte[] combinedByteArr = new byte[bytes0Len + bytes1Len + bytes2Len];

		// Copy in arrays.
		// copy 0th byte array (trackLine)
		System.arraycopy(bytes0, 0, combinedByteArr, 0, bytes0Len);
		bytes0 = null;	// now unused

		// copy 1st byte array (consensus syms)
		System.arraycopy(bytes1, 0, combinedByteArr, bytes0Len, bytes1Len);
		bytes1 = null;	// now unused

		// copy 2nd byte array (probeset syms)
		byte[] bytes2 = IndexingUtils.readBytesFromFile(secondIndexesFile, 0, bytes2Len);

		System.arraycopy(bytes2, 0, combinedByteArr, bytes0Len + bytes1Len, bytes2Len);
		bytes2 = null;	// now unused

		return new ByteArrayInputStream(combinedByteArr);
	}

	
	/**
	 * Find the maximum overlap given a range.
	 * @param overlapRange -- an array of length 2, with a start and end coordinate.
	 * @param outputRange -- an outputted array of length 2, with a start position (from min[] array) and an end position (from min[] array).
	 * @param min -- array of SORTED min points.
	 * @param max -- array of max points.
	 */
	public static void findMaxOverlap(int [] overlapRange, int [] outputRange, int [] min, int [] max) {
		// Find the first element with min at least equal to our start.
		int minStart = findMinimaGreaterOrEqual(min, overlapRange[0]);

		// Correct this estimate by backtracking to find any max values where start <= max.
		// (Otherwise, we will miss half-in intervals that have min < start, but start <= max.)
		int correctedMinStart = backtrackForHalfInIntervals(minStart, max, overlapRange[0]);

		outputRange[0] = correctedMinStart;

		// Find the last element with start(min) at most equal to our overlap end.
		// Since min is always <= max, this gives us a correct bound on our return values.
		int maxEnd = findMaximaLessOrEqual(min, overlapRange[1]);
		outputRange[1] = maxEnd;
	}


	/**
	 * Find minimum index of min[] array that is >= start range.
	 * @param min
	 * @param elt
	 * @return tempPos
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
	 * @return tempPos
	 */
	private static int findMaximaLessOrEqual(int[] min, int elt) {
		int tempPos = Arrays.binarySearch(min, elt);
		if (tempPos >= 0) {
			tempPos = forwardtrack(min, tempPos);
		} else {
			// This means the end element was not found in the array.  Translate back to "insertion point", which is:
			//the index of the first element greater than the key, or min.length, if all elements in the list are less than the specified key.
			tempPos = (-(tempPos + 1));
			// But here, we want to go the last element < the key.
			if (tempPos > 0) {
				tempPos--;
			}
			// Don't go past array limit (this case is probably impossible)
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

	/**
	 * Backtrack to find any max values where start <= max <= end.
	 * @param minStart
	 * @param max
	 * @param overlapStart
	 * @return minVal
	 */
	private static int backtrackForHalfInIntervals(int minStart, int[] max, int overlapStart) {
		int minVal = minStart;
		for (int i=minStart-1;i>=0;i--) {
			if (max[i] >= overlapStart) {
				minVal = i;
			}
		}
		return minVal;
	}

}
