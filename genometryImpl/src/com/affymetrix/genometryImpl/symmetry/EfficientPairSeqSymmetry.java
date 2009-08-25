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

package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import java.util.List;

public final class EfficientPairSeqSymmetry implements SeqSymmetry {

	protected static final int count = 2;
	protected int startA, startB, endA, endB;
	protected MutableAnnotatedBioSeq seqA, seqB;
	protected List<SeqSymmetry> children;
	protected SeqSymmetry parent;
	protected String id;

	public EfficientPairSeqSymmetry(int startA, int endA, MutableAnnotatedBioSeq seqA, int startB, int endB, MutableAnnotatedBioSeq seqB) {
		this.startA = startA;
		this.startB = startB;
		this.endA = endA;
		this.endB = endB;
		this.seqA = seqA;
		this.seqB = seqB;
	}

	public SeqSpan getSpan(MutableAnnotatedBioSeq seq) {
		if (seqA == seq) { return new SimpleSeqSpan(startA, endA, seqA); }
		else if (seqB == seq) { return new SimpleSeqSpan(startB, endB, seqB); }
		return null;
	}

	public int getSpanCount() {
		return count;
	}

	public SeqSpan getSpan(int i) {
		if (i == 0) { return new SimpleSeqSpan(startA, endA, seqA); }
		else if (i == 1) { return new SimpleSeqSpan(startB, endB, seqB); }
		else { return null; }
	}

	public MutableAnnotatedBioSeq getSpanSeq(int i) {
		if (i == 0) { return seqA; }
		else if (i == 1) { return seqB;  }
		else { return null; }
	}

	public boolean getSpan(MutableAnnotatedBioSeq seq, MutableSeqSpan span) {
		if (seqA == seq) {
			span.setStart(startA);
			span.setEnd(endA);
			span.setBioSeq(seqA);
			return true;
		}
		else if (seqB == seq) {
			span.setStart(startB);
			span.setEnd(endB);
			span.setBioSeq(seqB);
			return true;
		}
		return false;
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == 0) {
			span.setStart(startA);
			span.setEnd(endA);
			span.setBioSeq(seqA);
			return true;
		}
		else if (index == 1) {
			span.setStart(startB);
			span.setEnd(endB);
			span.setBioSeq(seqB);
			return true;
		}
		return false;
	}


	public int getChildCount() {
		if (null != children)
			return children.size();
		else
			return 0;
	}

	public SeqSymmetry getChild(int index) {
		if (null != children && index < children.size())
			return children.get(index);
		else
			return null;
	}

	public String getID() { return id; }
}
