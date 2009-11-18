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
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 *  A SeqSymmetry for holding graph data.
 */
public class GraphSym extends SimpleSymWithProps {

	/** A property that can optionally be set to give a hint about the graph strand for display. */
	public static final String PROP_GRAPH_STRAND = "Graph Strand";
	public static final Integer GRAPH_STRAND_PLUS = new Integer(1);
	public static final Integer GRAPH_STRAND_MINUS = new Integer(-1);
	public static final Integer GRAPH_STRAND_BOTH = new Integer(2);
	
	private int pointCount = 0;	// count of points
	private int xMin = 0;		// min X coord
	private int xMax = 0;		// max X coord

	private boolean hasWidth = false;

	private final BioSeq graph_original_seq;
	private String gid;

	private static final int BUFSIZE = 100000;	// buffer size
	private int bufStart = 0;	// current buffer start
	//private int xBuf[];
	private float yBuf[];
	private int wBuf[];
	private File bufFile;

	// index for faster searching
	//private int xIndex[];
	//private float yIndex[];
	//private int wIndex[];

	private int xCoords[];	// too slow to do indexing of x right now

	private double xDelta = 0.0f;	// used by GraphGlyph

	/**
	 *  id_locked is a temporary fix to allow graph id to be changed after construction, 
	 *  but then lock once lockID() is called.
	 *  Really want to forbid setting id except in constructor, but currently some code 
	 *    needs to modify this after construction, but before adding as annotation to graph_original_seq
	 */
	private boolean id_locked = false;

	public GraphSym(int[] x, float[] y, String id, BioSeq seq) {
		this(x,null,y,id, seq);
	}

	public GraphSym(int[] x, int[] w, float[] y, String id, BioSeq seq) {
		super();

		this.gid = id;
		this.graph_original_seq = seq;
		
		this.hasWidth = (w != null);

		if (x == null || x.length == 0) {
			xMax = seq.getLength();
		} else {
			setCoords(x, y, w);
		}
		
		SeqSpan span = new SimpleSeqSpan(this.xMin, this.xMax, seq);
		this.addSpan(span);

	}


	public final void lockID() {
		id_locked = true;
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
		if (id_locked) {
			SingletonGenometryModel.getLogger().warning("called GraphSym.setID() while id was locked:  " + this.getID() + " -> " + id);
		}
		else {
			gid = id;
		}
	}

	/**
	 *  Sets the x and y coordinates and indexes.
	 *  @param x an array of int, or null.
	 *  @param y must be an array of float of same length as x.
	 *  @param w must be an array of float of same length as x and y, or null
	 */
	protected void setCoords(int[] x, float[] y, int[] w) {
		if (x.length != y.length) {
			throw new IllegalArgumentException("X-coords and y-coords must have the same length.");
		}
		if (w != null && (x.length != w.length)) {
			throw new IllegalArgumentException("X,W, and Y arrays must have the same length");
		}
		xMin = x[0];
		pointCount = x.length;
		xMax = x[pointCount - 1];
		if (w != null) {
			xMax += w[pointCount - 1];
		}

		bufFile = index(this.getGraphName() + this.getGraphSeq().getID(), x,y,w);
	}

	protected void nullCoords() {
		// null out for garbage collection and cleanup
		yBuf = null;
		wBuf = null;
		if (bufFile != null && bufFile.exists()) {
			try {
				bufFile.delete();
			} catch (Exception ex) {
				// doesn't matter
			}
		}
	}

	public final int getPointCount() {
		return pointCount;
	}

	public final int[] getGraphXCoords() {
		int[] tempCoords = new int[this.pointCount];
		for (int i=0;i<this.pointCount;i++) {
			tempCoords[i] = getGraphXCoord(i);
		}
		return tempCoords;
	}

