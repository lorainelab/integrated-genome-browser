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

import java.util.*;

public class DasType {
  String type_id;
  String method;
  String category;
  List preferred_formats;
  int annot_count;
  // hmm, how about two timestamps?
  // This could take advantage of a DAS server which can distinguish between 
  //   the last time annotations of a particular type were _added_ to it, versus 
  //   the last time a pre-existing annotation of a particular type was _modified_
  // For example, at UCSC EST annotations are updated regularly (weekly? nightly?)
  //   However for the most part previous EST annotations don't change, but instead 
  //   new annotations are added for new ESTs
  //   (see http://www.soe.ucsc.edu/pipermail/genome/2004-April/004409.html for an 
  //     brief explanation -- incremental nightly updates are just appended to 
  //     the end of the UCSC tables (or at least the table dumps))
  // Therefore could potentially improve (yet-to-be-implemented) caching considerably if 
  //    could distinguish that no previous annotations of type X have changed, just 
  //    that more have been added since last cached date D, and then have a way to modify 
  //    the DAS range query "feature filter" to specify only loading features that have 
  //    been added since date D (or over a time range -- from D to current time).  
  // Of course this also implies some more smarts in the caching mechanism since can then 
  //    end up with overlapping cached DAS-range-queries for same type, because they 
  //    differ in their _time-range_
  //   
  long last_modified;  // timestamp (in milliseconds?)
  long last_added;  
  DasSource das_source;

  public DasType(DasSource source, String id) {
    type_id = id;
    das_source = source;
  }

  public String getID() { return type_id; }
  public String getMethod() { return method; }
  public String getCategory() { return category; }
  public List getPreferredFormats() { return preferred_formats; }
  public int getAnnotationCount() { return annot_count; }
  public long getLastModified() { return last_modified; }
  public DasSource getDasSource() { return das_source; }
}
