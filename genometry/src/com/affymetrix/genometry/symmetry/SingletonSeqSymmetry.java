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

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;

import java.util.*;

/**
 * A symmetry containing only a single span.
 * In other words, it has a "breadth" of one.
 */
public class SingletonSeqSymmetry extends SimpleMutableSeqSpan implements SeqSymmetry {

  protected java.util.List children;
  protected static int count = 1;

  public SingletonSeqSymmetry(SeqSpan span) {
    start = span.getStart();
    end = span.getEnd();
    seq = span.getBioSeq();
  }

  public SingletonSeqSymmetry(int start, int end, BioSeq seq) {
    super(start, end, seq);
  }
  
  public SeqSpan getSpan(BioSeq seq) {
    if (this.getBioSeq() == seq) { return this; }
    else { return null; }
  }

  public int getSpanCount() {
    return count;
  }

  public SeqSpan getSpan(int i) {
    if (i == 0) { return this; }
    else { return null; }
  }

  public BioSeq getSpanSeq(int i) {
    if (i == 0) { return seq; }
    else { return null; }
  }

  public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
    if (this.getBioSeq() == seq) {
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

  public int getChildCount() {
    if (null != children)
      return children.size();
    else
      return 0;
  }

  public SeqSymmetry getChild(int index) {
    if (null != children)
      return (SeqSymmetry)(children.get(index));
    else
      return null;
  }

  /** Returns null. */
  public String getID() { return null; }

}
