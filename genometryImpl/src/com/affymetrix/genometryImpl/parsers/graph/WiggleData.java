package com.affymetrix.genometryImpl.parsers.graph;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *  A class used to temporarily hold data during processing of Wiggle-format files.
 */
final class WiggleData {
	private IntList xList;
	private FloatList yList;
	private IntList wList;
	private final String seq_id;

	WiggleData(String seq_id) {

		this.xList = new IntList();
		this.yList = new FloatList();
		this.wList = new IntList();
		this.seq_id = seq_id;
	}

	/**
	 *  Creates a GraphSym from the stored data, or returns null if no data
	 *  has been stored yet.
	 */
	GraphSym createGraph(AnnotatedSeqGroup seq_group, String graph_id) {
		if (xList.isEmpty()) {
			return null;
		}

		int largest_x = xList.get(xList.size() - 1);
		int largest_w = wList.get(wList.size() - 1);

		BioSeq seq = seq_group.addSeq(seq_id, largest_x + largest_w);

		int[] xArr = xList.copyToArray();
		xList = null;
		float[] yArr = yList.copyToArray();
		yList = null;
		int[] wArr = wList.copyToArray();
		wList = null;

		return new GraphIntervalSym(xArr, wArr, yArr, graph_id, seq);
	}

	void add(int x, float y, int w) {
		// see if this point obeys sorting
		if (xList.isEmpty() || x > xList.get(xList.size()-1)) {
			xList.add(x);
			yList.add(y);
			wList.add(w);
			return;
		}

		// point does not obey sorting.  Must add the point at the proper location
		System.out.println("WARNING: x coordinate " + x + " is not sorted.  This violates Wiggle specification and will be very slow to load.");

		int index = xList.binarySearch(x);
		if (index >= 0) {
			System.out.println("WARNING: x coordinate " + x + " is repeated, which is invalid.  Ignoring data point.");
			return;
		}
		int insertionPoint = -index-1;	// from binarySearch definition

		// see if previous interval covers this
		int previousInterval = xList.get(insertionPoint) + wList.get(insertionPoint);
		if (previousInterval > x) {
			System.out.println("WARNING: x coordinate " + x + " is in a previous interval, which is invalid.  Ignoring data point.");
			return;
		}
		xList.add(insertionPoint,x);
		yList.add(insertionPoint,y);
		wList.add(insertionPoint,w);
	}
}
