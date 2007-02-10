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
import java.util.*;

/**
 *   EfficientProbesetSymA is an efficient representation of probesets that meet
 *   certain criteria.
 *   <pre>
 *       a) all probes are same length
 *       b) all probes align to a contiguous genome interval (no split probes)
 *       c) probeset ids can be represented numerically (with an optional prefix string)
 *       d) all probes within a probeset are on same strand
 *       e) probeset must have at least one probe
 *   </pre>
 *
 *   Assumption is that this sym will be child of a sym that handles type, etc.
 */
public class EfficientProbesetSymA implements SeqSymmetry, SeqSpan, SymWithProps, ParentOfLeafSym {
  BioSeq seq;
  int probe_length;
  boolean forward;
  int nid;
  int[] child_mins;
  String id_prefix;
  Map props;

  /**
   * Constructor.
   * @param props  a hash (usually shared with other syms) that EfficientProbesetSymA can use for common properties
   * @param cmins an array of the minima of the probe positions, this should
   *   be sorted in ascending order (but will be automatically sorted by this
   *   routine if this is not the case.  This means that the ordering
   *   of the elements in the array you pass in may be altered as a side-effect.)
   * @param probe_length  the length of each probe
   * @param forward  true for forward strand
   * @param nid  an integer to be used as the ID
   * @param seq  the BioSeq
   */
  public EfficientProbesetSymA(Map props, int[] cmins, int probe_length, boolean forward,
  			       String prefix, int nid, BioSeq seq) {
    this.props = props;
    this.child_mins = cmins;
    this.probe_length = probe_length;
    this.forward = forward;
    this.nid = nid;
    this.seq = seq;
    this.id_prefix = prefix;


    java.util.Arrays.sort(this.child_mins);
  }

  /** implementing ParentOfLeafSpan interface */
  public MutableSeqSpan getChildSpan(int child_index, BioSeq aseq, MutableSeqSpan result_span) {
    if ((child_index >= child_mins.length) ||
	(aseq != seq) ||
	(result_span == null)) {
      return null;
    }
    if (forward) { result_span.set(child_mins[child_index], child_mins[child_index] + probe_length, seq); }
    else { result_span.set(child_mins[child_index] + probe_length, child_mins[child_index], seq); }
    return result_span;
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

  public int getIntID() { return nid; }
  public int getProbeLength() { return probe_length; }
  public String getPrefixID() { return id_prefix; }

  /** The integer id converted to String representation. */
  public String getID() {
    String rootid = Integer.toString(getIntID());
    if (id_prefix == null) {
      return rootid;
    }
    else {
      return (id_prefix + rootid);
    }
  }

  /* SeqSpan implementation */

  public int getStart() {
    // assumes child_mins has been sorted in ascending order
    if (forward)  { return child_mins[0]; }
    else { return (child_mins[child_mins.length-1] + probe_length); }
  }

  public int getEnd() {
    // assumes child_mins has been sorted in ascending order
    if (forward) { return (child_mins[child_mins.length-1] + probe_length); }
    else { return child_mins[0]; }
  }

  public int getMin() {
    // assumes child_mins has been sorted in ascending order
    return child_mins[0];
  }

  public int getMax() {
    // assumes child_mins has been sorted in ascending order
    return (child_mins[child_mins.length-1] + probe_length);
  }

  public int getLength() { return (getMax() - getMin()); }
  public boolean isForward() { return forward; }
  public BioSeq getBioSeq() { return seq; }
  public double getStartDouble() { return (double)getStart(); }
  public double getEndDouble() { return (double)getEnd(); }
  public double getMinDouble() { return (double)getMin(); }
  public double getMaxDouble() { return (double)getMax(); }
  public double getLengthDouble() { return (double)getLength(); }
  public boolean isIntegral() { return true; }

  /**
   *  WARNING: The implementation of the Propertied (SymWithProps) interface in this class
   *  is incomplete and is very likely to change or be removed in future implementations.
   *  Returns a new Map instance with only two values:
   *  "method" maps to "HuEx-1_0-st-Probes"; and "id" maps to the value of getID().
   */
  public Map getProperties() {
    HashMap properties = new HashMap(1);
    if(props != null)  { properties.put("method", (String)props.get("method")); }
    properties.put("id", "" + this.getID());
    return properties;
  }

  /** Has no effect, and returns false. */
  public boolean setProperty(String key, Object val) {
    return false;
  }

  /** See getProperties(). */
  public Object getProperty(String key) {
    if ("method".equals(key) && props != null) { return (String)props.get("method"); }
    if ("id".equals(key)) return this.getID();
    else return null;
  }

  /** Returns a clone of the Map from getProperties(). */
  public Map cloneProperties() {
    return getProperties();
  }

}
