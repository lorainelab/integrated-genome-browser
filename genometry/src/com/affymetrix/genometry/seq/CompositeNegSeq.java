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
 *  A CompositeBioSeq that can have start less than 0.
 *  (Other BioSeqs only have a length, and start = 0, end = length is implicit)
 *  <p>
 *  <strong>Strongly urge</strong> that this is only used when BioSeq
 *  <em>has</em> to have negative coords.
 *  <p>
 *  For example when renumbering from a given point in an
 *  AnnotatedBioSeq and want to do this via genometry, so need a
 *  CompositeBioSeq whose composition is the full span of the
 *  AnnotatedBioSeq, mapped (via a single symmetry) to the
 *  CompositeBioSeq with the desired zero-point at 0 (which pushes
 *  coords 5' to zero-point into negative coords)
 */
public abstract class CompositeNegSeq implements CompositeBioSeq {
	private static final boolean DEBUG_GET_RESIDUES = false;

	/** The index of the first residue of the sequence. */
	int start;
	/** The index of the last residue of the sequence. */
	int end;
	/**
	 * SeqSymmetry to store the sequence in.
	 */
	protected SeqSymmetry compose;
	
	/**
	 * Length of the sequence, stored as a double.  The value is always an
	 * integer and much of the functionality of this class and its sub-classes
	 * is lost if the length is greater than Integer.INT_MAX.
	 */
	protected double length = 0;
	/**
	 * String identifier for the sequence.  This is not guaranteed to be unique.
	 */
	private final String id;


	public CompositeNegSeq(String id, int length) {
		this.id = id;
		this.length = length;
		start = 0;
		end = length;
	}

	/*public CompositeNegSeq(String id) {
		this.id = id;
	}*/

	/**
	 * Returns the integer index of the first residue of the sequence.  Negative
	 * values are acceptable.  The value returned is undefined if the minimum
	 * value is set using setBoundsDouble(double, double) to something outside
	 * of Integer.MIN_VALUE and Integer.MAX_VALUE.
	 *
	 * @return the integer index of the first residue of the sequence.
	 */
	public int getMin() { return start; }

	/**
	 * Returns the integer index of the last residue of the sequence.  The
	 * maximum value must always be greater than the minimum value.  The value
	 * returned is undefined if the maximum value is set using
	 * setBoundsDouble(double, double) to something outside of Integer.MIN_VALUE
	 * and Integer.MAX_VALUE.
	 *
	 * @return the integer index of the last residue of the sequence.
	 */
	public int getMax() { return end; }

	/**
	 * Sets the start and end of the sequence as double values.
	 * <p />
	 * <em>WARNING:</em> min and max are stored intenally using integers.  If
	 * min or max are outside of the range Integer.MIN_VALUE and
	 * Interger.MAX_VALUE, the values will not be stored properly.  The length
	 * (min - max) is computed and stored as a double before min and max are
	 * downcast to int.
	 *
	 * @param min the index of the first residue of the sequence, as a double.
	 * @param max the index of the last residue of the sequence, as a double.
	 */
	public void setBoundsDouble(double min, double max) {
		length = max - min;
		if (min < Integer.MIN_VALUE) { start = Integer.MIN_VALUE + 1; }
		else { start = (int)min; }
		if (max > Integer.MAX_VALUE) { end = Integer.MAX_VALUE - 1; }
		else { end = (int)max; }
	}

	/**
	 * Sets the start and end of the sequence
	 *
	 * @param min the index of the first residue of the sequence.
	 * @param max the index of the last residue of the sequence.
	 */
	public void setBounds(int min, int max) {
		start = min;
		end = max;
		//    length = end - start;
		length = (double)end - (double)start;
	}

	/**
	 * Returns true if all residues on the sequence are available.
	 *
	 * @return true if all residues on the sequence are available.
	 */
		public boolean isComplete() {
			return isComplete(start, end);
		}

	/**
	 * Returns all residues on the sequence.
	 *
	 * @return a String containing all residues on the sequence.
	 */
		public String getResidues() {
			return getResidues(start, end);
		}

	/**
	 * Returns the residues on the sequence between start and end using the
	 * fillchar to fill any gaps in the sequence.  Unknown if this implementation
	 * is inclusive or exclusive on start and end.
	 *
	 * @param  start    the start index (inclusive?)
	 * @param  end      the end index (exclusive?)
	 * @param  fillchar the character to fill empty residues in the sequence with.
	 * @return          a String containing residues between start and end.
	 */
		public String getResidues(int res_start, int res_end, char fillchar) {
			SeqSpan residue_span = new SimpleSeqSpan(res_start, res_end, this);
			int reslength = Math.abs(res_end - res_start);
			char[] char_array = new char[reslength];
			java.util.Arrays.fill(char_array, fillchar);
			SeqSymmetry rootsym = this.getComposition();
			if (rootsym == null)  { return null; }
			// adjusting index into array to compensate for possible seq start < 0
			int array_offset = -start;
			getResidues(residue_span, rootsym, char_array);
			// Note that new String(char[]) causes the allocation of a second char array
			String res = new String(char_array);
			return res;
		}

	/**
	 * Function for finding residues.  This function is a bit of a mess:
	 * the implementation is more confusing than it needs to be.
	 *
	 * @param this_residue_span the SeqSpan to find residues on
	 * @param sym               the SeqSymmetry to search for residues
	 * @param residues          the character array to be filled with residues
	 */
	protected void getResidues(SeqSpan this_residue_span, SeqSymmetry sym, char[] residues) {
		int symCount = sym.getChildCount();
		if (symCount == 0) {
			SeqSpan this_comp_span = sym.getSpan(this);
			if (SeqUtils.intersects(this_comp_span, this_residue_span)) {
				BioSeq other_seq = SeqUtils.getOtherSeq(sym, this);
				SeqSpan other_comp_span = sym.getSpan(other_seq);
				MutableSeqSpan ispan = new SimpleMutableSeqSpan();
				boolean intersects = SeqUtils.intersection(this_comp_span, this_residue_span, ispan);
				MutableSeqSpan other_residue_span = new SimpleMutableSeqSpan();
				SeqUtils.transformSpan(ispan, other_residue_span, other_seq, sym);
				boolean opposite_strands = this_comp_span.isForward() ^ other_comp_span.isForward();
				boolean resultForward = opposite_strands ^ this_residue_span.isForward();
				String spanResidues;
				if (resultForward) {
					spanResidues = other_seq.getResidues(other_residue_span.getMin(), other_residue_span.getMax());
				} else {
					spanResidues = other_seq.getResidues(other_residue_span.getMax(), other_residue_span.getMin());
				}
				if (spanResidues != null) {
					if (DEBUG_GET_RESIDUES) {
						System.out.println(spanResidues.substring(0, 15) + "..." + spanResidues.substring(spanResidues.length() - 15));
						System.out.println("desired span: " + SeqUtils.spanToString(this_residue_span));
						System.out.println("child residue span: " + SeqUtils.spanToString(this_comp_span));
						System.out.println("intersect(child_span, desired_span): " + SeqUtils.spanToString(ispan));
						System.out.println("other seq span: " + SeqUtils.spanToString(other_residue_span));
						System.out.println("opposite strands: " + opposite_strands);
						System.out.println("result forward: " + resultForward);
						System.out.println("start < end: " + (other_residue_span.getStart() < other_residue_span.getEnd()));
						System.out.println("");
					}
					int offset = ispan.getMin() - this_residue_span.getMin();
					for (int j = 0; j < spanResidues.length(); j++) {
						residues[offset + j] = spanResidues.charAt(j);
					}
				}
			}
		} else {
			// recurse to children
			for (int i = 0; i < symCount; i++) {
				SeqSymmetry childSym = sym.getChild(i);
				getResidues(this_residue_span, childSym, residues);
			}
		}
	}

	public SeqSymmetry getComposition() {
		return compose;
	}

	public void setComposition(SeqSymmetry compose) {
		this.compose = compose;
	}

	/**
	 * Returns the number of residues in the sequence as a double.
	 *
	 * @return the number of residues in the sequence as a double
	 */
	public double getLengthDouble() {
		return length;
	}

	public int getLength() {
		if (length > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE - 1;
		} else {
			return (int) length;
		}
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

		if (rootsym == null) {
			return false;
		}
		int comp_count = rootsym.getChildCount();
		if (comp_count == 0) {
			BioSeq other_seq = SeqUtils.getOtherSeq(rootsym, this);
			return other_seq.isComplete(start, end);
		}

		boolean comp_complete = true;
		for (int i = 0; i < comp_count; i++) {
			SeqSymmetry comp_sym = rootsym.getChild(i);
			BioSeq other_seq = SeqUtils.getOtherSeq(comp_sym, this);
			comp_complete = (comp_complete & other_seq.isComplete());
		}
		return comp_complete;
	}

	public String getID() {
		return id;
	}

	/**
	 * Returns all residues on the sequence.
	 *
	 * @return a String containing all residues on the sequence
	 */
	public String getResidues(int start, int end) {
		return getResidues(start, end, ' ');
	}
}
