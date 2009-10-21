package com.affymetrix.genometryImpl;

import java.util.*;

/**
 *   CompositeGraphSym.
 *   Was originally envisioning that a CompositeGraphSym would have a set of GraphSym children.
 *   But, this causes lots of problems for calculations that need to cross transitions between children,
 *      for example percentile binning or dynamic thresholding.
 *   So, new plan is to keep composite graph x and y coords each in single array, and every time
 *      more coords are added make new array and populate with old and new coords via System.arraycopy().
 *      BUT, also have child syms of CompositeGraphSym that keep track of what slices coords have already
 *      been populated from.
 */
public final class CompositeGraphSym extends GraphSymFloat {

	public CompositeGraphSym(String id, MutableAnnotatedBioSeq seq) {
		super(null, null, id, seq);
	}

	/**
	 *  Overriding addChild() to only accept GraphSymFloat children,
	 *     integrates x and y coord arrays of child into composite's coord arrays
	 *     (and nulls old ones out for gc).
	 *  Assumes that slices can abut but do _not_ overlap
	 *    "abut" in this case means that (sliceA.span.max == sliceB.span.min)
	 *    since these are half-open half-closed intervals, this is not actually overlap but abutment...
	 *
	 */
	public void addChild(SeqSymmetry sym) {
		// System.out.println("called CompositeGraphSym.addChild(): " + sym);
		if (sym instanceof GraphSym) {
			GraphSymFloat slice = (GraphSymFloat) sym;
			int[] slice_xcoords = slice.getGraphXCoords();
			float[] slice_ycoords;
			if (sym instanceof GraphSymFloat) {
				slice_ycoords = slice.getGraphYCoords();
			} else {
				slice_ycoords = slice.copyGraphYCoords();
			}

			if (this.getPointCount() == 0 && getGraphYCoords() == null) { // first GraphSym child, so just set xcoords and ycoords
				setCoords(slice_xcoords, slice_ycoords);
				slice.setCoords(null, null);
			} else {
				// if no data points in slice, then just keep old coords
				if (slice.getPointCount() > 0) {
					// use binary search to figure out what index "A" that slice_xcoords array should insert
					//    into existing xcoord array
					int slice_min = slice.getGraphXCoord(0);
					int slice_index = Arrays.binarySearch(xcoords, slice_min);
					if (slice_index < 0) {
						// want draw_beg_index to be index of max xcoord <= view_start
						//  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
						slice_index = (-slice_index - 1);
					}

					int[] new_xcoords = new int[this.getPointCount() + slice.getPointCount()];
					int new_index = 0;
					// since slices cannot overlap, new xcoord array should be:

					//    old xcoord array entries up to "A-1"
					if (slice_index > 0) {
						System.arraycopy(xcoords, 0, new_xcoords, new_index, slice_index);
						new_index += slice_index;
					}
					//    all of slice_xcoords entries
					System.arraycopy(slice_xcoords, 0, new_xcoords, new_index, slice.getPointCount());
					new_index += slice.getPointCount();
					//    old xcoord array entries from "A" to end of old xcoord array
					if (slice_index < this.getPointCount()) {
						System.arraycopy(xcoords, slice_index, new_xcoords, new_index, this.getPointCount() - slice_index);
					}

					// get rid of old xcoords
					xcoords = new_xcoords;
					slice_xcoords = null;
					slice.xcoords = null;
					float[] new_ycoords = new float[getGraphYCoords().length + slice_ycoords.length];
					new_index = 0;

					//    old ycoord array entries up to "A-1"
					if (slice_index > 0) {
						System.arraycopy(getGraphYCoords(), 0, new_ycoords, new_index, slice_index);
						new_index += slice_index;
					}
					//    all of slice_ycoords entries
					System.arraycopy(slice_ycoords, 0, new_ycoords, new_index, slice_ycoords.length);
					new_index += slice_ycoords.length;
					//    old ycoord array entries from "A" to end of old ycoord array
					if (slice_index < getGraphYCoords().length) {
						System.arraycopy(getGraphYCoords(), slice_index, new_ycoords, new_index, getGraphYCoords().length - slice_index);
					}
					setCoords(xcoords, new_ycoords);
					slice.setCoords(null, null);
				}
				// also need to recalculate point_min_ycoord and point_max_ycoord
				//   but already know these for previous coords, so just iterate through slice coords to update
				// actually these are only in the graph glyphs -- but might want to move them to graph sym
				//   to improve recalculation performance
			}
			// assuming GraphSym seq span is bounds of graph slice, add GraphSym as child
			// but remember coords are nulled out!
			// NOTE: the slice coordinates do not HAVE to be set to null, they could
			// be set to some sub-array of the composite graph arrays backed by the same data.
			super.addChild(slice);
		} else {
			throw new RuntimeException("only GraphSyms can be added as children to CompositeGraphSym!");
		}
	}

}
