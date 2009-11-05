package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
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
 *  A SeqSymmetry for holding graph for graphs that have y values that apply to
 *  intervals.  So instead of (x,y) there is (x_start, x_width, y).
 */
public final class GraphIntervalSym extends GraphSym {
	private static final int BUFSIZE = 1000;	// buffer size
	private int bufStart = 0;	// current buffer start
	private int xBuf[] = new int[BUFSIZE];
	private float yBuf[] = new float[BUFSIZE];
	private int wBuf[] = new int[BUFSIZE];
	private File bufFile = null;

	public GraphIntervalSym(int[] x, int[] width, float[] y, String id, BioSeq seq) {
		super(x,y,id,seq);

		if (x.length != y.length || x.length != width.length) {
			throw new IllegalArgumentException("X,W, and Y arrays must have the same length");
		}

		this.removeSpans();
		int xmin = x[0];
		int xmax = x[x.length-1] + width[x.length-1];
		this.addSpan(new SimpleSeqSpan(xmin, xmax, seq));
		bufFile = index(this.getGraphName() + this.getGraphSeq().getID(),
				width,
				xBuf, yBuf, wBuf);
	}

	/**
	 * This is expensive, and should only happen when we're copying the coords.
	 * @return
	 */
	public int[] getGraphWidthCoords() {
		int[] tempCoords = new int[this.getPointCount()];
		for (int i=0;i<tempCoords.length;i++) {
			tempCoords[i] = getGraphWidthCoord(i);
		}
		return tempCoords;
	}

	public int getGraphWidthCoord(int i) {
		if (i >= this.getPointCount()) {
			return 0;	// out of range
		}
		if (i < bufStart || i >= bufStart + BUFSIZE) {
			this.bufStart = i;
			readIntoBuffers(i, this.getPointCount(), this.bufFile, this.xBuf, this.yBuf, this.wBuf);
		}
		return wBuf[i - bufStart];
	}

	public int getGraphWidthCount() {
		return this.getPointCount();	// same as width
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
				this.getGraphSeq(),
				getGraphYCoord(index));
	}

	private File index(String graphName, int[] width, int[] xBuf, float[] yBuf, int[] wBuf) {
		int pointCount = this.getPointCount();
		
		// initialize buffer.
		System.arraycopy(width, 0, wBuf, 0, Math.min(BUFSIZE, pointCount));
		if (pointCount <= BUFSIZE) {
			// no need to index.  Array is too small.
			return null;
		}

		File bufVal = null;
		DataOutputStream dos = null;
		try {
			// create indexed file.
			bufVal = File.createTempFile(graphName, "idx");
			bufVal.deleteOnExit();	// Delete this file when IGB shuts down.
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bufVal)));
			
			for (int i=0;i<pointCount;i++) {
				dos.writeInt(this.getGraphXCoord(i));
				dos.writeFloat(this.getGraphYCoord(i));
				dos.writeInt(width[i]);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			GeneralUtils.safeClose(dos);
			return bufVal;
		}
	}

	/**
	 * Read into buffers
	 * @param start
	 * @param pointCount
	 * @param bufFile
	 * @param buffer
	 */
	private static void readIntoBuffers(int start, int pointCount, File bufFile, int[] xBuf, float[] yBuf, int[] wBuf) {
		DataInputStream dis = null;
		try {
			// open stream
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(bufFile)));

			// skip to proper location
			int bytesToSkip = (start*3*4);	// 3 coords (x,y,w) -- 4 bytes each
			int bytesSkipped = dis.skipBytes(bytesToSkip);
			if (bytesSkipped < bytesToSkip) {
				System.out.println("ERROR: skipped " + bytesSkipped + " out of " + bytesToSkip + " bytes when indexing");
				Arrays.fill(xBuf, 0);
				Arrays.fill(yBuf, 0.0f);
				Arrays.fill(wBuf, 0);
				return;
			}

			int maxPoints = Math.min(BUFSIZE, pointCount - start);
			// read in bytes
			for (int i=0;i<maxPoints;i++) {
				xBuf[i] = dis.readInt();	// x
				yBuf[i] = dis.readFloat();	// y
				wBuf[i] = dis.readInt();
			}
			// zero out remainder of buffer, if necessary
			for (int i=maxPoints;i<BUFSIZE;i++) {
				xBuf[i] = 0;
				yBuf[i] = 0.0f;
				wBuf[i] = 0;
			}
		} catch (Exception ex) {
			Arrays.fill(xBuf, 0);
			Arrays.fill(yBuf, 0.0f);
			Arrays.fill(wBuf, 0);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dis);
		}
	}

}
