package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.ProbeSetDisplayPlugin;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jnicol
 */
public class IndexingUtils {
	private static final boolean DEBUG = false;

	/**
	 * Used to index the symmetries for interval searches.
	 */
	public static class IndexedSyms {
		public File file;
		public int[] min;
		public int[] max;
		//public String[] id;
		public long[] filePos;
		public String typeName;
		public IndexWriter iWriter;

		public IndexedSyms(int resultSize, File file, String typeName, IndexWriter iWriter) {
			min = new int[resultSize];
			max = new int[resultSize];
			//id = new String[resultSize];
			filePos = new long[resultSize + 1];
			this.file = file;
			this.typeName = typeName;
			this.iWriter = iWriter;
		}
	}

	/**
	 * Used to index the symmetries for name (ID) searches.
	 */
	public static class IndexedIDs {
		public List<String> id;
		public List<Long> filePos;
		public DataOutputStream symDos;	// output stream for all of the symmetries
		public IndexWriter iWriter;		// BPS writer

		public IndexedIDs(String dataRoot, AnnotatedSeqGroup group) throws FileNotFoundException {
			this(dataRoot,group,
				new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexedGenomeSymFileName(dataRoot, group)))));
		}

		public IndexedIDs(String dataRoot, AnnotatedSeqGroup group, DataOutputStream symDos) {
			this.id = new ArrayList<String>(1000);
				this.filePos = new ArrayList<Long>(1000);
				this.filePos.add(new Long(0));
			this.symDos = symDos;
			this.iWriter = new BedParser();
		}

		public void writeIDFile(String dataRoot, AnnotatedSeqGroup originalGenome) {
			DataOutputStream IDdos = null;
			try {
				String IDFileName = indexedGenomeIDFileName(dataRoot, originalGenome);
				IDdos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(IDFileName)));
				int symSize = this.id.size();
				for (int i = 0; i < symSize; i++) {
					IDdos.writeChars(this.id.get(i));
					IDdos.writeLong(this.filePos.get(i).longValue());
				}
				IDdos.writeLong(this.filePos.get(symSize).longValue());
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				GeneralUtils.safeClose(symDos);
				GeneralUtils.safeClose(IDdos);
			}
		}
	}

	// filename of indexed annotations.
	static String indexedFileName(String dataRoot, String fileName, AnnotatedSeqGroup genome, BioSeq seq) {
		return indexedDirName(dataRoot, genome, seq) + "/" + fileName;
	}
	static String indexedDirName(String dataRoot, AnnotatedSeqGroup genome, BioSeq seq) {
		return indexedGenomeDirName(dataRoot, genome) + "/" + seq.getID();
	}
	static String indexedGenomeIDFileName(String dataRoot, AnnotatedSeqGroup genome) {
		return indexedGenomeDirName(dataRoot, genome) + "/" + "IDX_IDs" ;
	}
	static String indexedGenomeSymFileName(String dataRoot, AnnotatedSeqGroup genome) {
		return indexedGenomeDirName(dataRoot, genome) + "/" + "IDX_syms" ;
	}
	static String indexedGenomeDirName(String dataRoot, AnnotatedSeqGroup genome) {
		String optimizedDirectory = dataRoot + ".indexed";
		return optimizedDirectory + "/" + genome.getOrganism() + "/" + genome.getID();
	}


	/**
	 * Generate indexes.
	 * @param genome
	 * @param dataRoot
	 * @param file
	 * @param originalFileName
	 * @param loadedSyms
	 * @param iWriter
	 * @param typeName
	 */
	static void determineIndexes(
			AnnotatedSeqGroup originalGenome, AnnotatedSeqGroup tempGenome,
			String dataRoot, File file, List loadedSyms, IndexWriter iWriter, String typeName, String returnTypeName) {

		ServerUtils.createDirIfNecessary(IndexingUtils.indexedGenomeDirName(dataRoot, originalGenome));
		IndexedIDs iIDs = null;
		try {
			iIDs = new IndexedIDs(dataRoot, originalGenome);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.SEVERE, null, ex);
			System.exit(-1);
		}

		for (BioSeq originalSeq : originalGenome.getSeqList()) {
			BioSeq tempSeq = tempGenome.getSeq(originalSeq.getID());
			if (tempSeq == null) {
				continue;	// ignore; this is a seq that was added during parsing.
			}

			String dirName = IndexingUtils.indexedDirName(dataRoot, tempGenome, tempSeq);
			String indexedAnnotationsFileName = IndexingUtils.indexedFileName(dataRoot, file.getName(), tempGenome, tempSeq);
			File indexedAnnotationsFile = new File(indexedAnnotationsFileName);

			ServerUtils.createDirIfNecessary(dirName);

			// Sort symmetries for this specific chromosome.
			List<SeqSymmetry> sortedSyms =
					IndexingUtils.getSortedAnnotationsForChrom(loadedSyms, tempSeq, iWriter.getComparator(tempSeq));
			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), indexedAnnotationsFile, typeName, iWriter);

			// add symmetries to the chromosome (used by types request)
			// TODO: use this for names request
			originalSeq.addIndexedSyms(returnTypeName, iSyms);
			// Write the annotations out to a file.
			IndexingUtils.writeIndexedAnnotations(sortedSyms, tempSeq, iSyms, iIDs, indexedAnnotationsFileName);
		}

		iIDs.writeIDFile(dataRoot, originalGenome);
	}


	public static List<SeqSymmetry> findSymsByName(String fileName, Pattern regex, IndexWriter iWriter) {
		final Matcher matcher = regex.matcher("");
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		DataInputStream dis = null;

		DataInputStream dis2 = null;
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>(1000);

		try {
			fis = new FileInputStream(fileName);
			bis = new BufferedInputStream(fis);
			dis = new DataInputStream(bis);

			int symsSize = dis.readInt();	// determine number of rows.
			long oldFilePos = 0;
			for (int i=0;i<symsSize;i++) {
				String id = dis.readUTF();
				long newFilePos = dis.readLong();
				matcher.reset(id);
				if (matcher.matches()) {
					SeqSymmetry sym = getSingleSymmetry(fis, oldFilePos, newFilePos, iWriter, dis, dis2);
					if (sym != null) {
						results.add(sym);
					}
				}
				oldFilePos = newFilePos;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(fis);
		}
		return results;
	}

	private static SeqSymmetry getSingleSymmetry(FileInputStream fis, long oldFilePos, long newFilePos, IndexWriter iWriter, DataInputStream dis, DataInputStream dis2) {
		// Get the symmetry; it's from oldFilePos to newFilePos.
		byte[] bytes = IndexingUtils.readBytesFromFile(fis, oldFilePos, (int) (newFilePos - oldFilePos));
		ByteArrayInputStream newIstr = new ByteArrayInputStream(bytes);
		try {
			dis2 = new DataInputStream(newIstr);
			@SuppressWarnings(value = "unchecked")
			List<SeqSymmetry> syms = iWriter.parse(dis, "", null);
			if (syms != null && !syms.isEmpty()) {
				return syms.get(0);
			}
			return null;
		} finally {
			GeneralUtils.safeClose(dis2);
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
			IndexedIDs iIDs,
			String indexesFileName) {
		if (DEBUG) {
			System.out.println("in IndexingUtils.writeIndexedAnnotations()");
		}

		try {
			createIndexArray(iSyms, iIDs, syms, seq);
			writeIndex(iSyms, iIDs, indexesFileName, syms, seq);
			return true;
		} catch (Exception ex) {
			Logger.getLogger(IndexingUtils.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
	}

	/**
	 * Determine file positions and create iSyms array.
	 * @param iSyms
	 * @param syms
	 * @param seq
	 * @throws IOException
	 */
	private static void createIndexArray(
			IndexedSyms iSyms,
			IndexedIDs iIDs,
			List<SeqSymmetry> syms,
			MutableAnnotatedBioSeq seq) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int index = 0;
		long currentFilePos = 0;
		IndexWriter iWriter = iSyms.iWriter;
		iSyms.filePos[0] = 0;
		for (SeqSymmetry sym : syms) {
			iWriter.writeSymmetry(sym, seq, baos);
			baos.flush();
			byte[] buf = baos.toByteArray();
			baos.reset();

			// add to iIDS
			iIDs.id.add(sym.getID());
			long cumulativeFilePos = buf.length + iIDs.filePos.get(iIDs.filePos.size()-1);
			iIDs.filePos.add(new Long(cumulativeFilePos));

			// add to iSyms, and advance index.
			iSyms.min[index] = iWriter.getMin(sym, seq);
			iSyms.max[index] = iWriter.getMax(sym, seq);
			currentFilePos += buf.length;
			index++;
			iSyms.filePos[index] = currentFilePos;
			
		}
		GeneralUtils.safeClose(baos);
	}


	/**
	 * Write out indexes to files (for one chromosome).
	 *
	 * @param iSyms
	 * @param iIDs
	 * @param indexesFileName -- file
	 * @param syms -- symmetries to write out
	 * @param seq -- chromosome
	 * @throws IOException
	 */
	private static void writeIndex(
			IndexedSyms iSyms,
			IndexedIDs iIDs,
			String indexesFileName,
			List<SeqSymmetry> syms,
			MutableAnnotatedBioSeq seq) throws IOException {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			fos = new FileOutputStream(indexesFileName);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			IndexWriter iSymWriter = iSyms.iWriter;
			IndexWriter iIDWriter = iIDs.iWriter;
			for (SeqSymmetry sym : syms) {
				iSymWriter.writeSymmetry(sym, seq, dos);	// write out interval<->symmetries
			}
			if (iIDs.symDos != null) {
				for (SeqSymmetry sym : syms) {
					iIDWriter.writeSymmetry(sym, seq, iIDs.symDos);	// write out ID<->symmetries to genome file
				}
			}
			if (iSymWriter instanceof PSLParser && indexesFileName.toLowerCase().endsWith(".link.psl")) {
				writeAdditionalLinkPSLIndex(indexesFileName, syms, seq, iSyms.typeName);
			}
		} finally {
			GeneralUtils.safeClose(fos);
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
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
			String indexesFileName, List<SeqSymmetry> syms, MutableAnnotatedBioSeq seq, String typeName) throws FileNotFoundException {
		if (DEBUG) {
			System.out.println("in IndexingUtils.writeAdditionalLinkPSLIndex()");
		}

		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		String secondIndexesFileName = indexesFileName.substring(0, indexesFileName.lastIndexOf(".link.psl"));
		secondIndexesFileName += ".link2.psl";
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
	 * Writes out the indexes (for later server reboots).
	 * @param iSyms
	 * @param indexesFileName
	 * @return - success or failure
	 */
	public static boolean writeIndexes(
			IndexedSyms iSyms,
			String indexesFileName) {
		if (DEBUG){
			System.out.println("in IndexingUtils.writeIndexes()");
		}
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			fos = new FileOutputStream(indexesFileName);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			int indexSymsSize = iSyms.min.length;
			dos.writeInt(indexSymsSize);	// used to determine iSyms size.
			for (int i = 0; i < indexSymsSize; i++) {
				dos.writeInt(iSyms.min[i]);
				dos.writeInt(iSyms.max[i]);
				dos.writeLong(iSyms.filePos[i]);
			}
			dos.writeLong(iSyms.filePos[indexSymsSize]);
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		} finally {
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(fos);
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
	 * @param fis
	 * @param filePosStart
	 * @param length
	 * @return
	 */
	public static byte[] readBytesFromFile(FileInputStream fis, long filePosStart, int length) {
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
