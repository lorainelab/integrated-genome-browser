/**
*   Copyright (c) 2005 Affymetrix, Inc.
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

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.LeafSingletonSymmetry;

/**
 *   EfficientProbesetSymA is an efficient representation of probesets that meet the following criteria:
 *       a) all probes are same length
 *       b) all probes align to a contiguous genome interval (no split probes)
 *       c) probeset ids can be represented numerically
 *       d) all probes within a probeset are on same strand
 *       e) probeset must have at least one probe
 *
 *   String id is just integer id converted to String representation
 *   Assumption is that this sym will be child of a sym that handles type, etc.
 */
public class EfficientProbesetSymA implements SeqSymmetry, SeqSpan {
  BioSeq seq;
  int probe_length;
  boolean forward;
  int nid;
  int[] child_mins;

  //  public EfficientSnpSym(SeqSymmetry sym_parent, int coord, int nid) {
  public EfficientProbesetSymA(int[] cmins, int probe_length, boolean forward, int nid, BioSeq seq) {
    this.child_mins = cmins;
    this.probe_length = probe_length;
    this.forward = forward;
    this.nid = nid;
    this.seq = seq;
    // if cmins are not already sorted in ascending order, sort them
  }

  /* SeqSymmetry implementation */
  public SeqSpan getSpan(BioSeq bs) {
    if (this.getBioSeq() == bs) { return this; }
    else { return null; }
  }

  public int getSpanCount() { return 1; }

  public SeqSpan getSpan(int i) {
    if (i == 0) { return this; }
    else { return null; }
  }

  public BioSeq getSpanSeq(int i) {
    if (i == 0) { return this.getBioSeq(); }
    else { return null; }
  }

  public boolean getSpan(BioSeq bs, MutableSeqSpan span) {
    if (this.getBioSeq() == bs) {
      span.setStart(this.getStart());
      span.setEnd(this.getEnd());
      span.setBioSeq(this.getBioSeq());
      return true;
    }
    return false;
  }

  public boolean getSpan(int index, MutableSeqSpan span) {
    if (index == 0) {
      span.setStart(this.getStart());
      span.setEnd(this.getEnd());
      span.setBioSeq(this.getBioSeq());
      return true;
    }
    return false;
  }

  public int getChildCount() { return child_mins.length; }
  public SeqSymmetry getChild(int index) {
    if (index >= getChildCount()) { return null; }
    else  {
      int start, end;
      if (forward) {
	start = child_mins[index];
	end = start + probe_length;
      }
      else {
	end = child_mins[index];
	start = end + probe_length;
      }
      return new LeafSingletonSymmetry(start, end, this.getBioSeq());
    }
  }

  public String getID() {
    return Integer.toString(nid);
  }

  /* SeqSpan implementation */

  /**
   *  assumes child_mins has been sorted in ascending order
   */
  public int getStart() {
    if (forward)  { return child_mins[0]; }
    else { return (child_mins[child_mins.length-1] + 25); }
  }

  /**
   *  assumes child_mins has been sorted in ascending order
   */
  public int getEnd() {
    if (forward) { return (child_mins[child_mins.length-1] + 25); }
    else { return child_mins[0]; }
  }

  /**
   *  assumes child_mins has been sorted in ascending order
   */
  public int getMin() { return child_mins[0]; }

  /**
   *  assumes child_mins has been sorted in ascending order
   */
  public int getMax() { return (child_mins[child_mins.length-1] + 25); }

  public int getLength() { return (getMax() - getMin()); }
  public boolean isForward() { return forward; }
  public BioSeq getBioSeq() { return seq; }
  public double getStartDouble() { return (double)getStart(); }
  public double getEndDouble() { return (double)getEnd(); }
  public double getMinDouble() { return (double)getMin(); }
  public double getMaxDouble() { return (double)getMax(); }
  public double getLengthDouble() { return (double)getLength(); }
  public boolean isIntegral() { return true; }


}
