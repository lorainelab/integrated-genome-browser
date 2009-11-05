package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.util.GraphSymUtils;

public final class CompositeGraphSym extends GraphSym {

	public CompositeGraphSym(String id, BioSeq seq) {
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
	@Override
	public void addChild(SeqSymmetry sym) {
		if (!(sym instanceof GraphSym)) {
			throw new RuntimeException("only GraphSyms can be added as children to CompositeGraphSym!");
		}
		GraphSym slice = (GraphSym) sym;
		
		if (this.getPointCount() == 0) { // first GraphSym child, so just set xcoords and ycoords
			int[] slice_xcoords = slice.getGraphXCoords();
			float[] slice_ycoords = slice.getGraphYCoords();
			setCoords(slice_xcoords, slice_ycoords);
			slice.setCoords(null, null);
		} else if (slice.getPointCount() > 0) {
			createNewCoords(slice);
		}
		super.addChild(slice);
	}

	private void createNewCoords(GraphSym slice) {
		int slice_min = slice.getMinXCoord();
		int slice_index = GraphSymUtils.determineBegIndex(this, slice_min);

		int coordSize = this.getPointCount();
		int sliceSize = slice.getPointCount();

		int[] old_xcoords = this.getGraphXCoords();
		int[] slice_xcoords = slice.getGraphXCoords();
		int[] new_xcoords = copyXCoords(coordSize, sliceSize, slice_index, old_xcoords, slice_xcoords);

		float[] old_ycoords = this.getGraphYCoords();
		float[] slice_ycoords = slice.getGraphYCoords();
		float[] new_ycoords = copyYCoords(coordSize, sliceSize, slice_index, old_ycoords, slice_ycoords);

		setCoords(new_xcoords, new_ycoords);
		slice.setCoords(null, null);	// get rid of old coords
	}

	private static int[] copyXCoords(int coordSize, int sliceSize, int slice_index, int[] old_coords, int[] slice_coords) {
		int[] new_coords = new int[coordSize + sliceSize];
		int new_index = 0;
		//    old coord array entries up to "A-1"
		if (slice_index > 0) {
			System.arraycopy(old_coords, 0, new_coords, new_index, slice_index);
			new_index += slice_index;
		}
		//    all of slice_coords entries
		System.arraycopy(slice_coords, 0, new_coords, new_index, sliceSize);
		new_index += sliceSize;
		//    old coord array entries from "A" to end of old coord array
		if (slice_index < coordSize) {
			System.arraycopy(old_coords, slice_index, new_coords, new_index, coordSize - slice_index);
		}
		return new_coords;
	}

	private static float[] copyYCoords(int coordSize, int sliceSize, int slice_index, float[] old_coords, float[] slice_coords) {
		float[] new_coords = new float[coordSize + sliceSize];
		int new_index = 0;
		//    old coord array entries up to "A-1"
		if (slice_index > 0) {
			System.arraycopy(old_coords, 0, new_coords, new_index, slice_index);
			new_index += slice_index;
		}
		//    all of slice_coords entries
		System.arraycopy(slice_coords, 0, new_coords, new_index, sliceSize);
		new_index += sliceSize;
		//    old coord array entries from "A" to end of old coord array
		if (slice_index < coordSize) {
			System.arraycopy(old_coords, slice_index, new_coords, new_index, coordSize - slice_index);
		}
		return new_coords;
	}
}
