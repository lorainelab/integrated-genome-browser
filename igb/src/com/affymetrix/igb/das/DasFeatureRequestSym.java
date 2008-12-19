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

package com.affymetrix.igb.das;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;

public class DasFeatureRequestSym extends SimpleSymWithProps  {

  SeqSymmetry query_overlap;
  SeqSymmetry query_within;
  
  
  /**
   *  Returns query_overlap span, the span specified in the original DAS query 
   *    that returned annotation must overlap with.
   *  Returns a sym instead of span to better match up with SeqSymmetrySummarizer 
   *      methods.
   */
  public SeqSymmetry getOverlapSpan() { return query_overlap; }

  /**
   *  Returns query_within span, the span specified in the original DAS query 
   *    that returned annotation must be contained within.
   *  Returns a sym instead of span to better match up with SeqSymmetrySummarizer 
   *      methods.
   */
  public SeqSymmetry getWithinSpan() { return query_within; }

}
