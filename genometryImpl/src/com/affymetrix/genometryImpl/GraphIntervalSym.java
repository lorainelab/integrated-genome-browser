/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.ScoredSingletonSym;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.span.SimpleSeqSpan;

/**
 *  A SeqSymmetry for holding graph for graphs that have y values that apply to
 *  intervals.  So instead of (x,y) there is (x_start, x_width, y).
 */
public class GraphIntervalSym extends GraphSymFloat {
	int wcoords[];

	public GraphIntervalSym(int[] x, int[] width, float[] y, String id, BioSeq seq) {
		super(x,y,id,seq);
		this.wcoords = width;

		if (xcoords.length != y.length || xcoords.length != wcoords.length) {
			throw new IllegalArgumentException("X,W, and Y arrays must have the same length");
		}

		this.removeSpans();
		int xmin = x[0];
		int xmax = x[x.length-1] + width[width.length-1];
		this.addSpan(new SimpleSeqSpan(xmin, xmax, seq));
	}

	public int[] getGraphWidthCoords() {
		return wcoords;
	}

	public int getChildCount() {
		return xcoords.length;
	}

	/**
	 *  Constructs a temporary SeqSymmetry to represent the graph value of a single span.
	 *  The returned SeqSymmetry will implement the {@link Scored} interface.
	 */
	public SeqSymmetry getChild(int index) {
		return new ScoredSingletonSym(xcoords[index], xcoords[index]+wcoords[index], graph_original_seq, getGraphYCoord(index));
	}
}
