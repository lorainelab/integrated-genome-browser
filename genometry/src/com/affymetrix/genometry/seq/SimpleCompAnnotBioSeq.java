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
import com.affymetrix.genometry.util.DNAUtils;
import java.util.*;

public abstract class SimpleCompAnnotBioSeq
	extends CompositeNegSeq
	implements CompositeBioSeq, MutableAnnotatedBioSeq {

	boolean DEBUG = false;
	// GAH 8-14-2002: need a residues field in case residues need to be cached
	// (rather than derived from composition), or if we choose to store residues here
	// instead of in composition seqs in case we actually want to compose/cache
	// all residues...
	protected String residues;

	protected List<SeqSymmetry> annots;

	@Override
	public String getResidues(int start, int end, char fillchar) {
		int residue_length = this.getLength();
		if (start < 0 || residue_length <= 0) {
			return null;
		}

		// Sanity checks on argument size.
		start = Math.min(start, residue_length);
		end = Math.min(end, residue_length);
		if (start <= end) {
			end = Math.min(end, start+residue_length);
		}
		else {
			start = Math.min(start, end+residue_length);
		}

		if (residues == null) {
			return super.getResidues(start, end, fillchar);
		}

		if (start <= end) {
			return residues.substring(start, end);
		}

		// start > end -- that means reverse complement.
		return DNAUtils.reverseComplement(residues.substring(end, start));
	}

	@Override
	public boolean isComplete(int start, int end) {
		if (residues != null) { return true; }
		else  { return super.isComplete(start, end); }
	}

	//-----------------------------------------------------
	// BEGIN Methods copied from SimpleAnnotatedBioSeq
	//-----------------------------------------------------
	public SimpleCompAnnotBioSeq(String id, int length)  {
		super(id, length);
	}

	/*public SimpleCompAnnotBioSeq(String id)  {
		super(id);
	}*/

	//public SimpleCompAnnotBioSeq()  { }

	public void addAnnotation(SeqSymmetry annot) {
		if (null == annots) { annots = new ArrayList<SeqSymmetry>(); }
		annots.add(annot);
	}

	public void removeAnnotation(SeqSymmetry annot) {
		if (null != annots) {
			annots.remove(annot);
		}
	}

	/*public void removeAnnotation(int index) {
		if (null != annots) {
			annots.remove(index);
		}
	}*/

	public int getAnnotationCount() {
		if (null != annots) return annots.size();
		else return 0;
	}

	public SeqSymmetry getAnnotation(int index) {
		if (null != annots && index < annots.size())
			return annots.get(index);
		else
			return null;
	}

	/** NOT YET IMPLEMENTED */
	//public SeqSymmetry getAnnotationByID(String id) { return null; }
	/** NOT YET IMPLEMENTED */
	//public List getIntersectedAnnotations(SeqSpan span) { return null; }
	/** NOT YET IMPLEMENTED */
	//public List getContainedAnnotations(SeqSpan span) { return null; }
	//-----------------------------------------------------
	// END Methods copied from SimpleAnnotatedBioSeq
	//-----------------------------------------------------


	//-----------------------------------------------------
	// BEGIN methods copied from SimpleBioSeq
	//-----------------------------------------------------
	//public void setID(String id) { this.id = id; }
	public void setLength(int length) {
		//    this.length = length;
		setBounds(0, length);  // sets start, end, bounds

		// if length does not agree with length of residues, null out residues
		if ((residues != null) && (residues.length() != length)) {
			System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
					", seq = " + this.length + ", nulling out residues ****");
			residues = null;
		}
	}

	public void setResidues(String residues) {
		if (DEBUG)  { System.out.println("**** called SimpleCompAnnotBioSeq.setResidues()"); }
		if (residues.length() != this.length) {
			System.out.println("********************************");
			System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
					", seq = " + this.length + " ****");
			System.out.println("********************************");
		}
		this.residues = residues;
		this.length = residues.length();
	}
	//-----------------------------------------------------
	// END methods copied from SimpleBioSeq
	//-----------------------------------------------------

}


