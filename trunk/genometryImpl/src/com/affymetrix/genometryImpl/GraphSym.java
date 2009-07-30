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

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphStateI;

/**
 *  A SeqSymmetry for holding graph data.
 */
public abstract class GraphSym extends SimpleSymWithProps {

	/** A property that can optionally be set to give a hint about the graph strand for display. */
	public static final String PROP_GRAPH_STRAND = "Graph Strand";
	public static final Integer GRAPH_STRAND_PLUS = new Integer(1);
	public static final Integer GRAPH_STRAND_MINUS = new Integer(-1);
	public static final Integer GRAPH_STRAND_BOTH = new Integer(2);
	public static final Integer GRAPH_STRAND_NEITHER = new Integer(0);

	int xcoords[];
	MutableAnnotatedBioSeq graph_original_seq;
	String gid;

	/**
	 *  id_locked is a temporary fix to allow graph id to be changed after construction, 
	 *  but then lock once lockID() is called.
	 *  Really want to forbid setting id except in constructor, but currently some code 
	 *    needs to modify this after construction, but before adding as annotation to graph_original_seq
	 */
	boolean id_locked = false;

	/** Constructor.  Subclasses should provide a constructor that specifies the
	 *  y-coordinate array.
	 */
	protected GraphSym(int[] x, String id, MutableAnnotatedBioSeq seq) {
		super();
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

	//public abstract void setCoords(int[] x, Object y);

	public void lockID() {
		setLockID(true);
	}

	public void setLockID(boolean b) {
		id_locked = b;
	}

	public boolean isLockID() {
		return id_locked;
	}

	public void setGraphName(String name) {
		getGraphState().getTierStyle().setHumanName(name);
		setProperty("name", name);
	}

	public String getGraphName() {
		String gname = getGraphState().getTierStyle().getHumanName();
		if (gname == null) {
			gname = this.getID();
		}
		return gname;
	}

	public String getID() {
		return gid;
	}

	/**
	 *  Not allowed to call GraphSym.setID(), id
	 */
	public void setID(String id) {
		if (isLockID()) {
			SingletonGenometryModel.getLogger().warning("called GraphSym.setID() while id was locked:  " + this.getID() + " -> " + id);
		}
		else {
			gid = id;
		}
		//    throw new RuntimeException("Attempted to call GraphSym.setID(), but not allowed to modify GraphSym id!");
	}

	public int getPointCount() {
		if (xcoords == null) { return 0; }
		else { return xcoords.length; }
	}

	public int[] getGraphXCoords() {
		return xcoords;
	}

	/**
	 *  Returns the y coordinate as a float, even if it is internally stored
	 *  as an integer or in some other form.
	 */
	public abstract float getGraphYCoord(int i);

	/**
	 *  Returns the y coordinate as a String.
	 */
	public abstract String getGraphYCoordString(int i);

	/** Returns a copy of the graph Y coordinates as a float[], even if the Y coordinates
	 *  were originally specified as non-floats.
	 */
	public abstract float[] copyGraphYCoords();


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
