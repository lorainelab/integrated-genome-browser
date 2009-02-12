/**
 *   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.genometry.span;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.MutableSeqSpan;

public class MutableDoubleSeqSpan extends DoubleSeqSpan implements MutableSeqSpan {

	public MutableDoubleSeqSpan(double start, double end, BioSeq seq) {
		super(start, end, seq);
	}

	public MutableDoubleSeqSpan(SeqSpan span) {
		this(span.getStartDouble(), span.getEndDouble(), span.getBioSeq());
	}

	public MutableDoubleSeqSpan()  {
		this(0, 0, null);
	}

	public void set(int start, int end, BioSeq seq) {
		this.start = (double)start;
		this.end = (double)end;
		this.seq = seq;
	}

	public void setCoords(int start, int end) {
		this.start = (double)start;
		this.end = (double)end;
	}

	public void setStart(int start) {
		this.start = (double)start;
	}

	public void setEnd(int end) {
		this.end = (double)end;
	}

	public void setBioSeq(BioSeq seq) {
		this.seq = seq;
	}


	public void setDouble(double start, double end, BioSeq seq) {
		this.start = start;
		this.end = end;
		this.seq = seq;
	}

	public void setCoordsDouble(double start, double end) {
		this.start = start;
		this.end = end;
	}

	public void setStartDouble(double start) {
		this.start = start;
	}

	public void setEndDouble(double end) {
		this.end = end;
	}

}





