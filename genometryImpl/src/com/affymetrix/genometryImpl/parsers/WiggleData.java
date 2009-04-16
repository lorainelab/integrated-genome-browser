package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *  A class used to temporarily hold data during processing of Wiggle-format files.
 */
final class WiggleData {
	private final ArrayList<Point3D> data;
	private final AnnotatedSeqGroup seq_group;
	private final String seq_id;

	WiggleData(AnnotatedSeqGroup group, String seq_id) {
		this.data = new ArrayList<Point3D>();
		this.seq_id = seq_id;
		this.seq_group = group;
	}

	/**
	 *  Creates a GraphSym from the stored data, or returns null if no data
	 *  has been stored yet.
	 */
	GraphSymFloat createGraph(String graph_id) {
		if (data.isEmpty()) {
			return null;
		}

		PointComp pointcomp = new PointComp();
		Collections.sort(data, pointcomp);

		int dataSize = data.size();

		int[]   xlist = new int[dataSize];
		float[] ylist = new float[dataSize];
		int[]   wlist = new int[dataSize];

		Point3D largest = data.get(dataSize - 1);
		int largest_x = largest.x + largest.w;

		SmartAnnotBioSeq seq = seq_group.addSeq(seq_id, largest_x);

		for (int i=0; i<dataSize; i++) {
			Point3D p = data.get(i);
			xlist[i] = p.x;
			ylist[i] = p.y;
			wlist[i] = p.w;
		}

		return new GraphIntervalSym(xlist, wlist, ylist, graph_id, seq);
	}

	public void add(int x, float y, int w) {
		if (!data.isEmpty()) {
			// See if we can merge intervals with same Y coordinate.
			// This is done to save space in redundant wiggle files.
			Point3D lastP = data.get(data.size()-1);
			if (y == lastP.y && x >= lastP.x && x <= (lastP.x + lastP.w)) {
				lastP.w = x - lastP.x + w;
				return;
			}
		}
		data.add(new Point3D(x, y, w));
	}

	private static final class Point3D {
		private final float y;
		private final int x;
		private int w;
		public Point3D(int x, float y,int w) {
			this.x = x; this.y =y; this.w = w;
		}
	}

	private static final class PointComp implements Comparator<Point3D> {
		public int compare(Point3D p1, Point3D p2) {
			return ((Integer)p1.x).compareTo(p2.x);
		}
	}
}