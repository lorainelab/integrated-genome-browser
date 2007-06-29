package com.affymetrix.genometryImpl;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.util.Timer;

/**
 *   CompositeGraphSym.
 *   Was originally envisioning that a CompositeGraphSym would have a set of GraphSym children
 *   But, this causes lots of problems for calculations that need to cross transitions between children,
 *      for example percentile binning or dynamic thresholding
 *   So, new plan is to keep composite graph x and y coords each in single array, and every time
 *      more coords are added make new array and populate with old and new coords via System.arraycopy()
 *      BUT, also have child syms of CompositeGraphSym that keep track of what slices coords have already
 *      been populated from
 */
public class CompositeGraphSym extends GraphSym  {
  // extends GraphSym  {
  //  public Object clone()  // Does clone need to be reimplemented here?  Not sure yet...

  public CompositeGraphSym(String id, BioSeq seq) {
    super(null, null, id, seq);
  }

  /**
   *  Overriding addChild() to only accept GraphSym children,
   *     integrates x and y coord arrays of child into composite's coord arrays
   *     (and nulls old ones out for gc)
   *  Assumes that slices can abut but do _not_ overlap
   *    "abut" in this case means that (sliceA.span.max == sliceB.span.min)
   *    since these are half-open half-closed intervals, this is not actually overlap but abutment...
   *
   */
  public void addChild(SeqSymmetry sym)  {
    if (sym instanceof GraphSym) {
      GraphSym slice = (GraphSym)sym;
      int[] slice_xcoords = slice.getGraphXCoords();
      float[] slice_ycoords = slice.getGraphYCoords();

      if (xcoords == null && ycoords == null) { // first GraphSym child, so just set xcoords and ycoords
	xcoords = slice_xcoords;
	ycoords = slice_ycoords;
	slice.xcoords = null;
	slice.ycoords = null;
      }
      else {

        // if no data points in slice, then just keep old coords
        if ((slice_xcoords != null) && (slice_xcoords.length > 0))  {

	  // use binary search to figure out what index "A" that slice_xcoords array should insert
	  //    into existing xcoord array
	  int slice_min = slice_xcoords[0];
	  int slice_index = Arrays.binarySearch(xcoords, slice_min);
	  if (slice_index < 0) {
	    // want draw_beg_index to be index of max xcoord <= view_start
	    //  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
	    slice_index = (-slice_index -1);
	  }

	  int[] new_xcoords = new int[xcoords.length + slice_xcoords.length];
	  int new_index = 0;
	  // since slices cannot overlap, new xcoord array should be:

	  //    old xcoord array entries up to "A-1"
	  if (slice_index > 0)  {
	    System.arraycopy(xcoords, 0, new_xcoords, new_index, slice_index);
	    new_index += slice_index;
	  }
	  //    all of slice_xcoords entries
	  System.arraycopy(slice_xcoords, 0, new_xcoords, new_index, slice_xcoords.length);
	  new_index += slice_xcoords.length;
	  //    old xcoord array entries from "A" to end of old xcoord array
	  if (slice_index < xcoords.length) {
	    System.arraycopy(xcoords, slice_index, new_xcoords, new_index, xcoords.length - slice_index);
	  }

	  // get rid of old xcoords
	  xcoords = new_xcoords;
	  slice_xcoords = null;
	  slice.xcoords = null;

	  float[] new_ycoords = new float[ycoords.length + slice_ycoords.length];
	  new_index = 0;
	  //    old ycoord array entries up to "A-1"
	  if (slice_index > 0)  {
	    System.arraycopy(ycoords, 0, new_ycoords, new_index, slice_index);
	    new_index += slice_index;
	  }
	  //    all of slice_ycoords entries
	  System.arraycopy(slice_ycoords, 0, new_ycoords, new_index, slice_ycoords.length);
	  new_index += slice_ycoords.length;
	  //    old ycoord array entries from "A" to end of old ycoord array
	  if (slice_index < ycoords.length) {
	    System.arraycopy(ycoords, slice_index, new_ycoords, new_index, ycoords.length - slice_index);
	  }

	  ycoords = new_ycoords;
	  slice_ycoords = null;
	  slice.ycoords = null;
	  // trying to encourage garbage collection of old coord arrays
	  //	System.gc();
	}
	// also need to recalculate point_min_ycoord and point_max_ycoord
	//   but already know these for previous coords, so just iterate through slice coords to update
	// actually these are only in the graph glyphs -- but might want to move them to graph sym
	//   to improve recalculation performance
      }
      // assuming GraphSym seq span is bounds of graph slice, add GraphSym as child
      // but remember coords are nulled out!
      super.addChild(slice);
    }

    else {
      throw new RuntimeException("only GraphSyms can be added as children to CompositeGraphSym!");
    }
  }


  public static void main(String[] args) {
    // testing large System.arrayCopy operations
    Timer tim= new Timer();
    int asize = 5000000;
    int[] array1 = new int[asize];
    float[] array2 = new float[asize];
    for (int i=0; i<array1.length; i++) {
      array1[i] = i*3;
      array2[i] = i*5.0f;
    }

    System.out.println("starting array copies");

    tim.start();
    int[] array3 = new int[asize];
    System.arraycopy(array1, 0, array3, 0, asize);
    long timeA = tim.read();
    tim.start();
    float[] array5 = new float[asize];
    for (int i=0; i<asize; i++) {
      array5[i] = array2[i];
    }
    long timeC = tim.read();
    tim.start();
    float[] array4 = new float[asize];
    System.arraycopy(array2, 0, array4, 0, asize);
    long timeB = tim.read();


    System.out.println("time for int array copy: " + timeA/1000f);
    System.out.println("time for float incremental copy: " + timeC/1000f);
    System.out.println("time for float array copy: " + timeB/1000f);

  }

  /*  Old plan -- child GraphSym's, each with own xcoord and ycoord array
  public int[] getGraphXCoords() {
    int total_points = this.getPointCount();
    int[] xcoords = new int[total_points];
    int point_count = 0;
    int child_count = children.size();
    for (int i=0; i<child_count; i++) {
      Object child = children.get(i);
      if (child instanceof GraphSym) {
	GraphSym gsym = (GraphSym)child;
	int[] child_xcoords = gsym.getGraphXCoords();
	int child_point_count = child_xcoords.length;
	System.arraycopy(child_xcoords, 0, xcoords, point_count, child_point_count);
	point_count += child_point_count;
      }
    }
    return xcoords;
  }

  public float[] getGraphYCoords() {
    int total_points = this.getPointCount();
    float[] ycoords = new float[total_points];
    int point_count = 0;
    int child_count = children.size();
    for (int i=0; i<child_count; i++) {
      Object child = children.get(i);
      if (child instanceof GraphSym) {
	GraphSym gsym = (GraphSym)child;
	float[] child_ycoords = gsym.getGraphYCoords();
	int child_point_count = child_ycoords.length;
	System.arraycopy(child_ycoords, 0, ycoords, point_count, child_point_count);
	point_count += child_point_count;
      }
    }
    return ycoords;
  }

  public int getPointCount() {
    int point_count = 0;
    int child_count = children.size();
    for (int i=0; i<child_count; i++) {
      Object child = children.get(i);
      if (child instanceof GraphSym) {
	point_count += ((GraphSym)child).getPointCount();
      }
    }
    return point_count;
  }
  */


}
