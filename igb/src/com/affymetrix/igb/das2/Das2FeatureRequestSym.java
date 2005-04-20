/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

package com.affymetrix.igb.das2;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;

/**
 *  Encapsulates a _constrained_ DAS2 feature query and the features returned from the query
 *  The expectation is that if IGB user wants multiple regions on multiple seqs with multiple types,
 *      these queries will get broken down by IGB DAS2 query optimizer into multiple Das2FeatureRequestSyms
 *
 *  Constraints:
 *    Query for features on a single seq
 *    One overlap span on seq
 *    Zero or one inside span on seq
 *    One feature type
 *
 *  OR, do we want a more generic Das2FeatureRequestSym with a list of feature filters
 *  OR, break down into two classes -- this one and a more generic filter-based one...
 *
 *  sym.getSpanCount() = 1, and this SeqSpan is the encompass_span
 *       (bounds of all returned spans [or union of overlap_span and bounds of all returned spans?]
 *
 */
//public class Das2FeatureRequestSym   {extends Das2ContainerAnnot {
// public class Das2FeatureRequestSym extends TypeContainerAnnot {
public class Das2FeatureRequestSym extends SimpleSymWithProps  {  // or should extend TypeContainerAnnot?

  SeqSpan overlap_span;
  SeqSpan inside_span;
  // SeqSpan encompass_span;  // not needed, this is actually standard span of a single-span symmetry
  Das2ContainerAnnot parent_container;

  /**
   *  Returns the overlap span, the span specified in the original DAS query
   *    that returned annotation must overlap with.
   *  May need to returns a sym instead of span to better match up with SeqSymmetrySummarizer methods??
   */
  public SeqSpan getOverlapSpan() { return overlap_span; }

  /**
   *  Returns the inside span, the span specified in the original DAS query
   *    that returned annotation must be contained within.
   *  May need to returns a sym instead of span to better match up with SeqSymmetrySummarizer methods??
   */
  public SeqSpan getInsideSpan() { return inside_span; }

  /**
   *  Returns encompass span, the union of the bounds of all the features returned by the DAS2 feature query
   *  (OR, may need to be union of overlap span and bounds of all features returned...)
   */
  public SeqSpan getEncompassSpan() { return getSpan(0); }

  public Das2ContainerAnnot getParentContainer() { return parent_container; }

  public Das2Region getRegion() { return parent_container.getRegion(); }

  public Das2Type getDas2Type() { return parent_container.getDas2Type(); }



}
