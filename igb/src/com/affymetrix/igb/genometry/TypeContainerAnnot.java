/**
 *   Copyright (c) 2001-2006 Affymetrix, Inc.
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

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;

/**
 *  Top-level annots attached to a SmartAnnotBioSeq.
 */
public class TypeContainerAnnot extends SimpleSymWithProps implements TypedSym  {
  String type;
  
  public TypeContainerAnnot(String type) {
    super();
    this.type = type;
    this.setProperty("method", type);
  }
  
  public String getType()  { return type; }
  
  
  /** Returns the minimum and maximum positions of all included annotations.
   *  Necessary because getMin() and getMax() do not give this information
   *  for this type of SeqSymmetry.
   *
   *  @param seq  consider annotations only on this seq
   *  @param exclude_graphs if true, ignore graph annotations
   *  @param min  an initial minimum value.
   *  @param min  an initial maximum value.
   */
  public int[] getAnnotationBounds(BioSeq seq, boolean exclude_graphs, int min, int max) {
    int[] min_max = new int[2];
    min_max[0] = min;
    min_max[1] = max;
    
    int child_count = this.getChildCount();
    for (int j=0; j<child_count; j++) {
      SeqSymmetry next_sym = this.getChild(j);
      
      int annotCount = next_sym.getChildCount();
      for (int i=0; i<annotCount; i++) {
        // all_gene_searches, all_repeat_searches, etc.
        SeqSymmetry annotSym = next_sym.getChild(i);
        if (annotSym instanceof GraphSym) {
          if (! exclude_graphs) {
            GraphSym graf = (GraphSym)annotSym;
            int[] xcoords = graf.getGraphXCoords();
            min_max[0] = Math.min(xcoords[0], min_max[0]);
            min_max[1] = Math.max(xcoords[xcoords.length-1], min_max[0]);
          }
        } else {
          SeqSpan span = annotSym.getSpan(seq);
          if (span != null) {
            min_max[0] = Math.min(span.getMin(), min_max[0]);
            min_max[1] = Math.max(span.getMax(), min_max[1]);
          }
        }
      }
    }
    return min_max;
  }
}
