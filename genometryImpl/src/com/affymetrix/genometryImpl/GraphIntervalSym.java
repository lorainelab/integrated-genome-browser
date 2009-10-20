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

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

/**
 *  A SeqSymmetry for holding graph for graphs that have y values that apply to
 *  intervals.  So instead of (x,y) there is (x_start, x_width, y).
 */
public final class GraphIntervalSym extends GraphSymFloat {
	private int wcoords[];

	public GraphIntervalSym(int[] x, int[] width, float[] y, String id, MutableAnnotatedBioSeq seq) {
		super(x,y,id,seq);

		if (this.getPointCount() != y.length || this.getPointCount() != width.length) {
			throw new IllegalArgumentException("X,W, and Y arrays must have the same length");
		}

		this.wcoords = width;

		this.removeSpans();
		int xmin = x[0];
		int xmax = x[x.length-1] + width[width.length-1];
		this.addSpan(new SimpleSeqSpan(xmin, xmax, seq));
	}

	public int[] getGraphWidthCoords() {
		return wcoords;
	}

	public int getGraphWidthCoord(int i) {
		return wcoords[i];
	}

	public int getGraphWidthCount() {
		if (wcoords == null) {
			return 0;
		}
		return wcoords.length;
	}

	@Override
	public int getChildCount() {
		return this.getPointCount();
	}

	/**
	 *  Constructs a temporary SeqSymmetry to represent the graph value of a single span.
	 *  The returned SeqSymmetry will implement the {@link Scored} interface.
	 */
	@Override
	public SeqSymmetry getChild(int index) {
		return new ScoredSingletonSym(
				this.getGraphXCoord(index),
				this.getGraphXCoord(index)+ getGraphWidthCoord(index),
				graph_original_seq,
				getGraphYCoord(index));
	}
}
