/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

/**
 *  A SeqSymmetry for holding integer-based graph data.
 */
public final class GraphSymShort extends GraphSym {

	private short[] short_y;

	public GraphSymShort(int[] x, short[] y, String id, MutableAnnotatedBioSeq seq) {
		super(x, id, seq);
		setCoords(x, y);
	}

	/**
	 *  Sets the x and y coordinates.
	 *  @param x an array of int, or null.
	 *  @param y must be an array of int of same length as x, or null if x is null.
	 */
	/*public void setCoords(int[] x, Object y) {
		setCoords(x, (short[]) y);
	}*/

	/**
	 *  Sets the x and y coordinates.
	 *  @param x an array of int, or null.
	 *  @param y must be an array of int of same length as x, or null if x is null.
	 */
	public void setCoords(int[] x, short[] y) {
		if ((y == null && x != null) || (x == null && y != null)) {
			throw new IllegalArgumentException("Y-coords cannot be null if x-coords are not null.");
		}
		if (y != null && x != null && y.length  != x.length) {
			throw new IllegalArgumentException("Y-coords and x-coords must have the same length.");
		}
		this.xcoords = x;
		this.short_y = y;
	}

	public float getGraphYCoord(int i) {
		return (float) short_y[i];
	}

	public String getGraphYCoordString(int i) {
		return Short.toString(short_y[i]);
	}

	public short[] getGraphYCoords() {
		return short_y;
	}

	public float[] copyGraphYCoords() {
		int pcount = getPointCount();
		float[] new_ycoords = new float[pcount];
		for (int i=0; i<pcount; i++) {
			new_ycoords[i] = (float) short_y[i];
		}
		return new_ycoords;
	}
}
