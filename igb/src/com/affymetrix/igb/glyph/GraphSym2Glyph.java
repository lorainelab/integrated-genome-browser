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

package com.affymetrix.igb.glyph;

import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.GraphSym;
import com.affymetrix.igb.util.FloatList;
import com.affymetrix.igb.util.IntList;

/**
 *  Takes a GraphSym and a Symmetry hierarchy,
 *  and creates graph glyphs mapping GraphSym xcoords to
 *      scene/map coordinates.
 */
public class GraphSym2Glyph {


  public static void modifyGraphGlyph(GraphSym osym, SeqSymmetry sym, BioSeq tseq, GraphGlyph gl) {
    //    System.out.println("GraphSym2Glyph received a modifyGraphGlyph() call");
    // System.out.println("mapping symmetry: " );
    // SeqUtils.printSymmetry(sym);

    BioSeq oseq = osym.getGraphSeq();
    // oseq is BioSeq for original GraphSym, tseq is BioSeq that new GraphGlyph should base its xcoords on

    // assumes xcoords are sorted in ascending positions
    int[] xcoords = osym.getGraphXCoords();
    float[] ycoords = osym.getGraphYCoords();
    double points_per_base =
      ((double)xcoords.length / (double)osym.getSpan(oseq).getLength());
    // calculating initial capacity of new_xcoords to be:
    //   (average # of points / base) *  (# of bases in tseq) * 1.5   [1.5 gives it 50% deviation buffer]
    int initcap = (int)(points_per_base * tseq.getLength() * 1.5);
    IntList new_xcoords = new IntList(initcap);
    FloatList new_ycoords = new FloatList(initcap);
    if (oseq == null || xcoords == null || ycoords == null) { return; }
    List leaf_syms = SeqUtils.getLeafSyms(sym);
    for (int i=0; i<leaf_syms.size(); i++) {
      SeqSymmetry leafsym = (SeqSymmetry)leaf_syms.get(i);
      SeqSpan ospan = leafsym.getSpan(oseq);
      SeqSpan tspan = leafsym.getSpan(tseq);
      if (ospan == null || tspan == null) { continue; }
      boolean opposite_spans = ospan.isForward() ^ tspan.isForward();
      int ostart = ospan.getStart();
      int oend = ospan.getEnd();
      int tstart = tspan.getStart();
      int tend = tspan.getEnd();

      //    double scale = tspan.getLengthDouble() / ospan.getLengthDouble();
      // getting m (scale) and b (offset) parameters for y = mx+b calc
      //   (newx = (scale * x) + offset)
      //   this works for opposite_spans too, since then scale goes negative
      // if change to use getLength(), must factor in potential sign change in scale...
      double scale = tspan.getLengthDouble() / ospan.getLengthDouble();
      if (opposite_spans) { scale = -scale; }
      double offset = tspan.getStartDouble() - (scale * ospan.getStartDouble());
      int kmax = xcoords.length;
      // should really use a binary search here to speed things up...
      // but right now just doing a brute force scan for each leaf span to map to tseq
      //    any graph points that overlap ospan in oseq
      for (int k=0; k<kmax; k++) {
	int old_xcoord = xcoords[k];
	if (old_xcoord >= ostart && old_xcoord <= oend) {
	  int new_xcoord = (int)((scale * old_xcoord) + offset);
	  new_xcoords.add(new_xcoord);
	  new_ycoords.add(ycoords[k]);
	}
      }
    }
    Rectangle2D cbox = gl.getCoordBox();
    //    System.out.println("in GraphSym2Glyph, setting point coords for modified graph");
    gl.setPointCoords(new_xcoords.copyToArray(), new_ycoords.copyToArray());
    SeqSpan parent_tspan = sym.getSpan(tseq);
    gl.setCoords(parent_tspan.getMin(), cbox.y, parent_tspan.getLength(), cbox.height);
  }

}
