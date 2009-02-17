package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 *  A class used to temporarily hold data during processing of Wiggle-format files.
 */
final class WiggleData {
	IntList xlist;
	IntList wlist;
	FloatList ylist;

	private final AnnotatedSeqGroup seq_group;
	private final String seq_id;

	WiggleData(AnnotatedSeqGroup group, String seq_id) {
		this.seq_id = seq_id;
		this.seq_group = group;
		xlist = new IntList();
		wlist = new IntList();
		ylist = new FloatList();
	}

	/**
	 *  Creates a GraphSym from the stored data, or returns null if no data
	 *  has been stored yet.
	 */
	GraphSymFloat createGraph(String graph_id) {
		if (xlist.size() == 0 || ylist.size() == 0 || wlist.size() == 0) {
			return null;
		}

		int largest_x = xlist.get(xlist.size()-1) + wlist.get(wlist.size()-1);

		BioSeq seq = seq_group.addSeq(seq_id, largest_x);

		sortData(xlist.size(), xlist.getInternalArray(), wlist.getInternalArray(), ylist.getInternalArray());

		GraphSymFloat gsym = new GraphIntervalSym(xlist.copyToArray(), wlist.copyToArray(), ylist.copyToArray(), graph_id, seq);

		return gsym;
	}

	private void sortData(int graph_length, int[] xcoords, int[] wcoords, float ycoords[]) {
		List<Point3D> points = convertCoordsToPoints(graph_length, wcoords, xcoords, ycoords);
		PointComp pointcomp = new PointComp();
		Collections.sort(points, pointcomp);

		for (int i=0; i<graph_length; i++) {
			Point3D pnt = points.get(i);
			xcoords[i] = pnt.x;
			ycoords[i] = pnt.y;
			wcoords[i] = pnt.w;
		}
	}

	private static List<Point3D> convertCoordsToPoints(int graph_length, int[] wcoords, int[] xcoords, float[] ycoords) {
		List<Point3D> points = new ArrayList<Point3D>(graph_length);
		if (wcoords != null) {
			for (int i = 0; i < graph_length; i++) {
				Point3D pnt = new Point3D(xcoords[i], ycoords[i], wcoords[i]);
				points.add(pnt);
			}
		} else {
			for (int i = 0; i < graph_length; i++) {
				Point3D pnt = new Point3D(xcoords[i], ycoords[i], 1);
				points.add(pnt);
			}
		}
		return points;
	}

	private static final class Point3D {
		private final float y;
		private final int x, w;
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
