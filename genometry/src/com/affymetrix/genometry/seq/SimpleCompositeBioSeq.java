/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometry.seq;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.SeqUtils;

/**
 * A simple implementation of CompositeBioSeq. This class must be subclassed
 * to be used as there is no method to modify the length of the sequence. In
 * the future, this class may become an abstract class or be merged with its
 * subclass (CompositeNegSeq).
 */
public class SimpleCompositeBioSeq implements CompositeBioSeq {

	/** Boolean debug variable for the class. */
	public boolean DEBUG_GET_RESIDUES = false;
	/**
	 * String identifier for the sequence.  This is not guaranteed to be unique.
	 */
	protected String id;
	/** Length of the sequence. */
	//  protected int length = 0;
	/**
	 * Length of the sequence, stored as a double.  The value is always an
	 * integer and much of the functionality of this class and its sub-classes
	 * is lost if the length is greater than Interger.INT_MAX.
	 */
	protected double length = 0;
	/** SeqSymetry to store the sequence in. */
	protected SeqSymmetry compose;

	/** This class should never be instantiated. */
	public SimpleCompositeBioSeq(String id) {
		this.id = id;
	}

	/** This class should never be instantiated. */
	public SimpleCompositeBioSeq() { }

	public SeqSymmetry getComposition() {
		return compose;
	}

	public void setComposition(SeqSymmetry compose) {
		this.compose = compose;
	}

	public String getID() { return id; }

	public int getLength() {
		if (length > Integer.MAX_VALUE)  { return Integer.MAX_VALUE - 1; }
		else  { return (int)length; }
	}
	//  public int getMin() { return 0; }
	//  public int getMax() { return getLength(); }

	/**
	 * Returns the number of residues in the sequence as a double.
	 *
	 * @return the number of residues in the sequence as a double
	 */
	public double getLengthDouble() { return length; }

	/**
	 * Returns all residues on the sequence.
	 *
	 * This function is never used, as it is overridden in the only subclass and
	 * this class can not be called directly.
	 *
	 * @return a String containing all residues on the sequence
	 */
	public String getResidues() {
		// may want to do a caching strategy at some point, in case there are repeated
		//   getResidues calls...
		return getResidues(0, getLength());
	}

	public String getResidues(int start, int end) {
		return getResidues(start, end, ' ');
	}

	/**
	 * Returns the residues on the sequence between start and end using the
	 * fillchar to fill any gaps in the sequence.  Unknown if this implementation
	 * is inclusive or exclusive on start and end.
	 *
	 * This function is never used, as it is overridden in the only subclass and
	 * this class can not be called directly.
	 *
	 * @param  start    the start index (inclusive?)
	 * @param  end      the end index (exclusive?)
	 * @param  fillchar the character to fill empty residues in the sequence with
	 * @return          a String containing residues between start and end
	 */
	public String getResidues(int start, int end, char fillchar) {
		int residue_length = this.getLength();
		if (start < 0 || residue_length <= 0) {
			return null;
		}

		if (this.getComposition() == null) {
			return null;
		}

		// Sanity checks on argument size.
		start = Math.min(start, residue_length);
		end = Math.min(end, residue_length);
		if (start <= end) {
			end = Math.min(end, start + residue_length);
		} else {
			start = Math.min(start, end + residue_length);
		}

		SeqSpan residue_span = new SimpleSeqSpan(start, end, this);
		int reslength = Math.abs(end - start);
		char[] char_array = new char[reslength];
		// start with all spaces
		java.util.Arrays.fill(char_array, fillchar);
		SeqSymmetry rootsym = this.getComposition();
		getResidues(residue_span, fillchar, rootsym, char_array, 0);
		String result = new String(char_array);
		if (DEBUG_GET_RESIDUES) {
			System.out.println(result.substring(0, 15) + "..." + result.substring(result.length() - 15));
		}

		return result;
	}

