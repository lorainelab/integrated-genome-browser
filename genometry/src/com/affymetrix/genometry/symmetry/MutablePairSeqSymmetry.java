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


import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.MutableSeqSymmetry;
import java.util.ArrayList;

public class MutablePairSeqSymmetry extends EfficientPairSeqSymmetry
  implements MutableSeqSymmetry {

  public MutablePairSeqSymmetry(SeqSpan spanA, SeqSpan spanB) {
    super(spanA, spanB);
  }

  public MutablePairSeqSymmetry(String id, SeqSpan spanA, SeqSpan spanB) {
    this(spanA, spanB);
    this.id = id;
  }

  public MutablePairSeqSymmetry(SeqSymmetry parent, SeqSpan spanA, SeqSpan spanB) {
    super(spanA, spanB);
    this.parent = parent;
  }

  public MutablePairSeqSymmetry(String id, SeqSymmetry parent, SeqSpan spanA, SeqSpan spanB) {
    this(parent, spanA, spanB);
    this.id = id;
  }

  public MutablePairSeqSymmetry(int startA, int endA, BioSeq seqA, int startB, int endB, BioSeq seqB) {
    super(startA, endA, seqA, startB, endB, seqB);
  }

  public MutablePairSeqSymmetry(SeqSymmetry parent, int startA, int endA, BioSeq seqA, int startB, int endB, BioSeq seqB) {
    super(parent, startA, endA, seqA, startB, endB, seqB);
  }

  public MutablePairSeqSymmetry(SeqSymmetry parent, String id, int startA, int endA, BioSeq seqA, int startB, int endB, BioSeq seqB) {
    super(parent, startA, endA, seqA, startB, endB, seqB);
    this.id = id;
  }

  public void addChild(SeqSymmetry sym) {
    if (children == null) {
      children = new ArrayList<SeqSymmetry>();
    }
    children.add(sym);
  }

  public void removeChild(SeqSymmetry sym) {
    children.remove(sym);
  }

  public void removeChildren() { children = null; }
  public void removeSpans() {
    throw new RuntimeException("can't removeSpans(), MutablePairSeqSymmetry is not mutable itself, only its children");
  }
  public void clear() {
    throw new RuntimeException("can't clear(), MutablePairSeqSymmetry is not mutable itself, only its children");
  }

  public void addSpan(SeqSpan span) { throw new
    RuntimeException("MutablePairSeqSymmetry is not mutable itself, only its children"); }
  public void removeSpan(SeqSpan span) { throw new
    RuntimeException("MutablePairSeqSymmetry is not mutable itself, only its children"); }

  public String getID() { return id; }

}


