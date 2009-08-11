package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * IndexWriter interface.
 * To use this interface, a parser must have:
 * 1.  The ability to write a single symmetry
 * 2.  The ability to sort its symmetries.
 * @author jnicol
 */
public interface IndexWriter {
	/**
	 * Write a single symmetry to the file.
	 * It is assumed that the file only uses one chromosome.
	 * @param sym
	 * @param seq - necessary for backwards compatibility.
	 * @param dos
	 * @throws IOException
	 */
	public void writeSymmetry(SeqSymmetry sym, MutableAnnotatedBioSeq seq, DataOutputStream dos) throws IOException;

	/**
	 * Parse the given stream, returning a list of SeqSymmetries.
	 * @return list of SeqSymmetries.
	 */
	public List parse(DataInputStream dis, String annot_type, AnnotatedSeqGroup group);

	/**
	 * Get a comparator for the class.
	 * @return comparator.
	 */
	public Comparator getComparator(MutableAnnotatedBioSeq seq);

	/**
	 * Get the minimum of a given symmetry.
	 * @param sym
	 * @return
	 */
	public int getMin(SeqSymmetry sym, MutableAnnotatedBioSeq seq);

	/**
	 * Get the maximum of a given symmetry.
	 * @param sym
	 * @return
	 */
	public int getMax(SeqSymmetry sym, MutableAnnotatedBioSeq seq);

	/**
	 * Get the preferred formats.
	 * @return
	 */
	public List<String> getFormatPrefList();
}