	/**
	 * Function for finding residues.  This function is a bit of a mess:
	 * several of the parameters are unused and the implementation is more
	 * confusing than it needs to be.
	 *
	 * @param this_residue_span the SeqSpan to find residues on
	 * @param fillchar          unused
	 * @param sym               the SeqSymmetry to search for residues
	 * @param residues          the character array to be filled with residues
	 * @param buf_offset        unused
	 */
	protected void getResidues(SeqSpan this_residue_span, char fillchar,
			SeqSymmetry sym, char[] residues, int buf_offset) {
		int symCount = sym.getChildCount();
		if (symCount == 0) {  // leaf symmetry, need to retrieve residues from other seq in sym
			SeqSpan this_comp_span = sym.getSpan(this);
			if (SeqUtils.intersects(this_comp_span, this_residue_span)) {
				BioSeq other_seq = SeqUtils.getOtherSeq(sym, this);
				SeqSpan other_comp_span = sym.getSpan(other_seq);
				MutableSeqSpan ispan = new SimpleMutableSeqSpan();
				boolean intersects = SeqUtils.intersection(this_comp_span, this_residue_span, ispan);
				MutableSeqSpan other_residue_span = new SimpleMutableSeqSpan();
				// transform intersection to composition sym's other span, via composition sym.
				// format: transformSpan(srcSpan, dstSpan, dstSeq, transformSym)
				SeqUtils.transformSpan(ispan, other_residue_span, other_seq, sym);

				boolean opposite_strands = this_comp_span.isForward() ^ other_comp_span.isForward();
				boolean resultForward = opposite_strands ^ this_residue_span.isForward();
				String spanResidues;

				if (resultForward) {
					spanResidues = other_seq.getResidues(other_residue_span.getMin(),
							other_residue_span.getMax());
				}
				else {
					spanResidues = other_seq.getResidues(other_residue_span.getMax(),
							other_residue_span.getMin());
				}

				if (spanResidues != null) {
					if (DEBUG_GET_RESIDUES) {
						System.out.println(spanResidues.substring(0, 15) + "..." +
								spanResidues.substring(spanResidues.length()-15));
						System.out.println("desired span: " + SeqUtils.spanToString(this_residue_span));
						System.out.println("child residue span: " + SeqUtils.spanToString(this_comp_span));
						System.out.println("intersect(child_span, desired_span): " + SeqUtils.spanToString(ispan));
						System.out.println("other seq span: " + SeqUtils.spanToString(other_residue_span));
						System.out.println("opposite strands: " + opposite_strands);
						System.out.println("result forward: " + resultForward);
						System.out.println("start < end: " +
								(other_residue_span.getStart() < other_residue_span.getEnd()));
						System.out.println("");
					}

					int offset = ispan.getMin() - this_residue_span.getMin();
					for (int j=0; j<spanResidues.length(); j++) {
						residues[offset+j] = spanResidues.charAt(j);
					}
				}
			}
		}
		else {
			// recurse to children
			for (int i=0; i<symCount; i++) {
				SeqSymmetry childSym = sym.getChild(i);
				getResidues(this_residue_span, fillchar, childSym, residues, buf_offset);
			}
		}
	}

	/**
	 * Returns true if all residues on the sequence are available.
	 *
	 * This function is never used, as it is overridden in the only subclass and
	 * this class can not be called directly.
	 *
	 * @return true if all residues on the sequence are available
	 */
	public boolean isComplete() {
		return isComplete(0, this.getLength());
	}

	/**
	 * Returns true if all residues between start and end are available.  Unknown
	 * if implementations of this function are inclusive or exclusive on start
	 * and end.
	 * <p />
	 * <em>WARNING:</em> This implementation is flawed.  It only verifies that
	 * all SeqSymmetrys are complete, not that the SeqSymmetrys completely
	 * cover the range in question.
	 *
	 * @param  start the start index (inclusive?)
	 * @param  end   the end index (exclusive?)
	 * @return       true if all residues betwen start and end are available
	 */
	public boolean isComplete(int start, int end) {
		// assuming that if all sequences the composite is composed of are
		//    complete, then composite is also complete
		//    [which is an invalid assumption! Because that further assumes that composed seq
		//     is fully covered by the sequences that it is composed from...]
		SeqSymmetry rootsym = this.getComposition();
		// SeqUtils.printSymmetry(rootsym);
		if (rootsym == null) {
			return false;
		}
		int comp_count = rootsym.getChildCount();
		if (comp_count == 0) {
			//      System.out.println("    composition is only a single sym, no children");
			BioSeq other_seq = SeqUtils.getOtherSeq(rootsym, this);
			return other_seq.isComplete(start, end);
		}
		else {
			boolean comp_complete = true;
			for (int i=0; i<comp_count; i++) {
				SeqSymmetry comp_sym = rootsym.getChild(i);
				BioSeq other_seq = SeqUtils.getOtherSeq(comp_sym, this);
				comp_complete = (comp_complete & other_seq.isComplete());  // ignoring range -- would require a transform
			}
			return comp_complete;
		}
	}

}


