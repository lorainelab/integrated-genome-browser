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

package com.affymetrix.genometry.symmetry;

import java.util.List;
import java.util.Vector;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.MutableSeqSpan;

public class SimpleSeqSymmetry implements SeqSymmetry {

  protected Vector spans;
  protected Vector children = null;

  public SimpleSeqSymmetry() {
  }

  public SimpleSeqSymmetry(SeqSpan[] span_array) {
    this();
    if (span_array != null && span_array.length > 0) {
      this.spans = new Vector();
      for (int i=0; i<span_array.length; i++) {
	spans.addElement(span_array[i]);
      }
    }
  }

  public SimpleSeqSymmetry(SeqSpan spanA, SeqSpan spanB, SeqSpan[] cspans1, SeqSpan[] cspans2) {
    spans = new Vector();
    spans.addElement(spanA);
    spans.addElement(spanB);
    children = new Vector();
    for (int i=0; i<cspans1.length; i++) {
      SeqSpan span1 = cspans1[i];
      SeqSpan span2 = cspans2[i];
      SeqSymmetry childsym = new EfficientPairSeqSymmetry(span1, span2);
      children.addElement(childsym);
    }
  }
  
  public SimpleSeqSymmetry(Vector spans) {
    this();
    this.spans = spans;
  }

  public SeqSpan getSpan(BioSeq seq) {
    int max = getSpanCount();
    SeqSpan span;
    for (int i=0; i<max; i++) {
      span = getSpan(i);
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
    return (SeqSpan)spans.elementAt(i);
  }
  
  public BioSeq getSpanSeq(int i) {
    SeqSpan sp = getSpan(i);
    if (null != sp) { return sp.getBioSeq(); }
    return null;
  }

  public boolean getSpan(int index, MutableSeqSpan span) {
    SeqSpan vspan = (SeqSpan)spans.elementAt(index);
    span.setStart(vspan.getStart());
    span.setEnd(vspan.getEnd());
    span.setBioSeq(vspan.getBioSeq());
    return true;
  }

  public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
    for (int i=0; i<spans.size(); i++) {
      SeqSpan vspan = (SeqSpan)spans.elementAt(i);
      if (vspan.getBioSeq() == seq) {
	span.setStart(vspan.getStart());
	span.setEnd(vspan.getEnd());
	span.setBioSeq(vspan.getBioSeq());
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
      return (SeqSymmetry)(children.elementAt(index));
    else
      return null;
  }

  public String getID() { return null; }

  /**  to allow subclass access to children list without addressing it directly */
  protected List getChildren() {
    return children;
  }

}

