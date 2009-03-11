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

package com.affymetrix.genometry.symmetry;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import java.util.List;

public final class EfficientPairSeqSymmetry implements SeqSymmetry {

	protected static final int count = 2;
	protected int startA, startB, endA, endB;
	protected BioSeq seqA, seqB;
	protected List<SeqSymmetry> children;
	protected SeqSymmetry parent;
	protected String id;

	/*public EfficientPairSeqSymmetry(List<SeqSpan> spans) {
	  this(spans.get(0), spans.get(1));
	}

	public EfficientPairSeqSymmetry(SeqSpan spanA, SeqSpan spanB) {
	this(spanA.getStart(), spanA.getEnd(), spanA.getBioSeq(),
	spanB.getStart(), spanB.getEnd(), spanB.getBioSeq());
	}*/

	public EfficientPairSeqSymmetry(int startA, int endA, BioSeq seqA, int startB, int endB, BioSeq seqB) {
		this.startA = startA;
		this.startB = startB;
		this.endA = endA;
		this.endB = endB;
		this.seqA = seqA;
		this.seqB = seqB;
	}

	/*public EfficientPairSeqSymmetry(SeqSymmetry parent, int startA, int endA, BioSeq seqA, int startB, int endB, BioSeq seqB) {
	  this(startA, endA, seqA, startB, endB, seqB);
	  this.parent = parent;
	  }*/

	public SeqSpan getSpan(BioSeq seq) {
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

	public BioSeq getSpanSeq(int i) {
		if (i == 0) { return seqA; }
		else if (i == 1) { return seqB;  }
		else { return null; }
	}

	public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
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

	/*public void setSpan(int index, SeqSpan span) {
	  if (index == 0) {
	  startA = span.getStart();
	  endA = span.getEnd();
	  seqA = span.getBioSeq();
	  }
	  else if (index == 1) {
	  startB = span.getStart();
	  endB = span.getEnd();
	  seqB = span.getBioSeq();
	  } else {
	  throw new RuntimeException(
	  "EfficientPairSeqSymmetry.setSpan requires an index of 0 or 1");
	  }
	  }*/

	/* public void setSpan(int index, int start, int end, BioSeq seq) {
	   if (index == 0) {
	   startA = start;
	   endA = end;
	   seqA = seq;
	   return;
	   } else if (index == 1) {
	   startB = start;
	   endB = end;
	   seqB = seq;
	   return;
	   } else {
	   throw new RuntimeException(
	   "EfficientPairSeqSymmetry.setSpan requires an index of 0 or 1");
	   }
	   }*/

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
