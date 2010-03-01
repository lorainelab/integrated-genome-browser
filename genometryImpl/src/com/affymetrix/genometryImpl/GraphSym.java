package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  A SeqSymmetry for holding graph data.
 */
public class GraphSym extends SimpleSymWithProps {

	/** A property that can optionally be set to give a hint about the graph strand for display. */
	public static final String PROP_GRAPH_STRAND = "Graph Strand";
	public static final Integer GRAPH_STRAND_PLUS = Integer.valueOf(1);
	public static final Integer GRAPH_STRAND_MINUS = Integer.valueOf(-1);
	public static final Integer GRAPH_STRAND_BOTH = Integer.valueOf(2);
	
	private int pointCount = 0;	// count of points
	private int xMin = 0;		// min X coord
	private int xMax = 0;		// max X coord

	private boolean hasWidth = false;

	private final BioSeq graph_original_seq;
	private String gid;

	private static final int BUFSIZE = 100000;	// buffer size
	private int bufStart = -1;	// current buffer start (defaults to -1 to force initialization)
	//private int xBuf[];
	private float yBuf[];
	private int wBuf[];
	private File bufFile;

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

	/**
	 * This initializer is used to save space.  All arrays are already saved in the file.
	 * @param f
	 * @param x
	 * @param len
	 * @param id
	 * @param seq
	 */
	public GraphSym(File f, int len, String id, BioSeq seq) {
		super();

		this.gid = id;
		this.graph_original_seq = seq;

		this.bufFile = f;

		this.hasWidth = true;

		this.pointCount = len;

		// Use given x array to save space.
		this.xCoords = new int[pointCount];
		DataInputStream dis = null;
		try {
			// open stream
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(bufFile)));

			for (int i=0;i<pointCount;i++) {
				this.xCoords[i] = dis.readInt();
				dis.readFloat();	// y
				dis.readInt();		// w
			}
			xMin = xCoords[0];
			xMax = xCoords[pointCount - 1];

			this.bufStart = 0;
			this.readIntoBuffers(0);	// initialize buffers

			SeqSpan span = new SimpleSeqSpan(this.xMin, this.xMax, seq);
			this.addSpan(span);
		} catch (Exception ex) {
			Logger.getLogger(GraphSym.class.getName()).log(Level.SEVERE, null, ex);
			return;
		} finally {
			GeneralUtils.safeClose(dis);
		}


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
			GenometryModel.getLogger().warning("called GraphSym.setID() while id was locked:  " + this.getID() + " -> " + id);
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
	protected final void setCoords(int[] x, float[] y, int[] w) {
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

	protected final void nullCoords() {
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
	public final String getGraphYCoordString(int i) {
		return Float.toString(getGraphYCoord(i));
	}

	public final float getGraphYCoord(int i) {
		if (i >= this.pointCount) {
			return 0;	// out of range
		}
		readFromBufferIfNecessary(i);
		return yBuf[i - bufStart];
	}

	public final float[] getGraphYCoords() {
		return this.copyGraphYCoords();
	}

	/** Returns a copy of the graph Y coordinates as a float[], even if the Y coordinates
	 *  were originally specified as non-floats.
	 */
	public final float[] copyGraphYCoords() {
		float[] tempCoords = new float[this.pointCount];
		for (int i=0;i<this.pointCount;i++) {
			tempCoords[i] = getGraphYCoord(i);
		}
		return tempCoords;
	}

	public final float[] getVisibleYRange() {
		float[] result = new float[2];
		float min_ycoord = Float.POSITIVE_INFINITY;
		float max_ycoord = Float.NEGATIVE_INFINITY;

		for (int i = 0; i < pointCount; i++) {
			float f = this.getGraphYCoord(i);
			if (f < min_ycoord) {
				min_ycoord = f;
			}
			if (f > max_ycoord) {
				max_ycoord = f;
			}
		}
		result[0] = min_ycoord;
		result[1] = max_ycoord;
		return result;
	}

	/**
	 * This is expensive, and should only happen when we're copying the coords.
	 * @return tempCoords
	 */
	public final int[] getGraphWidthCoords() {
		int[] tempCoords = new int[this.pointCount];
		for (int i=0;i<this.pointCount;i++) {
			tempCoords[i] = getGraphWidthCoord(i);
		}
		return tempCoords;
	}
	
	public final int getGraphWidthCoord(int i) {
		if (!this.hasWidth) {
			return 0;	// no width coords
		}
		if (i >= this.pointCount) {
			return 0;	// out of range
		}
		readFromBufferIfNecessary(i);
		return wBuf[i - bufStart];
	}

	private void readFromBufferIfNecessary(int i) {
		if (bufStart == -1 || i < bufStart || i >= bufStart + BUFSIZE) {
			this.bufStart = i;
			readIntoBuffers(i);
		}
	}

	public final boolean hasWidth() {
		return hasWidth;
	}


	/**
	 * Find last point with value <= xmin.
	 * @param xmin
	 * @return 0
	 */
	public final int determineBegIndex(double xmin) {
		int begIndex = 0;
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
	 * @param prevIndex
	 * @return pointCount-1
	 */
	public final int determineEndIndex(double xmax, int prevIndex) {
		int begIndex = 0;
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
		//System.arraycopy(x, 0, xBuf, 0, Math.min(BUFSIZE, pointCount));

		return IndexingUtils.createIndexedFile(graphName, this.pointCount, x, y, w);
	}

	
	/**
	 * Read into buffers
	 * @param start
	 */
	private void readIntoBuffers(int start) {
		if (yBuf == null) {
			yBuf = new float[BUFSIZE];
			if (this.hasWidth) {
				wBuf = new int[BUFSIZE];
			}
		}
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
	public final BioSeq getGraphSeq() {
		return graph_original_seq;
	}

	/**
	 *  Returns the graph state.  Will never be null.
	 */
	public final GraphState getGraphState() {
		return DefaultStateProvider.getGlobalStateProvider().getGraphState(this.gid);
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
