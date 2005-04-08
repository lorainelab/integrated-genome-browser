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

package com.affymetrix.igb.das2;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.igb.genometry.SmartAnnotBioSeq;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;


public class Das2Region {
  String region_id;
  int start;
  int end;
  String name;
  String info_url;
  boolean forward;
  //  java.util.List assembly;  // or should this be a SeqSymmetry??   // or composition of CompositeBioSeq??
  SeqSpan segment_span;
  MutableAnnotatedBioSeq aseq;
  Das2VersionedSource versioned_source;

  public Das2Region(Das2VersionedSource source, String id) {
    region_id = id;
    versioned_source = source;
  }


  public String getID() { return region_id; }  // or should ID be a URI?
  public String getName() { return name; }
  public String getInfoUrl() { return info_url; }
  public Das2VersionedSource getVersionedSource() { return versioned_source; }
  //  public String getSeqType() { return seqtype; }
  //  public boolean hasSubParts() { return has_subparts; }
  protected void setInterval(int start, int end, boolean forward_orient) {
    this.start = start;  // should already be in 0-interbase coords
    this.end = end;
    this.forward = forward_orient;
  }

  public SeqSpan getSegment() {
    if (segment_span == null) {
      initRegion();
    }
    return segment_span;
  }

  public MutableAnnotatedBioSeq getAnnotatedSeq() {
    if (aseq == null) {
      initRegion();
    }
    return aseq;
  }


  protected void initRegion() {
    AnnotatedSeqGroup genome = versioned_source.getGenome();

    // a)  see if id of Das2Region hashes directly to an already seen annotated seq in genome
    aseq = genome.getSeq(region_id);

    // b) if can't find a previously seen genome for this DasSource, then
    //     create a new genome entry
    if (aseq == null) {
      aseq = new SmartAnnotBioSeq(region_id, genome.getID(), end);  // therefore end must be populated first!
      genome.addSeq(aseq);
    }

    // System.out.println(aseq);

    if (forward) {  segment_span = new SimpleSeqSpan(start, end, aseq);  }
    else {  segment_span = new SimpleSeqSpan(end, start, aseq); }
    System.out.println("in initRegion() method, start = " + start + ", end = " + end);
    System.out.println("    seq = " + aseq.getID() + ", genome = " + genome.getID());

  }

}
