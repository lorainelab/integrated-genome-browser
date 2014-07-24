package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A SeqSymmetry for holding graph data.
 */
public class GraphSym extends RootSeqSymmetry {

    /**
     * A property that can optionally be set to give a hint about the graph
     * strand for display.
     */
    public static final String PROP_GRAPH_STRAND = "Graph Strand";
    public static final Integer GRAPH_STRAND_PLUS = Integer.valueOf(1);
    public static final Integer GRAPH_STRAND_MINUS = Integer.valueOf(-1);
    public static final Integer GRAPH_STRAND_BOTH = Integer.valueOf(2);

    private int pointCount = 0;	// count of points
    private int xMin = 0;		// min X coord
    private int xMax = 0;		// max X coord

    private float yFirst = 0;		// Y value at 0th coord

    private boolean hasWidth = false;
	// to prevent NPE when xCoords is null - after clear() -
    // use validData to indicate that xCoords would have been null
    private boolean validData = true;

    private final BioSeq graph_original_seq;
    private String gid;

    public static final int BUFSIZE = 100000;	// buffer size
    private int bufStart = 0;	// current buffer start
    //private int xBuf[];
    protected float yBuf[];
    private int wBuf[];
    private File bufFile;

    private int xCoords[];	// too slow to do indexing of x right now

    private double xDelta = 0.0f;	// used by GraphGlyph

    private float min_ycoord = Float.POSITIVE_INFINITY;
    private float max_ycoord = Float.NEGATIVE_INFINITY;

    //To be used for view mode only
    private GraphState gState = null;

    /**
     * id_locked is a temporary fix to allow graph id to be changed after
     * construction, but then lock once lockID() is called. Really want to
     * forbid setting id except in constructor, but currently some code needs to
     * modify this after construction, but before adding as annotation to
     * graph_original_seq
     */
    private boolean id_locked = false;

    public GraphSym(File f, int[] x, float yFirst, float yMin, float yMax, String id, BioSeq seq) {
        super();

        this.gid = id;
        this.graph_original_seq = seq;
        this.hasWidth = true;

        this.xMin = x[0];
        this.xMax = x[x.length - 1];

        this.yFirst = yFirst;
        pointCount = x.length;

        this.min_ycoord = yMin;
        this.max_ycoord = yMax;

        bufFile = f;

        // initialize xCoords
        xCoords = new int[this.pointCount];
        validData = true;
        yBuf = new float[BUFSIZE];
        wBuf = new int[BUFSIZE];
        System.arraycopy(x, 0, this.xCoords, 0, this.pointCount);

        SeqSpan span = new SimpleSeqSpan(this.xMin, this.xMax, seq);
        this.addSpan(span);
    }

