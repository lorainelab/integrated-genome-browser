package com.affymetrix.igb.genometry;

import java.io.*;
import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.parsers.BarParser;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.SeqUtils;

/**
 *  GraphSliceOptimizer is intended to be used for piecewise loading of graph slices rather than
 *     loading entire graphs at once.
 *
 *  Basic idea is that figuring out what graph slice(s) to retrieve is similar to (but simpler than)
 *     the task Das2ClientOptimizer handles for optimizing DAS/2 region-filtered feature requests
 *     So borrowing heavily from Das2ClientOptmizer, but simplifying where possible
 *
 *  For now assuming that graph slices are being extracted from bar formatted graph files,
 *  Soon will switch to requesting slices from a remote "slice server"?
 */
public class GraphSliceOptimizer {

  static String TEST_BAR_FILE = "c:/data/graph_slice_test/test.bar";
  /**
   *  Given a CompositeGraphSym and a SeqSpan, retrieve slice(s) of graph to make sure
   *  entire span is covered by slice(s).  Optimize so any region already retrieved does
   *  not need to be retrieved again
   *
   *  returns boolean indicating whether any data points were actually loaded
   *    (if false (and no errors), then presumably the slice was already completely covered
   *    by previous requests
   *
   */
  public static boolean loadSlice(CompositeGraphSym gsym, SeqSpan slice_span) {
    boolean TEST1 = false;
    boolean TEST2 = false;
    boolean TEST3 = true;
    boolean new_points = true;
    BioSeq aseq = slice_span.getBioSeq();

    // test by adding points in middle of slice_span
    if (TEST1) {
      int test_count = 1000;
      int test_step = 50;
      int midspan = (slice_span.getMin() + (slice_span.getLength()/2));
      int[] xcoords = new int[test_count];
      float[] ycoords = new float[test_count];
      int xpos = midspan - (test_count * test_step / 2);
      for (int i=0; i<test_count; i++) {
	xcoords[i] = xpos;
	ycoords[i] = (float)(100 * Math.random()) - 50f;
	xpos += test_step;
      }
      ycoords[0] = 200f;
      ycoords[1] = 100f;
      ycoords[ycoords.length-1] = 200f;
      ycoords[ycoords.length-2] = 100f;

      GraphSym child = new GraphSym(xcoords, ycoords, "unknown", aseq);
      child.removeSpan(child.getSpan(aseq));
      child.addSpan(slice_span);
      gsym.addChild(child);
    }
    else {
      int prevcount = gsym.getChildCount();
      java.util.List subslices = new ArrayList();
      SeqSymmetry slice_sym = new SingletonSeqSymmetry(slice_span);

      if (prevcount == 0) {
	subslices.add(slice_sym);
      }
      else {
	ArrayList prev_slices = new ArrayList(prevcount);
	for (int i=0; i<prevcount; i++) {
	  SeqSymmetry prev_slice = gsym.getChild(i);
	  if (prev_slice instanceof GraphSym) {
	    prev_slices.add(prev_slice);
	  }
	}

	//	System.out.println("number of previous slices: " + prev_slices.size());
	SeqSymmetry prev_union = SeqSymSummarizer.getUnion(prev_slices, aseq);
	//	System.out.println("union of previous slices: ");
	//	SeqUtils.printSymmetry(prev_union);
	ArrayList qnewlist = new ArrayList();
	qnewlist.add(slice_sym);
	ArrayList qoldlist = new ArrayList();
	qoldlist.add(prev_union);
	SeqSymmetry split_slice = SeqSymSummarizer.getExclusive(qnewlist, qoldlist, aseq);
        if (split_slice == null)  {
          System.out.println("split slice is null!");
          return true;
        }

	int slice_count = split_slice.getChildCount();
	for (int i=0; i<slice_count; i++) {
	  subslices.add(split_slice.getChild(i));
	}
      }


      for (int slice_index=0; slice_index < subslices.size(); slice_index++) {
	SeqSymmetry subslice = (SeqSymmetry)subslices.get(slice_index);
	SeqSpan subspan = subslice.getSpan(aseq);
	if (TEST2) {
	  int test_count = 1000;
	  int test_step = 50;
	  int midspan = (subspan.getMin() + (subspan.getLength()/2));
	  int[] xcoords = new int[test_count];
	  float[] ycoords = new float[test_count];
	  int xpos = midspan - (test_count * test_step / 2);
	  for (int i=0; i<test_count; i++) {
	    xcoords[i] = xpos;
	    ycoords[i] = (float)(100 * Math.random()) - 50f;
	    xpos += test_step;
	  }
	  xcoords[0] = subspan.getMin();
	  xcoords[1] = subspan.getMin() + 5;
	  ycoords[0] = 200f;
	  ycoords[1] = 100f;

	  xcoords[xcoords.length-1] = subspan.getMax()-5;
	  xcoords[xcoords.length-2] = subspan.getMax()-10;
	  ycoords[ycoords.length-1] = 200f;
	  ycoords[ycoords.length-2] = 100f;

	  GraphSym child = new GraphSym(xcoords, ycoords, "unknown", aseq);
          child.removeSpan(child.getSpan(aseq));
	  child.addSpan(subspan);
	  gsym.addChild(child);
	}
	else if (TEST3) {
	  // graphsym span is also set correctly in BarParser.getSlice()
	  GraphSym child = BarParser.getSlice(TEST_BAR_FILE, subspan);
	  gsym.addChild(child);
	}
      }
    }
    return new_points;
  }

}