	public final int getGraphXCoord(int i) {
		if (i >= this.pointCount) {
			return 0;	// out of range
		}
		/*if (i < bufStart || i >= bufStart + BUFSIZE) {
			this.bufStart = i;
			readIntoBuffers(i);
		}
		return (int)(xBuf[i - bufStart] + xDelta);*/
		return (int)(xCoords[i] + xDelta);
	}

	public final int getMinXCoord() {
		return (int)(xMin + xDelta);
	}
	
	public final int getMaxXCoord() {
		return (int)(xMax + xDelta);
	}

	public final void moveX(double delta) {
		this.xDelta += delta;
	}

	/**
	 *  Returns the y coordinate as a String.
	 */
	public String getGraphYCoordString(int i) {
		return Float.toString(getGraphYCoord(i));
	}

	public float getGraphYCoord(int i) {
		if (i >= this.pointCount) {
			return 0;	// out of range
		}
		if (i < bufStart || i >= bufStart + BUFSIZE) {
			this.bufStart = i;
			readIntoBuffers(i);
		}
		return yBuf[i - bufStart];
	}

	public float[] getGraphYCoords() {
		return this.copyGraphYCoords();
	}

	/** Returns a copy of the graph Y coordinates as a float[], even if the Y coordinates
	 *  were originally specified as non-floats.
	 */
	public float[] copyGraphYCoords() {
		float[] tempCoords = new float[this.pointCount];
		for (int i=0;i<this.pointCount;i++) {
			tempCoords[i] = getGraphYCoord(i);
		}
		return tempCoords;
	}

	/**
	 * This is expensive, and should only happen when we're copying the coords.
	 * @return
	 */
	public int[] getGraphWidthCoords() {
		int[] tempCoords = new int[this.pointCount];
		for (int i=0;i<this.pointCount;i++) {
			tempCoords[i] = getGraphWidthCoord(i);
		}
		return tempCoords;
	}
	
	public int getGraphWidthCoord(int i) {
		if (!this.hasWidth) {
			return 0;	// no width coords
		}
		if (i >= this.pointCount) {
			return 0;	// out of range
		}
		if (i < bufStart || i >= bufStart + BUFSIZE) {
			this.bufStart = i;
			readIntoBuffers(i);
		}
		return wBuf[i - bufStart];
	}

	public int getGraphWidthCount() {
		return hasWidth ? this.pointCount : 0;
	}


	/**
	 * Find last point with value <= xmin.
	 * @param xmin
	 * @return
	 */
	public final int determineBegIndex(double xmin) {
		int begIndex = 0;
		// Do quick search through indexed arrays.
		/*for (int i=0;i<xIndex.length;i++) {
			if (xIndex[i] > xmin) {
				break;
			}
			begIndex += BUFSIZE;
		}*/

		for (int i=begIndex;i<this.pointCount;i++) {
			if (this.getGraphXCoord(i) > (int)xmin) {
				return Math.max(0, i-1);
			}
		}
		return 0;
	}

	/**
	 * Find first point with value >= xmax.
	 * Use previous starting index as a starting point.
	 * @param xmax
	 * @return
	 */
	public final int determineEndIndex(double xmax, int prevIndex) {
		int begIndex = 0;
		// Do quick search through indexed arrays.
		/*for (int i=0;i<xIndex.length;i++) {
			if (xIndex[i] >= xmax) {
				break;
			}
			begIndex += BUFSIZE;
		}*/
		//begIndex = Math.max(begIndex, prevIndex);
		
		for (int i=begIndex;i<this.pointCount;i++) {
			if (this.getGraphXCoord(i) >= (int)xmax) {
				return i;
			}
		}
		return this.pointCount-1;
	}