    public GraphSym(int[] x, float[] y, String id, BioSeq seq) {
        this(x, null, y, id, seq);
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

//		SeqSpan span = new SimpleSeqSpan(this.xMin, this.xMax, seq);
        // HV : 26/09/13
        // Do not use minimum and maximum values from graph. It cause bug
        // on alt splice view. Moreover graph should be of full length as seq.
        SeqSpan span = new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq);
        this.addSpan(span);

    }

    public final void lockID() {
        id_locked = true;
    }

    public final void setGraphName(String name) {
        getGraphState().getTierStyle().setTrackName(name);
        setProperty("name", name);
    }

    public final String getGraphName() {
        String gname = getGraphState().getTierStyle().getTrackName();
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
     * Not allowed to call GraphSym.setID(), id
     */
    @Override
    public void setID(String id) {
        if (id_locked) {
            Logger.getLogger(GraphSym.class.getName()).log(
                    Level.WARNING, "called GraphSym.setID() while id was locked:  {0} -> {1}", new Object[]{this.getID(), id});
        } else {
            gid = id;
        }
    }

    /**
     * Sets the coordinates and indexes.
     *
     * @param y Must be of same length as <var>x</var>.
     * @param w If not null, <var>w</var> must be of same length as <var>x</var>
     * and <var>y</var>.
     */
    protected final synchronized void setCoords(int[] x, float[] y, int[] w) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("X-coords and y-coords must have the same length.");
        }
        if (w != null && (x.length != w.length)) {
            throw new IllegalArgumentException("X,W, and Y arrays must have the same length");
        }
        xMin = x[0];
        yFirst = y[0];
        pointCount = x.length;
        xMax = x[pointCount - 1];
        if (w != null) {
            xMax += w[pointCount - 1];
        }

        this.hasWidth = (w != null);

        setVisibleYRange(y);

        bufFile = index(x, y, w);
    }

    private synchronized void setVisibleYRange(float[] y) {
        min_ycoord = Float.POSITIVE_INFINITY;
        max_ycoord = Float.NEGATIVE_INFINITY;
        for (float f : y) {
            if (f < min_ycoord) {
                min_ycoord = f;
            }
            if (f > max_ycoord) {
                max_ycoord = f;
            }
        }
    }

    protected final synchronized void nullCoords() {
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

    public final synchronized int[] getGraphXCoords() {
        int[] tempCoords = new int[this.pointCount];
        for (int i = 0; i < this.pointCount; i++) {
            tempCoords[i] = getGraphXCoord(i);
        }
        return tempCoords;
    }

    public int getGraphXCoord(int i) {
        if (i >= this.pointCount) {
            return 0;	// out of range
        }
        /*if (i < bufStart || i >= bufStart + BUFSIZE) {
         this.bufStart = i;
         readIntoBuffers(i);
         }
         return (int)(xBuf[i - bufStart] + xDelta);*/
        return (int) (xCoords[i] + xDelta);
    }

    public final int getMinXCoord() {
        return (int) (xMin + xDelta);
    }

    public final int getMaxXCoord() {
        return (int) (xMax + xDelta);
    }

    public final void moveX(double delta) {
        this.xDelta += delta;
    }

    public final float getFirstYCoord() {
        return yFirst;
    }

    /**
     * Returns the y coordinate as a String.
     */
    public final String getGraphYCoordString(int i) {
        return Float.toString(getGraphYCoord(i));
    }

    public float getGraphYCoord(int i) {
        if (i >= this.pointCount) {
            return 0;	// out of range
        }
        if (i == 0) {
            return getFirstYCoord();
        }
        if (i < bufStart || i >= bufStart + BUFSIZE) {
            readIntoBuffers(i);
        }
        return yBuf[i - bufStart];
    }

    public final float[] getGraphYCoords() {
        return this.copyGraphYCoords();
    }

    /**
     * Returns a copy of the graph Y coordinates as a float[], even if the Y
     * coordinates were originally specified as non-floats.
     */
    public synchronized float[] copyGraphYCoords() {
        float[] tempCoords = new float[this.pointCount];
        for (int i = 0; i < this.pointCount; i++) {
            tempCoords[i] = getGraphYCoord(i);
        }
        return tempCoords;
    }

    /**
     * @return a "normalized" copy of the Y coords, remove consecutive
     * duplicates
     */
    public synchronized float[] normalizeGraphYCoords() {
        return copyGraphYCoords();
    }

    public float[] getVisibleYRange() {
        float[] result = new float[2];
        result[0] = min_ycoord;
        result[1] = max_ycoord;
        return result;
    }

    /**
     * This is expensive, and should only happen when we're copying the coords.
     *
     * @return tempCoords
     */
    public final synchronized int[] getGraphWidthCoords() {
        if (!this.hasWidth) {
            return null;
        }
        int[] tempCoords = new int[this.pointCount];
        for (int i = 0; i < this.pointCount; i++) {
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
            readIntoBuffers(i);
        }
        if (wBuf == null) {
            return 0;
        }
        return wBuf[i - bufStart];
    }

    public boolean hasWidth() {
        return hasWidth;
    }

    public boolean isValid() {
        return validData;
    }

    /**
     * Find last point with value <= xmin.
	 * @pa
     *
     * ram xmin
     * @return 0
     */
    public final int determineBegIndex(double xmin) {
        int index = Arrays.binarySearch(xCoords, (int) Math.floor(xmin));
        if (index >= 0) {
            return index;
        }
		// negative, which means it's (-(first elt > key) - 1).
        // Thus first elt <= key = (-index - 1) -1 = (-index -2)
        return Math.max(0, (-index - 2));

        /* The below code should be used if we need to use accessors on GraphXCoord (for example, if we start using the buffer again).
         int begIndex = 0;
         for (int i=begIndex;i<this.pointCount;i++) {
         if (this.getGraphXCoord(i) > (int)xmin) {
         return Math.max(0, i-1);
         }
         }*/
    }

    /**
     * Find first point with value >= xmax. Use previous starting index as a
     * starting point.
     *
     * @param xmax
     * @param prevIndex
     * @return pointCount-1
     */
    public final int determineEndIndex(double xmax, int prevIndex) {
        int index = Arrays.binarySearch(xCoords, (int) Math.ceil(xmax));
        if (index >= 0) {
            return index;
        }
		// negative, which means it's (-(first elt > key) - 1).
        // We want that first elt.
        index = -index - 1;

        // need to be sure that this doesn't go beyond the end of the array, if all points are less than xmax
        index = Math.min(index, this.pointCount - 1);

        // need to be sure it's not less than 0
        index = Math.max(0, index);

        return index;

        /* The below code should be used if we need to use accessors on GraphXCoord (for example, if we start using the buffer again).
         for (int i=prevIndex;i<this.pointCount;i++) {
         if (this.getGraphXCoord(i) >= (int)xmax) {
         return i;
         }
         }
         return this.pointCount-1;
         */
    }

    /**
     * Determine the y coordinate, given x.
     *
     * @param x
     * @return y coord. -1 indicates not found.
     */
    protected float getYCoordFromX(int x) {
        int leftBound = this.determineBegIndex(x);
        if (this.getGraphXCoord(leftBound) == x || (this.hasWidth && this.getGraphXCoord(leftBound) + this.getGraphWidthCoord(leftBound) >= x)) {
            // Right on the point or in a region bound by its width
            return this.getGraphYCoord(leftBound);
        }
        // Couldn't find point
        return -1f;
    }

    private File index(int[] x, float[] y, int[] w) {
        if (pointCount == 0) {
            return null;	// no need to index.
        }

        // initialize xCoords
        this.xCoords = new int[this.pointCount];
        validData = true;
        System.arraycopy(x, 0, this.xCoords, 0, this.pointCount);

		// initialize buffers.
        //xBuf = new int[BUFSIZE];
        yBuf = new float[BUFSIZE];
        //System.arraycopy(x, 0, xBuf, 0, Math.min(BUFSIZE, pointCount));
        System.arraycopy(y, 0, yBuf, 0, Math.min(BUFSIZE, pointCount));
        if (this.hasWidth) {
            wBuf = new int[BUFSIZE];
            System.arraycopy(w, 0, wBuf, 0, Math.min(BUFSIZE, pointCount));
        }
        if (pointCount <= BUFSIZE) {
            // no need to index.  Array is too small.
            return null;
        }
        return createIndexedFile(this.pointCount, x, y, w);
    }

//	private String cleanFileName(String fileName) {
//		return fileName.replaceAll("\\(", "_").replaceAll("\\)", "_").replaceAll("\\*", "_").replaceAll("\\.", "_");
//	}
    /**
     * Read into buffers
     *
     * @param start
     */
    protected synchronized void readIntoBuffers(int start) {
        DataInputStream dis = null;
        try {
            // open stream
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(bufFile)));

            this.bufStart = start;

            // skip to proper location
            int bytesToSkip = (start * 3 * 4);	// 3 coords (x,y,w) -- 4 bytes each
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
            for (int i = 0; i < maxPoints; i++) {
                //xBuf[i] = dis.readInt();	// x
                dis.readInt();	//x
                yBuf[i] = dis.readFloat();	// y
                int w = dis.readInt();
                if (this.hasWidth) {
                    wBuf[i] = w;
                }
            }
            // zero out remainder of buffer, if necessary
            Arrays.fill(yBuf, maxPoints, BUFSIZE, 0.0f);
            if (this.hasWidth) {
                Arrays.fill(wBuf, maxPoints, BUFSIZE, 0);
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

    protected int getBufStart() {
        return bufStart;
    }

    /**
     * Get the seq that the graph's xcoords are specified in
     */
    public final BioSeq getGraphSeq() {
        return graph_original_seq;
    }

    /**
     * Returns the graph state. Will never be null.
     */
    public final GraphState getGraphState() {
        if (gState != null) {
            return this.gState;
        }

        return DefaultStateProvider.getGlobalStateProvider().getGraphState(this.gid);
    }

    //To be used by view mode only.
    public final void setGraphState(GraphState gState) {
        this.gState = gState;
    }

    /**
     * Overriding request for property "method" to return graph name.
     */
    @Override
    public Object getProperty(String key) {
//		if (key.equals("method")) {
//			return getGraphName();
//		}
        if (key.equals("id") || key.equals("method")) {
            return this.getID();
        }
        return super.getProperty(key);
    }

    @Override
    public boolean setProperty(String name, Object val) {
        if (name.equals("id") && val != null) {
            this.setID(val.toString());
            return false;
        }
        return super.setProperty(name, val);
    }

    public Map<String, Object> getLocationProperties(int x, SeqSpan span) {
        Map<String, Object> locprops = new HashMap<String, Object>();

        locprops.put("x coord", x);
        if (isValid()) {
            float y = getYCoordFromX(x);
            if (y < 0) {
                locprops.put("y coord", "no point");
            } else {
                locprops.put("y coord", y);
            }
        }
        return locprops;
    }

    @Override
    public void clear() {
        super.clear();
        validData = false;
        yBuf = new float[]{};//null; to avoid NPE
        wBuf = null;
        xCoords = new int[]{};//null; to avoid NPE
        bufFile = null;
        pointCount = 0;
    }

    /**
     * @return if this is a special graph that should be handled separately (not
     * combined into a composite graph)
     */
    public boolean isSpecialGraph() {
        return false;
    }

    @Override
    public FileTypeCategory getCategory() {
        return FileTypeCategory.Graph;
    }

    @Override
    public void search(Set<SeqSymmetry> results, String id) {
    }

    @Override
    public void searchHints(Set<String> results, Pattern regex, int limit) {
    }

    @Override
    public void search(Set<SeqSymmetry> result, Pattern regex, int limit) {
    }

    @Override
    public void searchProperties(Set<SeqSymmetry> results, Pattern regex, int limit) {
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        if (!isValid()) {
            return result;
        }
//		DO NOT USE bufStart
        result = prime * result + ((gid == null) ? 0 : gid.hashCode());
        result = prime
                * result
                + ((graph_original_seq == null) ? 0 : graph_original_seq
                .hashCode());
        result = prime * result + (hasWidth ? 1231 : 1237);
        result = prime * result + Float.floatToIntBits(max_ycoord);
        result = prime * result + Float.floatToIntBits(min_ycoord);
        result = prime * result + pointCount;
        result = prime * result + Arrays.hashCode(wBuf);
        result = prime * result + Arrays.hashCode(xCoords);
        long temp;
        temp = Double.doubleToLongBits(xDelta);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + xMax;
        result = prime * result + xMin;
//		DO NOT USE yBuf
        result = prime * result + Float.floatToIntBits(yFirst);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GraphSym other = (GraphSym) obj;
        if (!isValid() || !other.isValid()) {
            return false;
        }
//		DO NOT USE bufStart
        if (gid == null) {
            if (other.gid != null) {
                return false;
            }
        } else if (!gid.equals(other.gid)) {
            return false;
        }
        if (graph_original_seq == null) {
            if (other.graph_original_seq != null) {
                return false;
            }
        } else if (!graph_original_seq.equals(other.graph_original_seq)) {
            return false;
        }
        if (hasWidth != other.hasWidth) {
            return false;
        }
        if (Float.floatToIntBits(max_ycoord) != Float
                .floatToIntBits(other.max_ycoord)) {
            return false;
        }
        if (Float.floatToIntBits(min_ycoord) != Float
                .floatToIntBits(other.min_ycoord)) {
            return false;
        }
        if (pointCount != other.pointCount) {
            return false;
        }
        if (!Arrays.equals(wBuf, other.wBuf)) {
            return false;
        }
        if (!Arrays.equals(xCoords, other.xCoords)) {
            return false;
        }
        if (Double.doubleToLongBits(xDelta) != Double
                .doubleToLongBits(other.xDelta)) {
            return false;
        }
        if (xMax != other.xMax) {
            return false;
        }
        if (xMin != other.xMin) {
            return false;
        }
//		DO NOT USE yBuf
        if (Float.floatToIntBits(yFirst) != Float.floatToIntBits(other.yFirst)) {
            return false;
        }
        return true;
    }

    /**
     * Index a graph.
     */
    private static File createIndexedFile(int pointCount, int[] x, float[] y, int[] w) {
        File bufVal = null;
        DataOutputStream dos = null;
        try {
			// create indexed file.

			//if (graphName.length() < 3) {
            //graphName += "___";
            // fix for Java error with short names
            //}
            bufVal = File.createTempFile((Math.random() + "").substring(2), "idx");
			//cannot use the graph name since this is sometimes too long and throws a IOException
            //bufVal = File.createTempFile(URLEncoder.encode(graphName, "UTF-8"), "idx");
            bufVal.deleteOnExit(); // Delete this file when shutting down.
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bufVal)));
            for (int i = 0; i < pointCount; i++) {
                dos.writeInt(x[i]);
                dos.writeFloat(y[i]);
                dos.writeInt(w == null ? 1 : w[i]); // width of 1 is a single point.
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            GeneralUtils.safeClose(dos);
        }
        return bufVal;
    }

}
