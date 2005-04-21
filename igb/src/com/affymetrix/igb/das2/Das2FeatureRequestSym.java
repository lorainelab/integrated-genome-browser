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
public class Das2FeatureRequestSym extends SimpleSymWithProps implements TypedSym  {  // or should extend TypeContainerAnnot?

  boolean initialized = false;
  SeqSpan overlap_span;
  SeqSpan inside_span;
  // SeqSpan encompass_span;  // not needed, this is actually standard span of a single-span symmetry

  Das2Region das2_region;
  Das2Type das2_type;
  // not sure how to make sure that parent is a Das2ContainerAnnot, since auto-containment in SmartAnnotBioSeq
  //     uses TypeContainerAnnot instead.  So for now make parent the TypeContainerAnnot
  //  Das2ContainerAnnot parent_container;
  TypeContainerAnnot parent_container;

  /*
    actually should be able to populate with fewer args to constructor
    given Das2Type and Das2Region, should be able to figure out TypeContainerAnnot?
  */
  //  public Das2FeatureRequestSym(Das2Type type, Das2Region region, TypeContainerAnnot container, SeqSpan overlap, SeqSpan inside) {
  /*  for now trying to do without container info in constructor */
  public Das2FeatureRequestSym(Das2Type type, Das2Region region, SeqSpan overlap, SeqSpan inside) {
    das2_type = type;
    das2_region = region;
    //    parent_container = container;
    overlap_span = overlap;
    inside_span = inside;
  }

  public String getType() { return das2_type.getID(); }

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

  //  public Das2ContainerAnnot getParentContainer() { return parent_container; }
  //  public Das2Region getRegion() { return parent_container.getRegion(); }
  //  public Das2Type getDas2Type() { return parent_container.getDas2Type(); }

  public TypeContainerAnnot getParentContainer() { return parent_container; }
  public Das2Region getRegion() { return das2_region; }
  public Das2Type getDas2Type() { return das2_type; }

  public boolean isInitialized() { return initialized; }

  // for now just consider overlap_span to be the actual span...
  public SeqSpan getSpan(int index) {
    if (index == 0) { return overlap_span; }
    else { return null; }
  }

  public int getSpanCount() { return 1; }

  /**  override getChildCount() to initialize (populate via DAS2 feature query) if not yet initialized? */
  /*
  public int getChildCount() {
    if (! initialized) { init(); }
    return super.getChildCount();
  }
  */

  /**  override getChildren() to initialize (populate via DAS2 feature query) if not yet initialized? */
  /*
  public SeqSymmetry getChild(int index) {
    if (! initialized) { init(); }
    return super.getChild(index);
  }
  */

  /*
  public void init() {
    // initialize via call to Das2Region.getFeatures(this)?
    intialized = true;
  }
  */

}
