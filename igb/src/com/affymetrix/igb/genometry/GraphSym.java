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

package com.affymetrix.igb.genometry;

import java.util.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.igb.util.FloatList;

/**
 *  A SeqSymmetry for holding graph data.
 */
public class GraphSym extends SimpleSymWithProps implements Cloneable {
  int xcoords[];
  float ycoords[];
  BioSeq graph_original_seq;
  String graph_name;

  List thresh_names = null;
  FloatList thresh_vals = null;

  public Object clone() throws CloneNotSupportedException {
    GraphSym newsym = (GraphSym)super.clone();
    newsym.setGraphName(this.getGraphName() + ":clone");
    return newsym;
  }

  public GraphSym(int[] x, float[] y, String name, BioSeq seq) {
    super();
    this.xcoords = x;
    this.ycoords = y;
    this.graph_name = name;
    this.graph_original_seq = seq;
    // should at some point probably make seqspan shrink-wrap to bounds of xcoords...
    SeqSpan span = new SimpleSeqSpan(0, seq.getLength(), seq);
    this.addSpan(span);
  }

  public void addStoredThreshold(String thresh_name, float score_thresh) {
    if (thresh_names == null) { thresh_names = new ArrayList(); }
    if (thresh_vals == null) { thresh_vals = new FloatList(); }
    thresh_names.add(thresh_name);
    thresh_vals.add(score_thresh);
  }

  public int getStoredThreshCount() {
    if (thresh_vals == null) { return 0; }
    return thresh_vals.size();
  }

  public String getStoredThreshName(int i) {
    if (thresh_names == null) { return null; }
    return (String)thresh_names.get(i);
  }

  public float getStoredThreshValue(int i) {
    if (thresh_vals == null) { return Float.NEGATIVE_INFINITY; }
    return thresh_vals.get(i);
  }

  public void setGraphName(String name) {
    this.graph_name = name;
  }

  public String getGraphName() {
    return graph_name;
  }

  public void setGraphCoords(int[] x, float[] y) {
    this.xcoords = x;
    this.ycoords = y;
  }

  public int getPointCount() {
    if (xcoords == null) { return 0; }
    else { return xcoords.length; }
  }

  public int[] getGraphXCoords() {
    return xcoords;
  }

  public float[] getGraphYCoords() {
    return ycoords;
  }

  public void setGraphSeq(BioSeq seq) {
    this.graph_original_seq = seq;
  }

  /**
   *  Get the seq that the graph's xcoords are specified in
   */
  public BioSeq getGraphSeq() {
    return graph_original_seq;
  }

  /**
   *  Overriding request for property "method" to return graph name.
   */
  public Object getProperty(String key) {

    if (key.equals("method")) {
      return getGraphName();
    }
    else {
      return super.getProperty(key);
    }
  }
}
