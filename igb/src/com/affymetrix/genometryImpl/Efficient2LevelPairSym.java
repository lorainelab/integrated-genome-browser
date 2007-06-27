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

package com.affymetrix.genometryImpl;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.EfficientPairSeqSymmetry;

/**
 *  Efficient implementation of a basic SeqSymmetry that is
 *  of breadth 2 (2 SeqSpans per symmetry) and depth 2 (has one
 *  level of SeqSymmetry children.
 *
 *  Similar to UcscPslSym, but with most of the
 *    extras stripped away.
 */
public class Efficient2LevelPairSym implements SeqSymmetry {

  static int TARGET_INDEX = 0;
  static int QUERY_INDEX = 1;

  String id;
  String type;  // ???
  boolean qforward;
  BioSeq qseq;
  int qmin;
  int qmax;
  BioSeq tseq;
  int tmin;
  int tmax;
  int[] blockSizes;
  int[] qmins;
  int[] tmins;

  public Efficient2LevelPairSym(
		    String id,
		    String type,
		    boolean qforward,
		    BioSeq qseq,
		    int qmin,
		    int qmax,
		    BioSeq tseq,
		    int tmin,
		    int tmax,
		    int[] blockSizes,
		    int[] qmins,
		    int[] tmins
		    ) {
    this.id = id;
    this.type = type;
    this.qforward = qforward;
    this.qseq = qseq;
    this.qmin = qmin;
    this.qmax = qmax;
    this.tseq = tseq;
    this.tmin = tmin;
    this.tmax = tmax;
    this.blockSizes = blockSizes;
    this.qmins = qmins;
    this.tmins = tmins;
  }

  public String getType() { return type; }
  public String getID() { return id; }

  public SeqSpan getSpan(BioSeq bs) {
    SeqSpan span = null;
    if (bs.equals(tseq)) {
      if (qforward) { span = new SimpleSeqSpan(tmin, tmax, tseq); }
      else { span = new SimpleSeqSpan(tmax, tmin, tseq); }
    }
    else if (bs.equals(qseq)) {
      span = new SimpleSeqSpan(qmin, qmax, qseq);
    }
    return span;
  }

  public boolean getSpan(BioSeq bs, MutableSeqSpan span) {
    if (bs.equals(tseq)) {
      if (qforward) { span.set(tmin, tmax, tseq); }
      else { span.set(tmax, tmin, tseq); }
      return true;
    }
    else if (bs.equals(qseq)) {
      span.set(qmin, qmax, qseq);
    }
    return false;
  }

  public boolean getSpan(int index, MutableSeqSpan span) {
    if (index == TARGET_INDEX) {
      if (qforward) { span.set(tmin, tmax, tseq); }
      else { span.set(tmax, tmin, tseq); }
    }
    else if (index == QUERY_INDEX) {
      span.set(qmin, qmax, qseq);
    }
    return false;
  }

  /** Always returns 2. */
  public int getSpanCount() { return 2; }

  public SeqSpan getSpan(int index) {
    SeqSpan span = null;
    if (index == TARGET_INDEX) {
      if (qforward) { span = new SimpleSeqSpan(tmin, tmax, tseq); }
      else { span = new SimpleSeqSpan(tmax, tmin, tseq); }
    }
    else if (index == QUERY_INDEX) {
      span = new SimpleSeqSpan(qmin, qmax, qseq);
    }
    return span;
  }

  public BioSeq getSpanSeq(int index) {
    if (index == TARGET_INDEX) { return tseq; }
    else if (index == QUERY_INDEX) { return qseq; }
    return null;
  }

  public int getChildCount() { return blockSizes.length; }

  public SeqSymmetry getChild(int i) {
    if (qforward) {
      return new EfficientPairSeqSymmetry(tmins[i], tmins[i]+blockSizes[i], tseq,
					  qmins[i], qmins[i]+blockSizes[i], qseq);
    }
    else {
      return new EfficientPairSeqSymmetry(tmins[i] + blockSizes[i], tmins[i], tseq,
					  qmins[i], qmins[i]+blockSizes[i], qseq);
    }
  }


}
