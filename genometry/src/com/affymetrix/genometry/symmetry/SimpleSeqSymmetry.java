/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometry.symmetry;

import java.util.*;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.MutableSeqSpan;

public abstract class SimpleSeqSymmetry implements SeqSymmetry {

  protected List<SeqSpan> spans;
  protected List<SeqSymmetry> children = null;

  public SimpleSeqSymmetry() {
  }

  /*public SimpleSeqSymmetry(SeqSpan[] span_array) {
    this();
    if (span_array != null && span_array.length > 0) {
      this.spans = new ArrayList<SeqSpan>();
      // ? spans = Arrays.asList(span_array);
      for (int i=0; i<span_array.length; i++) {
        spans.add(span_array[i]);
      }
    }
  }*/

  /*public SimpleSeqSymmetry(SeqSpan spanA, SeqSpan spanB, SeqSpan[] cspans1, SeqSpan[] cspans2) {
    spans = new ArrayList<SeqSpan>(2);
    spans.add(spanA);
    spans.add(spanB);
    children = new ArrayList<SeqSymmetry>(2);
    for (int i=0; i<cspans1.length; i++) {
      SeqSpan span1 = cspans1[i];
      SeqSpan span2 = cspans2[i];
      SeqSymmetry childsym = new EfficientPairSeqSymmetry(span1, span2);
      children.add(childsym);
    }
  }

  public SimpleSeqSymmetry(List<SeqSpan> spans) {
    this();
    this.spans = spans;
  }*/

  public SeqSpan getSpan(BioSeq seq) {
    for (SeqSpan span : spans) {
      if (span.getBioSeq() == seq) {
        return span;
      }
    }
    return null;
  }

  public int getSpanCount() {
    if (spans == null) { return 0; }
    else  { return spans.size(); }
  }

  public SeqSpan getSpan(int i) {
    return spans.get(i);
  }

  public BioSeq getSpanSeq(int i) {
    SeqSpan sp = getSpan(i);
    if (null != sp) { return sp.getBioSeq(); }
    return null;
  }

  public boolean getSpan(int index, MutableSeqSpan span) {
    SeqSpan vspan = spans.get(index);
    span.set(vspan.getStart(), vspan.getEnd(), vspan.getBioSeq());
    return true;
  }

  public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
    for (SeqSpan vspan : spans) {
      if (vspan.getBioSeq() == seq) {
        span.set(vspan.getStart(), vspan.getEnd(), vspan.getBioSeq());
        return true;
      }
    }
    return false;
  }

  public int getChildCount() {
    if (null != children)
      return children.size();
    else
      return 0;
  }

  public SeqSymmetry getChild(int index) {
    if (null != children)
      return children.get(index);
    else
      return null;
  }

  public String getID() { return null; }

  /** Allows subclasses direct access to the children list. */
  protected List<SeqSymmetry> getChildren() {
    return children;
  }

}

