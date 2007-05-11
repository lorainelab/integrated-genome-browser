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
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.MutableSeqSymmetry;

import java.util.*;


public class MutableSingletonSeqSymmetry
    extends SingletonSeqSymmetry
    implements MutableSeqSymmetry
{

  protected String id;

  public MutableSingletonSeqSymmetry(SeqSpan span) {
    super(span);
  }

  public MutableSingletonSeqSymmetry(int start, int end, BioSeq seq) {
    super(start, end, seq);
  }

  public MutableSingletonSeqSymmetry(String id, int start, int end, BioSeq seq) {
    this(start, end, seq);
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

  /**
   * Operation not allowed, it will throw an exception.
   */
  public void removeSpans() {
    throw new RuntimeException("can't removeSpans(), MutableSingletonSeqSymmetry is not mutable itself, only its children");
  }

  /**
   * Operation not allowed, it will throw an exception.
   */
  public void clear() {
    throw new RuntimeException("can't clear(), MutableSingletonSeqSymmetry is not mutable itself, only its children");
  }

  /**
   * Operation not allowed, it will throw an exception.
   */
  public void addSpan(SeqSpan span) { throw new
    RuntimeException("Operation Not Allowed. Can't add a span to a SingletonSeqSymmetry."); }

  /**
   * Operation not allowed, it will throw an exception.
   */
  public void removeSpan(SeqSpan span) { throw new
    RuntimeException("Operation Not Allowed. Can't remove a span froma a SingletonSeqSymmetry."); }

  /**
   * The setSpan() operation is not allowed, it will throw an exception.
   * The issue is that we can set our coordinates to that of the span, but
   * we can't set the span to _be_ the span, that is if someone calls setSpan
   * and then changes the span, we won't know about it since we aren't actually
   * setting the span, but rather copying its start and end.
   */
  public void setSpan(int index, SeqSpan span) {
    throw new RuntimeException("Operation Not Allowed. Can't set the span of a SingletonSeqSymmetry.");
  }

  public String getID() { return id; }

  /**
   *  Sorts child syms based on the given comparator.
   *  This method is not thread-safe if you try to access the
   *  children while sorting is happening.
   */
  public void sortChildren(Comparator<SeqSymmetry> comp)  {
    if (children == null) {
      return;
    }
    Collections.sort(children, comp);
  }
}