	private File index(String graphName, int[] x, float[] y, int[] w) {
		if (pointCount == 0) {
			return null;	// no need to index.
		}

		// initialize xCoords
		this.xCoords = new int[this.pointCount];
		System.arraycopy(x, 0, this.xCoords, 0, this.pointCount);

		// initialize buffers.
		//xBuf = new int[BUFSIZE];
		yBuf = new float[BUFSIZE];
		//System.arraycopy(x, 0, xBuf, 0, Math.min(BUFSIZE, pointCount));
		Arrays.fill(y, 0, Math.min(BUFSIZE,pointCount)-1, 0f);
		if (this.hasWidth) {
			wBuf = new int[BUFSIZE];
			Arrays.fill(w, 0, Math.min(BUFSIZE,pointCount)-1, 0);
		}
		if (pointCount <= BUFSIZE) {
			// no need to index.  Array is too small.
			return null;
		}

		File bufVal = null;
		DataOutputStream dos = null;
		try {
			//xIndex = new int[this.pointCount / BUFSIZE];
			/*yIndex = new float[this.pointCount / BUFSIZE];
			if (this.hasWidth) {
				wIndex = new int[this.pointCount / BUFSIZE ];
			}*/
			
			// create index arrays for quick searching.
			/*for (int i=0;i<xIndex.length;i++) {
				xIndex[i] = x[i * BUFSIZE];
				yIndex[i] = y[i * BUFSIZE];
				if (this.hasWidth) {
					wIndex[i] = w[i * BUFSIZE];
				}
			}*/

			// create indexed file.
			bufVal = File.createTempFile(graphName, "idx");
			bufVal.deleteOnExit();	// Delete this file when IGB shuts down.
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bufVal)));

			for (int i=0;i<pointCount;i++) {
				dos.writeInt(x[i]);
				dos.writeFloat(y[i]);
				dos.writeInt(this.hasWidth ? w[i] : 1);	// width of 1 is a single point.
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			GeneralUtils.safeClose(dos);
		}
		return bufVal;
	}

	/**
	 * Read into buffers
	 * @param start
	 * @param pointCount
	 * @param bufFile
	 * @param buffer
	 */
	private void readIntoBuffers(int start) {
		DataInputStream dis = null;
		try {
			// open stream
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(bufFile)));

			// skip to proper location
			int bytesToSkip = (start*3*4);	// 3 coords (x,y,w) -- 4 bytes each
			int bytesSkipped = dis.skipBytes(bytesToSkip);
			if (bytesSkipped < bytesToSkip) {
				System.out.println("ERROR: skipped " + bytesSkipped + " out of " + bytesToSkip + " bytes when indexing");
				//Arrays.fill(xBuf, 0);
				Arrays.fill(yBuf, 0.0f);
				if (this.hasWidth) {
					Arrays.fill(wBuf, 0);
				}
				return;
			}

			int maxPoints = Math.min(BUFSIZE, pointCount - start);
			// read in bytes
			for (int i=0;i<maxPoints;i++) {
				//xBuf[i] = dis.readInt();	// x
				dis.readInt();	//x
				yBuf[i] = dis.readFloat();	// y
				int w = dis.readInt();
				if (this.hasWidth) {
					wBuf[i] = w;
				}
			}
			// zero out remainder of buffer, if necessary
			for (int i=maxPoints;i<BUFSIZE;i++) {
				//xBuf[i] = 0;
				yBuf[i] = 0.0f;
				if (this.hasWidth) {
					wBuf[i] = 0;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			//Arrays.fill(xBuf, 0);
			Arrays.fill(yBuf, 0.0f);
			if (this.hasWidth) {
				Arrays.fill(wBuf, 0);
			}
		} finally {
			GeneralUtils.safeClose(dis);
		}
	}


	/**
	 *  Get the seq that the graph's xcoords are specified in
	 */
	public BioSeq getGraphSeq() {
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
		if (key.equals("id")) {
			return this.getID();
		}
		return super.getProperty(key);
	}

	@Override
	public boolean setProperty(String name, Object val) {
		if (name.equals("id")) {
			this.setID(name);
			return false;
		}
		return super.setProperty(name, val);
	}
}
