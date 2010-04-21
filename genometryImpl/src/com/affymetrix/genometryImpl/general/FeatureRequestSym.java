/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.LeafSingletonSymmetry;

/**
 *
 * @author jnicol
 */
public class FeatureRequestSym extends SimpleSymWithProps {

  private final LeafSingletonSymmetry overlap_span; // LeafSingletonSym also implements SeqSymmetry interface
   private final SeqSpan inside_span;

  //  for now trying to do without container info in constructor
  public FeatureRequestSym(SeqSpan overlap, SeqSpan inside) {
    overlap_span = new LeafSingletonSymmetry(overlap);
	inside_span = inside;
	
    this.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
  }

  /**
   *  Returns the overlap span, the span specified in the original query
   *    that returned annotation must overlap with.
   */
  // May need to returns a sym instead of span to better match up with SeqSymmetrySummarizer methods??
  public final SeqSpan getOverlapSpan() { return overlap_span; }

  /**
   *  Convenience method for returning overlap span as a SeqSymmetry with 1 span and 0 children.
   */
  public final SeqSymmetry getOverlapSym() { return overlap_span; }
  
  /**
   *  Returns the inside span, the span specified in the original query
   *    that returned annotation must be contained within.
   */
  public final SeqSpan getInsideSpan() { return inside_span; }


}
