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

package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphStateI;

/**
 *  A SeqSymmetry for holding graph data.
 */
public class GraphSym extends SimpleSymWithProps {

	/** A property that can optionally be set to give a hint about the graph strand for display. */
	public static final String PROP_GRAPH_STRAND = "Graph Strand";
	public static final Integer GRAPH_STRAND_PLUS = new Integer(1);
	public static final Integer GRAPH_STRAND_MINUS = new Integer(-1);
	public static final Integer GRAPH_STRAND_BOTH = new Integer(2);
	
	private int xcoords[];
	private float[] float_y;


	protected MutableAnnotatedBioSeq graph_original_seq;
	private String gid;

	/**
	 *  id_locked is a temporary fix to allow graph id to be changed after construction, 
	 *  but then lock once lockID() is called.
	 *  Really want to forbid setting id except in constructor, but currently some code 
	 *    needs to modify this after construction, but before adding as annotation to graph_original_seq
	 */
	private boolean id_locked = false;

	/** Constructor.  Subclasses should provide a constructor that specifies the
	 *  y-coordinate array.
	 */
	public GraphSym(int[] x, float[] y, String id, MutableAnnotatedBioSeq seq) {
		super();
		setCoords(x, y);
		this.graph_original_seq = seq;

		int start = 0;
		int end = seq.getLength();
		if (x != null && x.length >= 1) {
			start = x[0];
			end = x[x.length-1];
		}
		SeqSpan span = new SimpleSeqSpan(start, end, seq);
		this.addSpan(span);
		this.xcoords = x;
		this.gid = id;
	}

	public final void lockID() {
		setLockID(true);
	}

	private final void setLockID(boolean b) {
		id_locked = b;
	}

	private final boolean isLockID() {
		return id_locked;
	}

	public final void setGraphName(String name) {
		getGraphState().getTierStyle().setHumanName(name);
		setProperty("name", name);
	}

	public final String getGraphName() {
		String gname = getGraphState().getTierStyle().getHumanName();
		if (gname == null) {
			gname = this.getID();
		}
		return gname;
	}

	@Override
	public String getID() {
		return gid;
	}

	/**
	 *  Not allowed to call GraphSym.setID(), id
	 */
	@Override
	public void setID(String id) {
		if (isLockID()) {
			SingletonGenometryModel.getLogger().warning("called GraphSym.setID() while id was locked:  " + this.getID() + " -> " + id);
		}
		else {
			gid = id;
		}
		//    throw new RuntimeException("Attempted to call GraphSym.setID(), but not allowed to modify GraphSym id!");
	}

	/**
	 *  Sets the x and y coordinates.
	 *  @param x an array of int, or null.
	 *  @param y must be an array of float of same length as x, or null if x is null.
	 */
	public void setCoords(int[] x, float[] y) {
		if ((y == null && x != null) || (x == null && y != null)) {
			throw new IllegalArgumentException("Y-coords cannot be null if x-coords are not null.");
		}
		if (y != null && x != null && y.length  != x.length) {
			throw new IllegalArgumentException("Y-coords and x-coords must have the same length.");
		}
		this.xcoords = x;
		this.float_y = y;
	}



	public final int getPointCount() {
		if (xcoords == null) { return 0; }
		else { return xcoords.length; }
	}

	public final int[] getGraphXCoords() {
		return xcoords;
	}

	public final int getGraphXCoord(int i) {
		return xcoords[i];
	}

	public final int getMinXCoord() {
		return xcoords[0];
	}

	public final int getMaxXCoord() {
		return xcoords[xcoords.length-1];
	}

	public float getGraphYCoord(int i) {
		return float_y[i];
	}

	/**
	 *  Returns the y coordinate as a String.
	 */
	public String getGraphYCoordString(int i) {
		return Float.toString(float_y[i]);
	}

	public float[] getGraphYCoords() {
		return float_y;
	}

	/** Returns a copy of the graph Y coordinates as a float[], even if the Y coordinates
	 *  were originally specified as non-floats.
	 */
	public float[] copyGraphYCoords() {
		float[] dest = new float[float_y.length];
		System.arraycopy(float_y, 0, dest, 0, float_y.length);
		return dest;
	}

	/**
	 *  Get the seq that the graph's xcoords are specified in
	 */
	public MutableAnnotatedBioSeq getGraphSeq() {
		return graph_original_seq;
	}

	/**
	 *  Returns the graph state.  Will never be null.
	 */
	public GraphStateI getGraphState() {
		GraphStateI state = DefaultStateProvider.getGlobalStateProvider().getGraphState(this.gid);
		return state;
	}

	/**
	 *  Overriding request for property "method" to return graph name.
	 */
	@Override
	public Object getProperty(String key) {
		if (key.equals("method")) {
			return getGraphName();
		}
		else if (key.equals("id")) {
			return this.getID();
		}
		else {
			return super.getProperty(key);
		}
	}

	@Override
	public boolean setProperty(String name, Object val) {
		if (name.equals("id")) {
			this.setID(name);
			return false;
		}
		else {
			return super.setProperty(name, val);
		}
	}
}
