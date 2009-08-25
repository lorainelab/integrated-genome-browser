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

package com.affymetrix.igb.das;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;


public final class DasEntryPoint {

  String entry_id;
  String description;
  String seqtype;
  SeqSpan segment_span;
  MutableAnnotatedBioSeq aseq;
  DasSource das_source;
  boolean has_subparts;
  int start; // start: 0-interbase coords
  int stop;
  boolean forward = true;

  // still need to figure out matching up entry point to MutableAnnotatedBioSeq in genometry model
  public DasEntryPoint(DasSource source, String id) {
    entry_id = id;
    das_source = source;
  }

  /** Passing in data to build segment span.
   *  @param start  interval start in 1-base coords; gets converted to 0-interbase coords
   */
  protected void setInterval(int start, int stop, boolean forward_orient) {
    this.start = start-1;  // converting from 1-base coords to 0-interbase coords
    this.stop = stop;
    this.forward = forward_orient;
	initEntryPoint();
  }
  protected void setDescription(String desc) { description = desc; }
  protected void setSeqType(String type) { seqtype = type; }
  protected void setSubParts(boolean b) { has_subparts = b; }

  public String getID() {  return entry_id; }  // or should ID be a URI?
  public String getDescription() { return description; }
  public String getSeqType() { return seqtype; }  // or should ID be a URI?
  //public boolean hasSubParts() { return has_subparts; }
  public SeqSpan getSegment() {
    if (segment_span == null) {
      initEntryPoint();
    }
    return segment_span;
  }
  public MutableAnnotatedBioSeq getAnnotatedSeq() {
    if (aseq == null) {
      initEntryPoint();
    }
    return aseq;
  }
  public DasSource getDasSource() { return das_source; }

  protected void initEntryPoint() {
    AnnotatedSeqGroup genome = das_source.getGenome();

    // a)  see if id of DasEntryPoint hashes directly to an already seen annotated seq in genome
    aseq = genome.getSeq(entry_id);

    // b) if can't find a previously seen genome for this DasSource, then
    //     create a new genome entry
    if (aseq == null) {
      // therefore stop must be populated first!
      genome.addSeq(entry_id, stop);
    }

    // System.out.println(aseq);

    if (forward) {  segment_span = new SimpleSeqSpan(start, stop, aseq);  }
    else {  segment_span = new SimpleSeqSpan(stop, start, aseq); }
  }

}
