/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.genometry;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.GraphSym;
import com.affymetrix.igb.util.FloatList;
import com.affymetrix.igb.util.IntList;


/**
 *  NO LONGER USED, USE GraphSymUtils.transformGraphSym() instead
 */
public class GraphSymTransformer {

  //  public GraphSym transformGraphSym(GraphSym osym, SeqSymmetry tsym, BioSeq tseq) {
  //    GraphSym result = null;
  //    return result;
  //  }

  /**
   *  Transforms a GraphSym based on a SeqSymmetry.
   *  This is _not_ a general algorithm for transforming GraphSyms with an arbitrary mapping sym --
   *    it is simpler, and assumes that the mapping symmetry is of depth=2 (or possibly 1?) and
   *    breadth = 2, and that they're "regular" (parent sym and each child sym have seqspans pointing
   *    to same two BioSeqs
   */
  /*
  public static GraphSym transformGraphSym(GraphSym original_graf, SeqSymmetry mapsym) {
    BioSeq fromseq = original_graf.getGraphSeq();
    SeqSpan fromspan = mapsym.getSpan(fromseq);
    GraphSym new_graf = null;
    if (fromseq != null && fromspan != null) {
      BioSeq toseq = SeqUtils.getOtherSeq(mapsym, fromseq);
      SeqSpan tospan = mapsym.getSpan(toseq);
      if (toseq != null && fromseq != null) {
	int[] xcoords = original_graf.getGraphXCoords();
	float[] ycoords = original_graf.getGraphYCoords();
	if (xcoords != null && ycoords != null) {
	  new_graf = new GraphSym(null, null, original_graf.getGraphName(), toseq);
	  double graf_base_length = xcoords[xcoords.length-1] - xcoords[0];
	  // calculating graf length from xcoords, since graf's span
	  //    is (usually) incorrectly set to start = 0, end = seq.getLength();
	  double points_per_base = (double)xcoords.length / (double)graf_base_length;
	  int initcap = (int)(points_per_base * toseq.getLength() * 1.5);
	  //    System.out.println("initial capacity for new_xcoords DoubleList: " + initcap);
	  IntList new_xcoords = new IntList(initcap);
	  FloatList new_ycoords = new FloatList(initcap);
	  List leaf_syms = SeqUtils.getLeafSyms(mapsym);
	  for (int i=0; i<leaf_syms.size(); i++) {
	    SeqSymmetry leafsym = (SeqSymmetry)leaf_syms.get(i);
	    SeqSpan fspan = leafsym.getSpan(fromseq);
	    SeqSpan tspan = leafsym.getSpan(toseq);
	    if (fspan == null || tspan == null) { continue; }
	    boolean opposite_spans = fspan.isForward() ^ tspan.isForward();
	    int ostart = fspan.getStart();
	    int oend = fspan.getEnd();
	    int tstart = tspan.getStart();
	    int tend = tspan.getEnd();
	    double scale = tspan.getLengthDouble() / fspan.getLengthDouble();
	    if (opposite_spans) { scale = -scale; }
	    double offset = tspan.getStartDouble() - (scale * fspan.getStartDouble());
	    int kmax = xcoords.length;
	    // should really use a binary search here to speed things up...
	    // but right now just doing a brute force scan for each leaf span to map to toseq
	    //    any graph points that overlap fspan in fromseq
	    for (int k=0; k<kmax; k++) {
	      double old_xcoord = xcoords[k];
	      if (old_xcoord >= ostart && old_xcoord <= oend) {
		int new_xcoord = (int)((scale * old_xcoord) + offset);
		new_xcoords.add(new_xcoord);
		new_ycoords.add(ycoords[k]);
	      }
	    }
	  }
	  new_graf.setGraphCoords(new_xcoords.copyToArray(), new_ycoords.copyToArray());
	}
      }
    }
    return new_graf;
  }
  */
}
