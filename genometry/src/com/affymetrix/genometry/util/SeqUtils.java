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

package com.affymetrix.genometry.util;

import java.util.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;

/**
 *  Holds many static methods for manipulating BioSeqs, SeqSpans, and SeqSymmetries
 *
 */
public abstract class SeqUtils {

  public static boolean DEBUG = false;
  static boolean DEBUG_INTERSECTION = false;
  static boolean DEBUG_UNION = false;
  static boolean DEBUG_ROLLUP = false;

  /** Controls the format used for printing spans in {@link #spanToString(SeqSpan)}. */
  public static boolean USE_SHORT_FORMAT_FOR_SPANS= true;

  /** Controls the format used for printing spans in {@link #symToString(SeqSymmetry)}. */
  public static boolean USE_SHORT_FORMAT_FOR_SYMS= true;

  /**
   * Get depth of the symmetry. (Longest number of recursive calls to getChild()
   *  required to reach deepest descendant)
   */
  public static int getDepth(SeqSymmetry sym) {
    return getDepth(sym, 1);
  }

  //  non-public method that does the recursive work for the getDepth(sym) call.
  static int getDepth(SeqSymmetry sym, int current_depth) {
    int child_count = sym.getChildCount();
    if (child_count == 0) { return current_depth; }
    int max_child_depth = current_depth;
    // descend down into children to find max depth
    int next_depth = current_depth + 1;
    for (int i=child_count-1; i>=0; i--) {
      SeqSymmetry child = sym.getChild(i);
      int current_child_depth = getDepth(child, next_depth);
      max_child_depth = Math.max(max_child_depth, current_child_depth);
    }
    return max_child_depth;
  }

  /**
   *  Roll-up, or simplify, a Symmetry hierarchy.
   *  If the parent symmetry has only one child, and every span in the child has
   *     an equivalent span in the parent, then perform a rollup:
   *          detach the grandchild symmetries from the child, attach them
   *          to the parent
   *  Recurse down through the symmetry doing this.
   *  <p>
   *  <strong>WARNING -- ASSUMPTIONS</strong>
   *  <ul>
   *  <li>currently assumes all symmetries in the hierarchy are mutable --
   *     may give a ClassCastException if this is not the case...
   *  <li>currently assumes there are no symmetries in the hierarchy where
   *     one BioSeq appears in multiple SeqSpans (at the same level)
   *  </ul>
   */
  public static void rollup(MutableSeqSymmetry parent) {
    if (parent == null) { return; }
    int child_count = parent.getChildCount();
    if (child_count == 1) {
      MutableSeqSymmetry child = (MutableSeqSymmetry)parent.getChild(0);
      if (parent.getSpanCount() == child.getSpanCount()) {
        int span_count = child.getSpanCount();
        for (int i=0; i<span_count; i++) {
          SeqSpan child_span = child.getSpan(i);
          BioSeq seq = child_span.getBioSeq();
          SeqSpan parent_span = parent.getSpan(seq);
          if (!(SeqUtils.spansEqual(child_span, parent_span)))  {
            if (DEBUG_ROLLUP)  { System.out.println("failed at span check"); }
            return;
          }
        }
        // if made it through the for loop without returning, then
        //   do the actual rollup at this level
        parent.removeChild(child);
        int grandchild_count = child.getChildCount();
        for (int i=0; i<grandchild_count; i++) {
          MutableSeqSymmetry grandchild = (MutableSeqSymmetry)child.getChild(i);
          parent.addChild(grandchild);
        }
        if (grandchild_count > 0) {
          // recursively call on again on parent (which now has different children,
          //    original child has been removed and grandchildren have become
          //    new children
          rollup(parent);
        }
      }
    }
    else {
      // recursively call on children if have more than one child
      for (int i=0; i<child_count; i++) {
        MutableSeqSymmetry child = (MutableSeqSymmetry)parent.getChild(i);
        rollup(child);
      }
    }
  }

  /**
   *  "Trimming" a MutableSeqSymmetry based on leaf spans.
   *  end result is that (recursively) if a parent has span that extends further
   *  than the union of all its children, then the parent span is trimmed down
   *  to the union of all its children
   * <p>
   *  Currently assumes that all descendant symmetries are mutable,
   *     and that all spans in the symmetries are also mutable,
   *     and that there are not multiple spans with same seq in the same symmetry
   *<p>
   *  WARNING!!! trim() is also being used for post-transformation correction of
   *    a bug somewhere in transform methods that ends up getting strand oriention
   *    of spans in non-leaf symmetries wrong.  trim() fixes this because all
   *    spans in non-leaf symmetries are recursively recalculated based on leaf
   *    symmetries/spans via getChildBounds().  Since transform bug does not affect leafs,
   *    this corrects the bug.  But _really_ need to fix bug in transform machinery,
   *    since not all calls to tranformSymmetry() are followed by trim()... (and
   *    a post-operative fix is really inelegant)
   */
  public static void trim(MutableSeqSymmetry sym) {
    int child_count = sym.getChildCount();
    if (child_count <= 0) { return; }
    for (int i=0; i<child_count; i++) {
      MutableSeqSymmetry child = (MutableSeqSymmetry)sym.getChild(i);
      SeqUtils.trim(child);
    }
    int span_count = sym.getSpanCount();
    for (int k=0; k<span_count; k++) {
      MutableSeqSpan span = (MutableSeqSpan)sym.getSpan(k);
      BioSeq seq = span.getBioSeq();
      SeqSpan newspan = getChildBounds(sym, seq);
      // Throwing a null pointer exception with AnnotMapper during
      // hg16-hg17 mapping used for exon array. --steve chervitz
      if(newspan != null) {
        span.set(newspan.getStart(), newspan.getEnd(), seq);
      }
    }
  }


  /**
   *  Like spansEqual() but is strand-insensitive. (Spans can be considered equal
   *  even if on different strands.)
   */
  public static boolean spansEqualIgnoreStrand(SeqSpan spanA, SeqSpan spanB) {
    return (spanA != null &&
            spanB != null &&
            spanA.getMinDouble() == spanB.getMinDouble() &&
            spanA.getMaxDouble() == spanB.getMaxDouble() &&
            spanA.getBioSeq() == spanB.getBioSeq());
  }

  /**
   *  Compares two spans, and returns true if they both refer to the same BioSeq and
   *      their starts and equal and their ends are equal.
   */
  public static boolean spansEqual(SeqSpan spanA, SeqSpan spanB) {
    return (spanA != null &&
            spanB != null &&
            spanA.getStartDouble() == spanB.getStartDouble() &&
            spanA.getEndDouble() == spanB.getEndDouble() &&
            spanA.getBioSeq() == spanB.getBioSeq());
  }



  //  protected static int getFirstNonNull(SeqSpan[] spans) {
  protected static int getFirstNonNull(List spans) {
    //    for (int i=0; i<spans.length; i++) {
    int spanCount = spans.size();
    for (int i=0; i<spanCount; i++) {
      //      if (spans[i] != null) { return i; }
      if (spans.get(i) != null) { return i; }
    }
    return -1;
  }


  protected static MutableSeqSpan mergeHelp(List<SeqSpan> spans, int index) {
    SeqSpan curSpan = spans.get(index);
    MutableSeqSpan result = new SimpleMutableSeqSpan(curSpan);
    boolean changed = true;
    while (changed) {
      changed = mergeHelp(spans, result);
    }
    return result;
  }

  protected static boolean mergeHelp(List<SeqSpan> spans, MutableSeqSpan result) {
    boolean changed = false;
    int spanCount = spans.size();
    for (int i=0; i<spanCount; i++) {
      SeqSpan curSpan = spans.get(i);
      if (curSpan == null) { continue; };
      //  Specifying that union should use loose overlap...
      boolean overlap = SeqUtils.union(result, curSpan, result, false);
      if (overlap) {
        changed = true;
        spans.set(i, null);
      }
    }
    return changed;
  }


  public static List<SeqSpan> getLeafSpans(SeqSymmetry sym, BioSeq seq) {
    ArrayList<SeqSpan> leafSpans = new ArrayList<SeqSpan>();
    collectLeafSpans(sym, seq, leafSpans);
    return leafSpans;
  }

  public static void collectLeafSpans(SeqSymmetry sym, BioSeq seq, Collection<SeqSpan> leafs) {
    if (sym.getChildCount() == 0) {
      SeqSpan span = sym.getSpan(seq);
      if (span != null) {
        leafs.add(span);
      }
    }
    else  {
      int childCount = sym.getChildCount();
      for (int i=0; i<childCount; i++) {
        collectLeafSpans(sym.getChild(i), seq, leafs);
      }
    }
  }

  public static List getLeafSyms(SeqSymmetry sym) {
    ArrayList<SeqSymmetry> leafSyms = new ArrayList<SeqSymmetry>();
    collectLeafSyms(sym, leafSyms);
    return leafSyms;
  }

  public static void collectLeafSyms(SeqSymmetry sym, Collection<SeqSymmetry> leafs) {
    if (sym.getChildCount() == 0) {
      leafs.add(sym);
    }
    else  {
      int childCount = sym.getChildCount();
      for (int i=0; i<childCount; i++) {
        collectLeafSyms(sym.getChild(i), leafs);
      }
    }
  }



  /**
   *  "Logical" NOT of SeqSymmetry (relative to a particular BioSeq).
   *   Extends to ends of BioSeq.
   *  @see #inverse(SeqSymmetry, BioSeq, boolean)
   */
  public static SeqSymmetry inverse(SeqSymmetry symA, BioSeq seq) {
    return inverse(symA, seq, true);
  }

  /**
   *  "Logical" NOT of SeqSymmetry (relative to a particular BioSeq).
   *
   *  @param include_ends indicates whether to extend to ends of BioSeq
   */
  public static SeqSymmetry inverse(SeqSymmetry symA, BioSeq seq, boolean include_ends) {
    // put leaf syms in list
    List<SeqSpan> spans = SeqUtils.getLeafSpans(symA, seq);

    // merge any overlaps
    MutableSeqSymmetry mergedSym = spanMerger(spans);
    List<SeqSpan> mergedSpans = SeqUtils.getLeafSpans(mergedSym, seq);

    // order them based on start
    Collections.sort(mergedSpans, new SeqUtils.StartSorter());

    // invert and add to new SeqSymmetry
    //   for now ignoring the ends...
    int spanCount = mergedSpans.size();
    if (! include_ends )  {
      if (spanCount <= 1) {  return null; }  // no gaps, no resulting inversion
    }
    MutableSeqSymmetry invertedSym = new SimpleMutableSeqSymmetry();
    if (include_ends) {
      if (spanCount < 1) {
        // no spans, so just return sym of whole range of seq
        invertedSym.addSpan(new SimpleMutableSeqSpan(0, seq.getLength(), seq));
        return invertedSym;
      }
      else {
        SeqSpan firstSpan = mergedSpans.get(0);
        if (firstSpan.getMin() > 0) {
          SeqSymmetry beforeSym = new MutableSingletonSeqSymmetry(0, firstSpan.getMin(), seq);
          invertedSym.addChild(beforeSym);
        }
      }
    }
    for (int i=0; i<spanCount-1; i++) {
      SeqSpan preSpan = mergedSpans.get(i);
      SeqSpan postSpan = mergedSpans.get(i+1);
      SeqSymmetry gapSym =
        new MutableSingletonSeqSymmetry(preSpan.getMax(), postSpan.getMin(), seq);
      invertedSym.addChild(gapSym);
    }
    if (include_ends) {
      SeqSpan lastSpan = mergedSpans.get(spanCount-1);
      if (lastSpan.getMax() < seq.getLength()) {
        SeqSymmetry afterSym = new MutableSingletonSeqSymmetry(lastSpan.getMax(), seq.getLength(), seq);
        invertedSym.addChild(afterSym);
      }
    }
    if (include_ends) {
      invertedSym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
    }
    else {
      int min = (mergedSpans.get(0)).getMax();
      int max = (mergedSpans.get(spanCount-1)).getMin();
      invertedSym.addSpan(new SimpleSeqSpan(min, max, seq));
    }
    return invertedSym;
  }


  /**
   *  "Logical" XOR of SeqSymmetries (relative to a particular BioSeq).
   */
  public static SeqSymmetry xor(SeqSymmetry symA, SeqSymmetry symB, BioSeq seq) {
    SeqSymmetry unionAB = union(symA, symB, seq);
    SeqSymmetry interAB = intersection(symA, symB, seq);
    SeqSymmetry inverseInterAB = inverse(interAB, seq);
    return intersection( unionAB, inverseInterAB, seq);
  }

  /**
   *  Like a one-sided xor.
   *  Creates a SeqSymmetry that contains children for regions covered by symA that
   *     are not covered by symB.
   */
  public static SeqSymmetry exclusive(SeqSymmetry symA, SeqSymmetry symB, BioSeq seq) {
    SeqSymmetry xorSym = xor(symA, symB, seq);
    return SeqUtils.intersection(symA, xorSym, seq);
  }


  /**
   *  "Logical" OR of SeqSymmetries (relative to a particular BioSeq).
   */
  public static MutableSeqSymmetry union(SeqSymmetry symA, SeqSymmetry symB, BioSeq seq) {
    MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
    union(symA, symB, resultSym, seq);
    return resultSym;
  }

  public static MutableSeqSymmetry union(List<SeqSymmetry> syms, BioSeq seq) {
    MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
    union(syms, resultSym, seq);
    return resultSym;
  }

  /**
   *  "Logical" OR of list of SeqSymmetries (relative to a particular BioSeq).
   */
  public static boolean union(List<SeqSymmetry> syms, MutableSeqSymmetry resultSym, BioSeq seq) {
    resultSym.clear();
    List<SeqSymmetry> leaves = new ArrayList<SeqSymmetry>();
    for (SeqSymmetry sym : syms) {
      SeqUtils.collectLeafSyms(sym, leaves);
    }
    int leafCount = leaves.size();
    ArrayList<SeqSpan> spans = new ArrayList<SeqSpan>(leafCount);
    for (SeqSymmetry sym : leaves) {
      spans.add(sym.getSpan(seq));
    }
    spanMerger(spans, resultSym);
    return true;
  }



  /**
   *  "Logical" OR of SeqSymmetries (relative to a particular BioSeq).
   */
  public static boolean union(SeqSymmetry symA, SeqSymmetry symB,
                              MutableSeqSymmetry resultSym, BioSeq seq) {
    resultSym.clear();
    List<SeqSpan> spans = new ArrayList<SeqSpan>();
    collectLeafSpans(symA, seq, spans);
    collectLeafSpans(symB, seq, spans);
    spanMerger(spans, resultSym);
    return true;
  }

  /**
   *  "Logical" AND of SeqSymmetries (relative to a particular BioSeq).
   */
  public static MutableSeqSymmetry intersection(SeqSymmetry symA, SeqSymmetry symB, BioSeq seq) {
    MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
    intersection(symA, symB, resultSym, seq);
    return resultSym;
  }

  /**
   *  "Logical" AND of SeqSymmetries (relative to a particular BioSeq).
   */
  public static boolean intersection(SeqSymmetry symA, SeqSymmetry symB,
                                     MutableSeqSymmetry resultSym,
                                     BioSeq seq) {
    // Need to merge spans of symA with each other
    // and symB with each other (in case symA has overlapping
    // spans within itself, for example)
    List<SeqSpan> tempA = getLeafSpans(symA, seq);
    List<SeqSpan> tempB= getLeafSpans(symB, seq);
    MutableSeqSymmetry mergesymA = spanMerger(tempA);
    MutableSeqSymmetry mergesymB = spanMerger(tempB);

    List leavesA = getLeafSpans(mergesymA, seq);
    List leavesB = getLeafSpans(mergesymB, seq);

    int countA = leavesA.size();
    int countB = leavesB.size();
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    for (int i=0; i<countA; i++) {
      SeqSpan spanA = (SeqSpan)leavesA.get(i);
      if (spanA == null) { continue; }
      for (int k=0; k<countB; k++) {
        SeqSpan spanB = (SeqSpan)leavesB.get(k);
        if (spanB == null) { continue; }
        if (SeqUtils.overlap(spanA, spanB)) {
          SeqSpan spanI = SeqUtils.intersection(spanA, spanB);
          min = Math.min(spanI.getMin(), min);
          max = Math.max(spanI.getMax(), max);
          MutableSeqSymmetry symI = new SimpleMutableSeqSymmetry();
          symI.addSpan(spanI);
          resultSym.addChild(symI);
        }
      }
    }
    // if above didn't add any children to resultSym, then there is _no_
    //      intersection between symA and symB,     //      so return false
    if (resultSym.getChildCount() == 0) {
      return false;
    }
    /* testing for merging any overlaps...
    java.util.List ispans = getLeafSpans(resultSym, seq);
    SeqSymmetry merged_results = spanMerger(spans);
    java.util.List merged_ispans = getLeafSpans(merged_results, seq);
    etc.
    */

    // now set resultSym to max and min of its children...
    SeqSpan resultSpan = new SimpleSeqSpan(min, max, seq);
    resultSym.addSpan(resultSpan);
    return true;
  }

  /** Inner class helper for inverse() method. */
  static class StartSorter implements Comparator<SeqSpan> {
    static StartSorter static_instance = null;

    public static StartSorter getInstance() {
      if (static_instance == null) {
        static_instance = new StartSorter();
      }
      return static_instance;
    }

    public int compare(SeqSpan spanA, SeqSpan spanB) {
      int minA = spanA.getMin();
      int minB = spanB.getMin();
      if (minA < minB) { return -1; } else if (minA > minB) { return 1; } else { return 0; }  // equal
    }
  }

  protected static MutableSeqSymmetry spanMerger(List<SeqSpan> spans) {
    MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
    spanMerger(spans, resultSym);
    return resultSym;
  }

  /**
   * Merges spans into a SeqSymmetry.
   * now ensures that spanMerger returns a resultSym whose children
   *    are sorted relative to span.getBioSeq()
   */
  protected static boolean spanMerger(List<SeqSpan> spans, MutableSeqSymmetry resultSym) {
    int index;
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    // will probably be smaller, but specifying an initial capacity
    //   that won't be exceeded can be more efficient
    ArrayList<SeqSpan> merged_spans = new ArrayList<SeqSpan>(spans.size());

    while ((index = getFirstNonNull(spans)) > -1) {
      MutableSeqSpan span = mergeHelp(spans, index);

      merged_spans.add(span);
    }
    Collections.sort(merged_spans, SeqUtils.StartSorter.getInstance());
    for (int i=0; i<merged_spans.size(); i++) {
      SeqSpan span = merged_spans.get(i);
      MutableSingletonSeqSymmetry childSym =
        new MutableSingletonSeqSymmetry(span.getStart(), span.getEnd(), span.getBioSeq());
      min = Math.min(span.getMin(), min);
      max = Math.max(span.getMax(), max);
      resultSym.addChild(childSym);
    }
    BioSeq seq;
    if (merged_spans.size() > 0) { seq = (merged_spans.get(0)).getBioSeq(); }
    else { seq = null; }
    SeqSpan resultSpan = new SimpleSeqSpan(min, max, seq);
    resultSym.addSpan(resultSpan);
    return true;
  }


  /**
   * Return the first span encountered in sym that is _not_ same a given span.
   */
  public static SeqSpan getOtherSpan(SeqSymmetry sym, SeqSpan span) {
    int spanCount = sym.getSpanCount();
    for (int i=0; i<spanCount; i++) {
      if (! spansEqual(span, sym.getSpan(i))) {
        return sym.getSpan(i);
      }
    }
    return null;
  }

  /**
   *  Returns the first BioSeq in the SeqSymmetry that is NOT
   *  equivalent to the given BioSeq.  (Compared using '=='.)
   */
  public static BioSeq getOtherSeq(SeqSymmetry sym, BioSeq seq) {
    int spanCount = sym.getSpanCount();
    for (int i=0; i<spanCount; i++) {
      BioSeq testseq = sym.getSpan(i).getBioSeq();
      if (testseq != seq) {
        return testseq;
      }
    }
    return null;
  }

  /**
   *
   */
  public static SeqSpan getOtherSpan(SeqSymmetry sym, SeqSpan[] spans) {
    int spanCount = sym.getSpanCount();
    OUTERLOOP:
    for (int i=0; i<spanCount; i++) {
      SeqSpan span_to_check = sym.getSpan(i);
      for (int k=0; k<spans.length; k++) {
        if (spansEqual(span_to_check, spans[k])) {
          continue OUTERLOOP;
        }
      }
      return sym.getSpan(i);
    }
    return null;
  }


  public static MutableSeqSymmetry flattenSymmetry(SeqSymmetry sym) {
    List leafSyms = SeqUtils.getLeafSyms(sym);
    MutableSeqSymmetry result = new SimpleMutableSeqSymmetry();
    //  spans in result should be same as input
    //    (so won't bother with getChildBounds(), since then would
    //     have to check all children to figure out all possible span seqs)
    int spanCount = sym.getSpanCount();
    for (int i=0; i<spanCount; i++) {
      SeqSpan span = sym.getSpan(i);
      SeqSpan newspan = new SimpleSeqSpan(span.getStart(), span.getEnd(), span.getBioSeq());
      result.addSpan(newspan);
    }
    int leafCount = leafSyms.size();
    for (int k=0; k<leafCount; k++) {
      SeqSymmetry child = (SeqSymmetry)leafSyms.get(k);
      result.addChild(child);
    }
    return result;
  }


  /**
   *  Take a MutableSeqSymmetry and "collapse" it based on abutments of BioSeq seq.
   *
   *  NOT YET IMPLEMENTED
   */
  public static boolean mergeSymmetries(MutableSeqSymmetry symset) {
    /**
     * Hmm, this could get messy. Is it even possible to "merge" symmetries based on
     *   abutment of spans from one BioSeq?  Because what happens to the spans from other
     *   BioSeqs that _don't_ abut?
     *
     * Maybe need to redefine this as more limited in scope -- can "merge" two symmetries
     *   where:
     *     for _all_ BioSeqs that spans in the symmetries refer to:
     *        the span in each symmetry abut
     *        (and in same orientation?  is this even a meaningful question?)
     * I think this would work, but it still begs the question of how to deal with
     *      the (very common) situation where spans for a particular BioSeq in two
     *      symmetries (or more) abut, but other spans in the symmetries for other
     *      BioSeqs don't.  Maybe this is not an issue of merging, just something
     *      that we need to provide query methods for:
     *
     *     boolean abuts;
     *     abuts = span1.abuts(span2);
     *     OR abuts = SeqUtils.abut(span1, span2);
     *     abuts = sym1.abut(sym2, seq1);  // any abutments of seq1 betweeen sym1 and sym2
     *     OR abuts = SeqUtils.abut(sym1, sym2, seq1)
     *
     *  (should such methods recurse down if syms are symsets?)
     */

    // trying simple version first:
    //    assume children are already sorted (in ascending order by start coord)
    //    no recursion for children of children, etc.)
    // loop through children
    // for each child symmetry
    //   check against (all / next) child
    //   loop through spans of current child
    //   for each span, retrieve seq
    //       is nextchild.getSpan(seq) adjacent to span?
    //       AND result across all spans tested
    //   if all spans of current and next symmetry are adjacent,
    //       then merge the two symmetries
    //
    // merge children
    // if all children merge, should child "collapse" into parent?
    int childcount = symset.getChildCount();
    for (int cindex = 0; cindex < childcount; cindex++) {
      SeqSymmetry current = symset.getChild(cindex);
    }
    return false;
  }

  /**
   *  NOT YET IMPLEMENTED.
   *  Given a MutableSeqSymmetry (which can start with just a single SeqSpan),
   *    and an initial SeqSymmetry to use for coordinate mapping,
   *    attempt to map resultSet coordinates from srcSeq to dstSeq
   *
   *  Finds path of BioSeqs and SeqSymmetries leading from srcSeq to dstSeq,
   *    and calls transformSymmetry(resultSet, SeqSymmetry[] symPath, BioSeq seqPath)
   *
   * WARNING:
   *     if there are multiple paths from srcSeq to dstSeq (by way of
   *     SeqSymmetries, starting with initialSym), then will use first path
   *     found, which is not guaranteed to be reproducible!
   */
  public static boolean transformSymmetry(MutableSeqSymmetry resultSet,
                                          SeqSymmetry initialSym,
                                          BioSeq srcSeq, BioSeq dstSeq) {
    return false;
  }


  /**
   *  NOT YET FULLY IMPLEMENTED.
   *  recursive depth-first search to try and find path from srcSeq to dstSeq via
   *     AnnotatedSeqs and SeqSymmetries
   */
  public static boolean findPath(AnnotatedBioSeq srcSeq, BioSeq dstSeq,
                                 List<BioSeq> seqPath, List<SeqSymmetry> symmetryPath) {
    // Vector annots = srcSeq.getAnnotations();
    int annotCount = srcSeq.getAnnotationCount();
    for (int i=0; i<annotCount; i++) {
      // SeqSymmetry annot = (SeqSymmetry)annots.elementAt(i);
      SeqSymmetry annot = srcSeq.getAnnotation(i);
      int spanCount = annot.getSpanCount();
      for (int j=0; j<spanCount; j++) {
        SeqSpan curSpan = annot.getSpan(j);
        BioSeq curSeq = curSpan.getBioSeq();
        if (curSeq == dstSeq) {
          // SUCCESS!
          symmetryPath.add(annot);
          seqPath.add(curSeq);
          return true;
        }
        else {
          // recurse as part of depth-first search?
          if (curSeq instanceof AnnotatedBioSeq) {
            if (findPath((AnnotatedBioSeq)curSeq, dstSeq, seqPath, symmetryPath)) {
              symmetryPath.add(annot);
              seqPath.add(srcSeq);
              return true;
            }
          }
        }

      }

    }
    return false;

  }


  /**
   *  More general version of transformSymmetry(resultSet, SeqSymmetry[] syms, BioSeq[] seqs).
   *  In this version, try and calculate resultSet span(s) for all BioSeqs encountered in syms,
   *  not just the BioSeqs found in an input seqs Vector.
   *
   *  WARNING!!! GAH 5-21-2003
   *  There seems to be a bug somewhere in transform methods that ends up sometimes getting
   *    strand oriention of spans in non-leaf symmetries wrong.  This can be fixed by calling
   *    trim(resultSym) after the transformation.  But _really_ need to fix this in the
   *    tranform methods themselves rather than have a post-operative fix!
   */
  public static boolean transformSymmetry(MutableSeqSymmetry resultSet, SeqSymmetry[] symPath) {
    // for each SeqSymmetry mapSym in SeqSymmetry[] symPathy
    for (int i=0; i<symPath.length; i++) {
      SeqSymmetry sym = symPath[i];
      boolean success = transformSymmetry(resultSet, sym, true);
      if (! success) { return false; }
      if (DEBUG) { System.out.print("after symPath entry " + i + ", "); SeqUtils.printSymmetry(resultSet); System.out.println("---\n"); }
    }
    return true;
  }

  public static boolean transformSymmetry2(MutableSeqSymmetry resultSet, SeqSymmetry[] symPath) {
    // for each SeqSymmetry mapSym in SeqSymmetry[] symPath
    for (int i=0; i<symPath.length; i++) {
      SeqSymmetry sym = symPath[i];
      boolean success = transformSymmetry2(resultSet, sym, true);
      if (! success) { return false; }
      if (DEBUG) { System.out.print("after symPath entry " + i + ", "); SeqUtils.printSymmetry(resultSet); System.out.println("---\n"); }
    }
    return true;
  }

  /**
   *  Convenience method for transforming through just on mapSym rather than a path of
   *  multiple mapSyms.  Hides src2dst_recurse boolean, which is mainly for internal use
   *
   *  WARNING!!! GAH 5-21-2003
   *  There seems to be a bug somewhere in transform methods that ends up sometimes getting
   *    strand oriention of spans in non-leaf symmetries wrong.  This can be fixed by calling
   *    trim(resultSym) after the transformation.  But _really_ need to fix this in the
   *    tranform methods themselves rather than have a post-operative fix!
   */
  public static boolean transformSymmetry(MutableSeqSymmetry resultSym, SeqSymmetry mapSym) {
    //    return transformSymmetry(resultSym, mapSym, true);
    return transformSymmetry(resultSym, mapSym, true);
  }

  public static boolean transformSymmetry2(MutableSeqSymmetry resultSym, SeqSymmetry mapSym) {
    //    return transformSymmetry(resultSym, mapSym, true);
    return transformSymmetry2(resultSym, mapSym, true);
  }

  public static int unsuccessful_count = 0;
  public static boolean debug_step3 = false;
  /**
   *  More general version of
   *      transformSymmetry(resultSet,  src2dstSym, srcSeq, dstSeq, rootSeq, src2dst_recurse).
   *  in this version, try and calculate resultSet span(s) for all BioSeqs encountered in syms,
   *  not just rolling back to rootSeq and forward to dstSeq
   *
   *  WARNING!!! GAH 5-21-2003
   *  There seems to be a bug somewhere in transform methods that ends up sometimes getting
   *    strand oriention of spans in non-leaf symmetries wrong.  This can be fixed by calling
   *    trim(resultSym) after the transformation.  But _really_ need to fix this in the
   *    tranform methods themselves rather than have a post-operative fix!
   */
  public static boolean transformSymmetry(MutableSeqSymmetry resultSym, SeqSymmetry mapSym,
                                          boolean src2dst_recurse) {
    // is resultSym leaf?  no
    if (resultSym.getChildCount() > 0) {
      if (DEBUG) { System.out.println("resultSym has children"); }
      // STEP 4
      int resChildCount = resultSym.getChildCount();
      for (int child_index = 0; child_index < resChildCount; child_index++) {
        MutableSeqSymmetry childResSym = (MutableSeqSymmetry)resultSym.getChild(child_index);
        if (DEBUG) {
          System.out.println("working on resultSym child " + child_index);
          System.out.print("      "); SeqUtils.printSymmetry(childResSym);
        }
        transformSymmetry(childResSym, mapSym, src2dst_recurse);
        if (DEBUG) { System.out.println("done with resultSym child " + child_index); }
      }
      //
      // now run STEP 2...
      addParentSpans(resultSym, mapSym);
    }
    // is resultSym leaf?  yes
    else {
      //      if (DEBUG) { System.out.println("resultSym is leaf"); }
      // is mapSym leaf?  no
      if (src2dst_recurse &&
        mapSym.getChildCount() > 0) {

        if (DEBUG) { System.out.println("looping through mapSym children"); }
        //        if (DEBUG) { System.out.print("mapSym: "); SeqUtils.printSymmetry(mapSym); }
        // STEP 1
        int map_childcount = mapSym.getChildCount();

        STEP1_LOOP:
        for (int index = 0; index < map_childcount; index++) {
          SeqSymmetry map_child_sym = mapSym.getChild(index);
          MutableSeqSymmetry childResult = null;

          // STEP 1a
          // "sit still"
          // find the subset of BioSeqs that are pointed to by SeqSpans in both
          //    the map_child_sym (spanX) and the resultSym (spanY)
          // for each seqA of these BioSeqs, calculate SeqSpan spanZ, the intersection of
          //        spanX and the spanY
          //    if no intersection, keep looping
          //    if intersection exists, then add to child_resultSym
          int spanCount = 0;
          if (map_child_sym != null) {
            spanCount = map_child_sym.getSpanCount();
          }

          // WARNING: still need to switch to using mutable SeqSpan args for efficiency
          for (int spandex=0; spandex < spanCount; spandex++) {
            SeqSpan mapspan = map_child_sym.getSpan(spandex);
            BioSeq seq = mapspan.getBioSeq();
            //            System.out.println(seq.getID());
            if (DEBUG)  { System.out.print("MapSpan -- "); SeqUtils.printSpan(mapspan); }
            SeqSpan respan = resultSym.getSpan(seq);
            if (DEBUG)  { System.out.print("ResSpan -- "); SeqUtils.printSpan(respan); }
            if (respan != null) {
              // GAH 6-12-2003 flipped intersection() span args around
              // shouldn't really matter, since later redoing intersectSpan based
              //     on respan orientation anyway...
              // SeqSpan intersectSpan = intersection(mapspan, respan);
              //              MutableSeqSpan intersectSpan = (MutableSeqSpan)intersection(mapspan, respan);
              MutableSeqSpan interSpan = (MutableSeqSpan)intersection(respan, mapspan);

              if (interSpan != null) {
                // GAH 6-12-2003
                // ensuring that orientation of intersectSpan is same as respan
                // (regardless of behavior of intersection());
                if (respan.isForward()) {
                  interSpan.setDouble(interSpan.getMinDouble(), interSpan.getMaxDouble(), interSpan.getBioSeq());
                }
                else {
                  interSpan.setDouble(interSpan.getMaxDouble(), interSpan.getMinDouble(), interSpan.getBioSeq());
                }
                /*
                System.out.print("resultSym: "); printSymmetry(resultSym);
                System.out.print("mapSym: "); printSymmetry(map_child_sym);
s                System.out.print("intersect span: "); printSpan(interSpan);
                System.out.println("-------------------------");
                */

                if (childResult == null) {
                  // special-casing derived seq symmetries to preserve derivation info...
                  // hmm, maybe should just make this the normal case and always preserve
                  //    derivation info (if available...)
                  // NOT YET TESTED!!!
                  //    GAH 5-14-2002
                  if (mapSym instanceof DerivedSeqSymmetry) {
                    childResult = new SimpleDerivedSeqSymmetry();
                    ((DerivedSeqSymmetry)childResult).setOriginalSymmetry(resultSym);
                  }
                  else {
                    childResult = new SimpleMutableSeqSymmetry();
                  }
                }
                childResult.addSpan(interSpan);
              }
            }
          }
          if (childResult == null) {
            if (DEBUG)  { System.out.println("NO INTERSECTION, SKIPPING REST OF STEP 1 LOOP"); }
            //            break STEP1_LOOP;
            continue;
          }
          if (DEBUG)  { System.out.print("" + index + ", after Step 1a -- "); SeqUtils.printSymmetry(childResult); }

          // STEP 1b
          // "roll back"
          // find the subset of BioSeqs that are pointed to by a SeqSpan spanX in resultSym
          //      but not by any SeqSpan in childResult
          // for each seqA of these BioSeqs, calculate spanY by using resultSym as a mapping
          //      symmetry and childResult as the resultSet (but don't recurse...):
          // ACTUALLY, don't have to find subset -- this will happen in transformSymmetry!
          //      (which will fall through to STEP 3??)
          //      transformSymmetry(childResult, resultSym, false)
          transformSymmetry(childResult, resultSym, false);

          if (DEBUG)  { System.out.print("" + index + ", after Step 1b -- "); SeqUtils.printSymmetry(childResult); }

          // STEP 1c
          // "roll forward"
          //  find the subset of BioSeqs that are pointed to by a SeqSpan spanX in
          //     subMapSym but not by any SeqSpan in childResult (subResSym)
          //  for each SeqA of these BioSeqs, calculate spanY by using subMapSym as
          //     a mapping symmetry and childResult as the resultSet (with recursion)
          // ACTUALLY, don't have to find subset -- this will happen in transformSymmetry!
          //      (which will fall through to STEP 3?? -- not sure how well this plays with
          //       the recursion...)
          //      transformSymmetry(childResult, subMapSym, true)
          transformSymmetry(childResult, map_child_sym, true);

          if (DEBUG)  { System.out.print("" + index + ", after Step 1c -- "); SeqUtils.printSymmetry(childResult); }

          resultSym.addChild(childResult);
        }

        // STEP 2
        addParentSpans(resultSym, mapSym);

      }  // end of loop through mapSym children

      // is mapSym leaf?  yes
      else {  // STEP3
        // STEP 3
        //
        // GAH 2006-03-28
        // changed transformSymmetry() step3 to force _all_ spans to be trimmed to interspan, even
        //   if they are already present in result sym.  This fixes bug encountered with shallow transforms
        //   (result depth = 1, mapping depth = 1)
        // Not sure how this will affect deep transformations, but so far appears to be working properly

        int spanCount = mapSym.getSpanCount();
        boolean success = false;

        // WARNING: still need to switch to using mutable SeqSpan args for efficiency

        // find a linker span first -- a span in resSym that has same BioSeq as
        //    a span in mapSym
        SeqSpan linkSpan = null;
        SeqSpan mapSpan = null;
        for (int spandex=0; spandex < spanCount; spandex++) {
          //          SeqSpan mapspan = mapSym.getSpan(spandex);
          mapSpan = mapSym.getSpan(spandex);
          BioSeq seq = mapSpan.getBioSeq();
          SeqSpan respan = resultSym.getSpan(seq);
          if (respan != null) {
            linkSpan = respan;
            break;
          }
        }

        // if can't find a linker span, then there's a problem...
        if (linkSpan == null) {
          System.out.println("Ackkk! Can't find a linker span!!");
          // what should happen???
          return false;
        }

        // for each spanX in mapSym that has no SeqSpan in resultSym with same
        //    BioSeq seqY
        else {  // have a linker span
          MutableSeqSpan interSpan = null;
          for (int spandex=0; spandex < spanCount; spandex++) {
            //            SeqSpan mapSpan = mapSym.getSpan(spandex);
            SeqSpan newspan = mapSym.getSpan(spandex);
            BioSeq seq = newspan.getBioSeq();
            SeqSpan respan = resultSym.getSpan(seq);
            //            if (respan == null) {
            //      MutableSeqSpan newResSpan = (MutableSeqSpan)respan;
            // GAH 5-17-2003
            // problem here when linkSpan is not contained within mapSym.getSpan(linkSpan.getBioSeq())!!!
            // transforSpan will transform _as if_ above were the case, therefore giving incorrect results
            // trying to fix by always calculating intersection: (interSpan) of
            //  interSpan = intersection(linkSpan and mapSym.getSpan(linkSpan.getBioSeq())),
            //  and doing tranform with interSpan instead of linkSpan...
            //
            success = false;
            //              SeqSpan mapSpan = mapSym.getSpan(linkSpan.getBioSeq());
            //              MutableSeqSpan interSpan = (MutableSeqSpan)SeqUtils.intersection(linkSpan, mapSpan);
            if (interSpan == null) {
              interSpan = (MutableSeqSpan)SeqUtils.intersection(linkSpan, mapSpan);
              if (interSpan != null) {
                // GAH 6-12-2003
                // problem with using intersection!
                // since intersection() obliterates orientation (span returned is _always_ forward),
                //   need to set intersect_span orientation afterwards to be same as linkSpan
                //   (so it has coorect orientation regardless of behavior of intersection());
                if (linkSpan.isForward()) {
                  interSpan.setDouble(interSpan.getMinDouble(), interSpan.getMaxDouble(), interSpan.getBioSeq());
                }
                else {
                  interSpan.setDouble(interSpan.getMaxDouble(), interSpan.getMinDouble(), interSpan.getBioSeq());
                }
              }
            }
            MutableSeqSpan newResSpan = null;
            if (interSpan != null) {
              if (respan == null) { newResSpan = new MutableDoubleSeqSpan(); }
              else { newResSpan = (MutableSeqSpan)respan; }
              success = transformSpan(interSpan, newResSpan, seq, mapSym);
            }

            if (debug_step3) {
              System.out.println("in SeqUtils.transformSymmetry(), step3");
              System.out.print("  span to transform: "); printSpan(respan);
              System.out.print("  linkSpan:          "); printSpan(linkSpan);
              System.out.print("  mapping span:      "); printSpan(mapSpan);
              System.out.print("  intersection span: "); printSpan(interSpan);
              System.out.print("  transformed span:  "); printSpan(newResSpan);
            }
            if (success) {
              if ((respan == null) && (newResSpan != null))  {  // only add new span if respan was not reused for transformed span
                resultSym.addSpan(newResSpan);
              }
            }
            else {
              //                System.out.println("unsuccessful trying to transform span: ");
              unsuccessful_count++;
              return false;
            }
            /*      }
                    else {
                    if (debug_step3)  {
                    System.out.println("in SeqUtils.transformSymmetry(), step3");
                    System.out.print("  linkSpan:          "); printSpan(linkSpan);
                    System.out.println("respan already exists: " + SeqUtils.spanToString(respan));
                    }
                    }
            */
          }
        }
      }  // end of STEP 3

    }   // end of (resultSym leaf? -- yes) branch
    return true;
  }

  public static boolean transformSymmetry2(MutableSeqSymmetry resultSym, SeqSymmetry mapSym,
                                          boolean src2dst_recurse) {
    //    System.out.println("called SeqUtils.transformSymmetry2()");
    // is resultSym leaf?  no
    if (resultSym.getChildCount() > 0) {
      // STEP 4
      int resChildCount = resultSym.getChildCount();
      for (int child_index = 0; child_index < resChildCount; child_index++) {
        MutableSeqSymmetry childResSym = (MutableSeqSymmetry)resultSym.getChild(child_index);
        transformSymmetry2(childResSym, mapSym, src2dst_recurse);
      }
      addParentSpans(resultSym, mapSym);  // now run STEP 2...
    }
    // is resultSym leaf?  yes
    else {
      // is mapSym leaf?  no
      if (src2dst_recurse &&
        mapSym.getChildCount() > 0) {
        // STEP 1
        int map_childcount = mapSym.getChildCount();
        /**
         *  GAH 6-25-2003
         *  NOT YET IMPLEMENTED
         *  new attempt at improving performance for mapping syms that have large number of children...
         *    for each spanA in result sym
         *       collect List of mapsym children childmapB where
         *          childmapB.getSpan(panA.getBioSeq()) intersects spanA
         *          for IntervalSearchSyms, can do this via:
         *               childlist = sym.getIntersectedSymmetries(spanA) // sortof...
         */
        // first trying just deciding based on
        boolean USE_NEW_STEP1 = true;
        if (USE_NEW_STEP1) {
          List map_child_list = null;
          // find a seq shared by both (linkseq);
          int res_span_count = resultSym.getSpanCount();
          for (int i=0; i<res_span_count; i++) {
            // hmm, making some assumptions here:
            //   A. any BioSeq used for mapsym child sym (or descendant?) spans will also be used for
            //      a span in parent mapsym
            //   B. either (1) only one span in resultSym will share a seq with a mapSym span,
            //        OR   (2) multiple spans in resultSym share seqs with mapSym spans, but
            //                  for every child of mapSym either all of these seqs are shared or none of them are???
            SeqSpan respan = resultSym.getSpan(i);
            SeqSpan mapspan = mapSym.getSpan(respan.getBioSeq());
            if (mapspan != null) {
              if (mapSym instanceof SearchableSeqSymmetry) {
                map_child_list = ((SearchableSeqSymmetry)mapSym).getOverlappingChildren(respan);
                //                System.out.println("calling SearchableSeqSymmetry.getOverlappingChildren()");
              }
              else {
                map_child_list = SeqUtils.getOverlappingChildren(mapSym, respan);
                //                System.out.println("calling SeqUtils.getOverlappingChildren()");
              }
              break;
            }
          }
          if (map_child_list != null) {
            int new_child_count = map_child_list.size();

            NEW_STEP1_LOOP:
            for (int index = 0; index < new_child_count; index++) {  // start of loop through mapSym children
              //            SeqSymmetry map_child_sym = mapSym.getChild(index);
              SeqSymmetry map_child_sym = (SeqSymmetry)map_child_list.get(index);
              MutableSeqSymmetry childResult = null;
              // STEP 1a "sit still"
              int spanCount = map_child_sym.getSpanCount();
              for (int spandex=0; spandex < spanCount; spandex++) {
                SeqSpan mapspan = map_child_sym.getSpan(spandex);
                BioSeq seq = mapspan.getBioSeq();
                SeqSpan respan = resultSym.getSpan(seq);
                if (respan != null) {
                  MutableSeqSpan interSpan = (MutableSeqSpan)intersection(respan, mapspan);
                  if (interSpan != null) {
                    if (respan.isForward()) {
                      interSpan.setDouble(interSpan.getMinDouble(), interSpan.getMaxDouble(), interSpan.getBioSeq());
                    }
                    else {
                      interSpan.setDouble(interSpan.getMaxDouble(), interSpan.getMinDouble(), interSpan.getBioSeq());
                    }
                    if (childResult == null) {
                      // GAH 11-18-2003  trying to add DerivedSeqSymmetry tracking to transformSymmetry2
                      // not sure here if should check mapSym or resultSym, so for now propogating symmetry tracking
                      // if _either_ is a DerivedSeqSymmetry
                      if (mapSym instanceof DerivedSeqSymmetry || resultSym instanceof DerivedSeqSymmetry) {
                        // System.out.println("in SeqUtils.transformSymmetry2(), making derived seq symmetry");
                        childResult = new SimpleDerivedSeqSymmetry();
                        if (resultSym instanceof DerivedSeqSymmetry) {
                          SeqSymmetry osym = ((DerivedSeqSymmetry)resultSym).getOriginalSymmetry();
                          if (osym != null) {
                            ((DerivedSeqSymmetry)childResult).setOriginalSymmetry(osym);
                          }
                          else {
                            ((DerivedSeqSymmetry)childResult).setOriginalSymmetry(resultSym);
                          }
                        }
                        else {
                          ((DerivedSeqSymmetry)childResult).setOriginalSymmetry(resultSym);
                        }
                      }
                      else {
                        childResult = new SimpleMutableSeqSymmetry();
                      }
                      //                      childResult = new SimpleMutableSeqSymmetry();
                    }
                    childResult.addSpan(interSpan);
                  }
                }
              }
              if (childResult == null) {
                //            break STEP1_LOOP;
                continue;
              }
              // STEP 1b "roll back"
              //  GAH 6-27-2003
              // at this point, already know that both childResult and resultSym are leaf symmetries,
              //   (since childResult was just created and no children have been added to it,
              //    and childResult itself doesn't get added to resultSym till later, so resultSym is still leaf),
              //     so this transformSymmetry call will fall through to STEP3
              // therefore should probably break out STEP3 into its own method, and call directly here rather than
              //       calling transformSymmetry, to avoid confusion
              // actually I think this also implies that src2dst_recurse flag is no longer needed, since
              //    this is the _only_ place that it is called with va = false...
              transformSymmetry2(childResult, resultSym, false);
              // STEP 1c "roll forward"
              //  GAH 6-27-2003
              // at this point, already know that childResult is leaf symmetry, so this transformSymmetry
              //    call will fall through to map_child_sym.getChildCount() test:
              //    if map_child_sym has no children, will fall through to STEP3
              //    if map_child_sym has children, will recurse via STEP1
              transformSymmetry2(childResult, map_child_sym, true);
              resultSym.addChild(childResult);
            }  // end of loop through mapSym children
          }
        } // end of NEW_STEP_1
        else {
          STEP1_LOOP:
          for (int index = 0; index < map_childcount; index++) {  // start of loop through mapSym children
            SeqSymmetry map_child_sym = mapSym.getChild(index);
            MutableSeqSymmetry childResult = null;
            // STEP 1a "sit still"
            int spanCount = map_child_sym.getSpanCount();
            for (int spandex=0; spandex < spanCount; spandex++) {
              SeqSpan mapspan = map_child_sym.getSpan(spandex);
              BioSeq seq = mapspan.getBioSeq();
              SeqSpan respan = resultSym.getSpan(seq);
              if (respan != null) {
                MutableSeqSpan interSpan = (MutableSeqSpan)intersection(respan, mapspan);
                if (interSpan != null) {
                  if (respan.isForward()) {
                    interSpan.setDouble(interSpan.getMinDouble(), interSpan.getMaxDouble(), interSpan.getBioSeq());
                  }
                  else {
                    interSpan.setDouble(interSpan.getMaxDouble(), interSpan.getMinDouble(), interSpan.getBioSeq());
                  }
                  if (childResult == null) { childResult = new SimpleMutableSeqSymmetry(); }
                  childResult.addSpan(interSpan);
                }
              }
            }
            if (childResult == null) {
              //            break STEP1_LOOP;
              continue;
            }
            // STEP 1b "roll back"
            transformSymmetry2(childResult, resultSym, false);
            // STEP 1c "roll forward"
            transformSymmetry2(childResult, map_child_sym, true);
            resultSym.addChild(childResult);
          }  // end of loop through mapSym children
        }  // end of OLD STEP1
        // STEP 2
        addParentSpans(resultSym, mapSym);
      }
      // is mapSym leaf?  yes
      else {
        // STEP 3
        int spanCount = mapSym.getSpanCount();
        boolean success = false;
        SeqSpan linkSpan = null;
        for (int spandex=0; spandex < spanCount; spandex++) {
          SeqSpan mapspan = mapSym.getSpan(spandex);
          BioSeq seq = mapspan.getBioSeq();
          SeqSpan respan = resultSym.getSpan(seq);
          if (respan != null) {
            linkSpan = respan;
            break;
          }
        }
        // if can't find a linker span, then there's a problem...
        if (linkSpan == null) { System.out.println("Ackkk! Can't find a linker span!!"); return false; }
        else {  // have a linker span
          for (int spandex=0; spandex < spanCount; spandex++) {
            //            SeqSpan mapspan = mapSym.getSpan(spandex);
            SeqSpan newspan = mapSym.getSpan(spandex);
            BioSeq seq = newspan.getBioSeq();
            SeqSpan respan = resultSym.getSpan(seq);
            if (respan == null) {
              //              MutableSeqSpan newResSpan = new SimpleMutableSeqSpan();
              MutableSeqSpan newResSpan = new MutableDoubleSeqSpan();
              success = false;
              SeqSpan mapSpan = mapSym.getSpan(linkSpan.getBioSeq());
              MutableSeqSpan interSpan = (MutableSeqSpan)SeqUtils.intersection(linkSpan, mapSpan);
              if (interSpan != null) {
                if (linkSpan.isForward()) {
                  interSpan.setDouble(interSpan.getMinDouble(), interSpan.getMaxDouble(), interSpan.getBioSeq());
                }
                else {
                  interSpan.setDouble(interSpan.getMaxDouble(), interSpan.getMinDouble(), interSpan.getBioSeq());
                }
                success = transformSpan(interSpan, newResSpan, seq, mapSym);
              }
              if (success) {
                resultSym.addSpan(newResSpan);
              }
              else { return false; }
            }
          }
        }
      }  // end of STEP 3

    }   // end of (resultSym leaf? -- yes) branch
    return true;
  }

  public static List<SeqSymmetry> getOverlappingChildren(SeqSymmetry sym, SeqSpan ospan) {
    int childcount = sym.getChildCount();
    if (childcount == 0) { return null; }
    else {
      List<SeqSymmetry> results = null;
      BioSeq oseq = ospan.getBioSeq();
      for (int i=0; i<childcount; i++) {
        SeqSymmetry child = sym.getChild(i);
        SeqSpan cspan = child.getSpan(oseq);
        if (SeqUtils.overlap(ospan, cspan)) {
          if (results == null) { results = new ArrayList<SeqSymmetry>(); }
          results.add(child);
        }
      }
      return results;
    }
  }


  // breaking out STEP 2
  protected static boolean addParentSpans(MutableSeqSymmetry resultSym, SeqSymmetry mapSym) {
    int resultChildCount = resultSym.getChildCount();
    if (DEBUG)  { System.out.println("result child count = " + resultChildCount); }
    if (DEBUG) { System.out.print("resSym -- "); SeqUtils.printSymmetry(resultSym); }
    if (DEBUG) { System.out.print("mapSym -- "); SeqUtils.printSymmetry(mapSym); }
    // possibly want to add another branch here if resultSym has only one child --
    //      could "collapse up" by moving any spans in child that aren't in
    //      parent up to parent, and removing child
    if (resultChildCount > 0) {
      // for now, only worry about SeqSpans corresponding to (having same BioSeq as)
      //    SeqSpans in _mapSym_ (rather than subMapSyms or subResSyms)
      int mapSpanCount = mapSym.getSpanCount();
      for (int spandex=0; spandex < mapSpanCount; spandex++) {
        SeqSpan mapSpan = mapSym.getSpan(spandex);
        BioSeq mapSeq = mapSpan.getBioSeq();
        SeqSpan resSpan = resultSym.getSpan(mapSeq);

        // if no span in resultSym with same BioSeq, then need to create one based
        //    on encompass() of childResSym spans (if there are any...)
        if (resSpan == null) {
          if (DEBUG) { System.out.println("need to create new resSpan for seq " + mapSeq.getID()); }

          int forCount = 0;
          // need to use NEGATIVE_INFINITY for doubles, since Double.MIN_VALUE is really smallest
          //   _positive_ value, and may be transforming into negative coords...
          //          double min = Double.MAX_VALUE;
          //          double max = Double.MIN_VALUE;
          double min = Double.POSITIVE_INFINITY;
          double max = Double.NEGATIVE_INFINITY;
          boolean bounds_set = false;
          for (int childIndex=0; childIndex < resultChildCount; childIndex++) {
            SeqSymmetry childResSym = resultSym.getChild(childIndex);
            SeqSpan childResSpan = childResSym.getSpan(mapSeq);
            if (DEBUG) { System.out.print("child span -- "); SeqUtils.printSpan(childResSpan); }
            if (childResSpan != null) {
              min = Math.min(childResSpan.getMinDouble(), min);
              max = Math.max(childResSpan.getMaxDouble(), max);
              bounds_set = true;
              if (childResSpan.isForward()) { forCount++; }
              else { forCount--; }
              //              (childResSpan.isForward()) ? (forCount++) : (forCount--);
              if (DEBUG)  { System.out.println("Min: " + min + ", Max: " + max); }
            }
          }
          if (bounds_set) {  // only add parent span if bounds were set by child span...
            //          MutableSeqSpan newResSpan = new SimpleMutableSeqSpan();
            MutableSeqSpan newResSpan = new MutableDoubleSeqSpan();
            newResSpan.setBioSeq(mapSeq);
            if (forCount >= 0) {  // new result span should be forward
              newResSpan.setStartDouble(min);
              newResSpan.setEndDouble(max);
            }
            else {  // new result span should be reverse
              newResSpan.setStartDouble(max);
              newResSpan.setEndDouble(min);
            }
            if (DEBUG) {System.out.print("adding span to resSym -- "); SeqUtils.printSpan(newResSpan);}
            resultSym.addSpan(newResSpan);
          }  // END if (bounds_set)
        }
      }
    }  // end of STEP 2 _(if resultChildCount > 0)_
    return true;
  }


  /**  Sugar method for using same MutableSeqSpan as both source and destination for transform. */
  public static boolean transformSpan(MutableSeqSpan span, BioSeq dstSeq, SeqSymmetry sym) {
    return transformSpan(span, span, dstSeq, sym);
  }


  /**
   *  Given a source SeqSpan srcSpan (which refers to a BioSeq srcSeq), a destination BioSeq dstSeq,
   *     and a SeqSymmetry sym that maps a span on srcSeq to a span on dstSeq, calculate and
   *     return the SeqSpan dstSpan on BioSeq dstSeq that corresponds to the SeqSpan srcSpan on
   *     BioSeq srcSeq.
   *
   *  Assumptions:
   *       no splitting of one span into multiple spans -- this is handled by transformSymmetry() methods
   *       never more than one SeqSpan with a given BioSeq in the same SeqSymmetry
   *           (no BioSeq duplications in SeqSymmetry)
   *       linearity
   *
   *  note that srcSpan and dstSpan may well be the same object...
   */
  public static boolean transformSpan(SeqSpan srcSpan, MutableSeqSpan dstSpan,
                                      BioSeq dstSeq, SeqSymmetry sym) {

    // to increase efficiency of "compressed" SeqSymmetry implementations,
    //    should probably really do span = sym.getSpan(seq, scratch_mutable_span)...
    SeqSpan span1 = sym.getSpan(srcSpan.getBioSeq());
    SeqSpan span2 = sym.getSpan(dstSeq);

    // check to make sure that appropriate spans were actually found in the SeqSymmetry
    if (span1 == null || span2 == null) { return false; }

    // check to see that the span being transformed overlaps the span with
    //   same BioSeq in the given SeqSymmetry
    if (! overlap(srcSpan, span1)) {
      //      System.out.println("no overlap: ");
      return false;
    }
    // trying to optimize out scaling, since it's usually not necessary...
    // length check to determine if scaling is needed
    boolean needScaling = (span1.getLengthDouble() != span2.getLengthDouble());
    if (needScaling) {
      boolean opposite_spans = (span1.isForward() ^ span2.isForward());
      boolean resultForward = opposite_spans ^ srcSpan.isForward();
      double scale = span2.getLengthDouble() / span1.getLengthDouble();
      dstSpan.setBioSeq(dstSeq);

      double vstart, vend;

      if (opposite_spans) {
        vstart = (scale * (span1.getStartDouble() - srcSpan.getStartDouble())) + span2.getStartDouble();
        vend = (scale * (span1.getEndDouble() - srcSpan.getEndDouble())) + span2.getEndDouble();
      }
      else {
        vstart = (scale * (srcSpan.getStartDouble() - span1.getStartDouble())) + span2.getStartDouble();
        vend = (scale * (srcSpan.getEndDouble() - span1.getEndDouble())) + span2.getEndDouble();
      }
      if (resultForward) {
        dstSpan.setStartDouble(Math.min(vstart, vend));
        dstSpan.setEndDouble(Math.max(vstart, vend));
      }
      else {
        dstSpan.setStartDouble(Math.max(vstart, vend));
        dstSpan.setEndDouble(Math.min(vstart, vend));
      }
      return true;
    }

    else {   // scaling not needed, so using faster implementation
      if (span1.isForward() == span2.isForward()) {
        double offset = span2.getStartDouble() - span1.getStartDouble();
        dstSpan.setBioSeq(dstSeq);
        dstSpan.setStartDouble(srcSpan.getStartDouble() + offset);
        dstSpan.setEndDouble(srcSpan.getEndDouble() + offset);
        return true;
      }
      else {
        double offset = span2.getStartDouble() + span1.getStartDouble();
        dstSpan.setBioSeq(dstSeq);
        dstSpan.setStartDouble(offset - srcSpan.getStartDouble());
        dstSpan.setEndDouble(offset - srcSpan.getEndDouble());
        return true;
      }
    }

  }

  /**
   *  Return true if input spans overlap;
   *    orientation of spans is ignored.
   *    WARNING: assumes both are on same sequence
   *
   *    GAH 6-30-2003  changed overlap() to call strictOverlap() instead of looseOverlap()
   *        Should fix many issues with intersection(), etc. generating 0-length overlaps...
   *        And hopefully won't break anything! (I don't think there's anything relying on
   *           overlap() including abutment
   */
  public static boolean overlap(SeqSpan spanA, SeqSpan spanB) {
    return strictOverlap(spanA, spanB);
  }

  /**
   *  Return true if input spans overlap;
   *    orientation of spans is ignored.
   *    Loose means that abutment is considered overlap.
   *    WARNING: assumes both are on same sequence
   */
  public static boolean looseOverlap(SeqSpan spanA, SeqSpan spanB) {
    // should (overlap() should also check to make sure they are the same seq?)
    double AMin = spanA.getMinDouble();
    double BMin = spanB.getMinDouble();
    if (AMin >= BMin) {
      double BMax = spanB.getMaxDouble();
      return AMin <= BMax;
    } else {
      double AMax = spanA.getMaxDouble();
      return BMin <= AMax;
    }
  }

  static boolean looseOverlap(double AMin, double AMax, double BMin, double BMax) {
    if (AMin >= BMin) {
      return AMin <= BMax;
    } else {
      return BMin <= AMax;
    }
  }

  /**
   *    Exactly like looseOverlap(spanA, spanB), except exact abutment is not considered overlap.
   *    (&gt; and &lt; rather than &gt;= and &lt;= used for comparisons...).
   */
  public static boolean strictOverlap(SeqSpan spanA, SeqSpan spanB) {
    double AMin = spanA.getMinDouble();
    double BMin = spanB.getMinDouble();
    if (AMin >= BMin) {
      double BMax = spanB.getMaxDouble();
      return AMin < BMax;
    } else {
      double AMax = spanA.getMaxDouble();
      return BMin < AMax;
    }
  }

  static boolean strictOverlap(double AMin, double AMax, double BMin, double BMax) {
    if (AMin >= BMin) {
      return AMin < BMax;
    } else {
      return BMin < AMax;
    }
  }

  /**
   *  Returns true if the extent of spanB is contained within spanA.
   *    Note that this method currently does _not_ check to ensure that the SeqSpans
   *    refer to the same BioSeq.
   */
  public static boolean contains(SeqSpan spanA, SeqSpan spanB) {
    return ( (spanA.getMinDouble() <= spanB.getMinDouble()) &&
             (spanA.getMaxDouble() >= spanB.getMaxDouble()) );
  }

  public static boolean contains(SeqSpan spanA, double point) {
    return ((spanA.getMinDouble() <= point) &&
            (spanA.getMaxDouble() >= point));
  }

  /**
   *  Returns true if spanA and spanB are immediately adjacent on the same seq.
   *  for example:
   *        spanX == { seq1, 100, 200 }
   *        spanY == { seq1, 200, 250 }
   *   adjacent(spanX, spanY) = true
   *    note that this method currently does _not_ check to ensure that the SeqSpans
   *    refer to the same BioSeq
   *  NOT YET TESTED
   */
  public static boolean adjacent(SeqSpan spanA, SeqSpan spanB) {
    /*
    return ((spanA.getMax() == (spanB.getMin() + 1) ) ||
            (spanB.getMax() == (spanA.getMin() + 1) ) );
    */
    return (((spanB.getMinDouble() - spanA.getMaxDouble()) < 0.000001)  ||
            ((spanA.getMinDouble() - spanB.getMaxDouble()) < 0.00001) );

  }

  /**
   *  Semantic sugar atop overlap().
   */
  public static boolean intersects(SeqSpan spanA, SeqSpan spanB) {
    return overlap(spanA, spanB);
  }


  /**
   *  Return SeqSpan that is the intersection of spanA and spanB.

   *  returns null if spans aren't on same seq
   *  returns null if spans have no intersection (don't overlap)
   *  orientation of returned SeqSpan:
   *      forward if both spans are forward
   *      reverse if both spans are reverse
   *      (currently) same orientation as spanA, if spanA and spanB are in different orientations
   *
   *  Performance issues:
   *     new MutableSeqSpan creation -- to avoid, use intersection(span, span, mutspan) instead
   *
   */
  public static SeqSpan intersection(SeqSpan spanA, SeqSpan spanB) {
    if (! (overlap(spanA, spanB))) { return null; }
    MutableSeqSpan dstSpan = null;
    //    dstSpan = new SimpleMutableSeqSpan();
    dstSpan = new MutableDoubleSeqSpan();
    if (intersection(spanA, spanB, dstSpan)) {
      return dstSpan;
    }
    else {
      return null;
    }
  }

  /**
   *  More efficient method to retrieve intersection of two spans.
   *  returns the resulting span in dstSpan, and returns true if
   *     attempt to calculate intersection was a success
   *  (if couldn't calculate intersection, returns false, and dstSpan is unmodified)
   *  orientation of returned SeqSpan:
   *      forward if both spans are forward
   *      reverse if both spans are reverse
   *      (currently) same orientation as spanA, if spanA and spanB are in different orientations
   */
  public static boolean intersection(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan) {
    if (null == spanA || null ==spanB) { return false; }
    if (spanA.getBioSeq() != spanB.getBioSeq()) { return false; }
    if (! overlap(spanA, spanB)) { return false; }

    boolean AForward = spanA.isForward();
    boolean BForward = spanB.isForward();
    double start, end;
    if (AForward && BForward) {
      start = Math.max(spanA.getStartDouble(), spanB.getStartDouble());
      end = Math.min(spanA.getEndDouble(), spanB.getEndDouble());
    }
    else if ((! AForward) && (! BForward)) {
      start = Math.min(spanA.getStartDouble(), spanB.getStartDouble());
      end = Math.max(spanA.getEndDouble(), spanB.getEndDouble());
    }
    else {
      // for now, give priority to spanA...
      if (AForward) {
        start = Math.max(spanA.getStartDouble(), spanB.getEndDouble());
        end = Math.min(spanA.getEndDouble(), spanB.getStartDouble());
      }
      else {
        start = Math.min(spanA.getStartDouble(), spanB.getEndDouble());
        end = Math.max(spanA.getEndDouble(), spanB.getStartDouble());
      }
    }
    dstSpan.setStartDouble(start);
    dstSpan.setEndDouble(end);

    dstSpan.setBioSeq(spanA.getBioSeq());
    return true;
  }

  /**
   * Return SeqSpan that is the union of spanA and spanB.
   *  returns null if spans aren't on same seq
   *  returns null if spans don't overlap
   *  orientation of returned SeqSpan:
   *      forward if both spans are forward
   *      reverse if both spans are reverse
   *      (currently) same orientation as spanA, if spanA and spanB are in different orientations
   *
   *  Performance issues:
   *     new MutableSeqSpan creation -- to avoid, use union(span, span, mutspan) instead
   *
   */
  public static SeqSpan union(SeqSpan spanA, SeqSpan spanB) {
    return union(spanA, spanB, true);
  }

  public static SeqSpan union(SeqSpan spanA, SeqSpan spanB, boolean use_strict_overlap) {
    //    MutableSeqSpan dstSpan = new SimpleMutableSeqSpan();
    MutableSeqSpan dstSpan = new MutableDoubleSeqSpan();
    if (union(spanA, spanB, dstSpan)) {
      return dstSpan;
    }
    else {
      return null;
    }
  }



  /**
   *  More efficient method to retrieve union of two spans.
   *  returns the resulting span in dstSpan, and returns true if
   *     attempt to calculate union was a success
   *  (if couldn't calculate union, returns false, and dstSpan is unmodified)
   *  orientation of returned SeqSpan:
   *      forward if both spans are forward
   *      reverse if both spans are reverse
   *      (currently) same orientation as spanA, if spanA and spanB are in different orientations
   *
   *  WARNING 8-11-2003
   *    really need to decide whether to use strictOverlap() or looseOverlap() when
   *    doing union (in other words, should "abutting but not overlapping" spans be merged?
   *    maybe make this a boolean arg to the method?
   *
   */
  public static boolean union(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan) {
    return union(spanA, spanB, dstSpan, true);
  }

  /**
   * Variant of making union of two spans,
   *   this one taking an additional boolean argument specifying whether to use
   *   strictOverlap() or looseOverlap().
   *   In other words, specifying whether "abutting but not overlapping" spans be merged.
   */
  public static boolean union(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan,
                              boolean use_strict_overlap) {
    if (spanA.getBioSeq() != spanB.getBioSeq()) { return false; }
    boolean AForward = spanA.isForward();
    boolean BForward = spanB.isForward();
    double AMin, AMax, BMin, BMax;
    if (AForward) {
      AMin = spanA.getStartDouble();
      AMax = spanA.getEndDouble();
    } else {
      AMin = spanA.getEndDouble();
      AMax = spanA.getStartDouble();
    }

    if (BForward) {
      BMin = spanB.getStartDouble();
      BMax = spanB.getEndDouble();
    } else {
      BMin = spanB.getEndDouble();
      BMax = spanB.getStartDouble();
    }

    if (use_strict_overlap) {
      if (! strictOverlap(AMin, AMax, BMin, BMax)) { return false; }
    }
    else {
      if (! looseOverlap(AMin, AMax, BMin, BMax)) { return false; }
    }
    return encompass(AForward, BForward, AMin, AMax, BMin, BMax, spanA.getBioSeq(), dstSpan);
  }

  /**
   *  Just like {@link #union(SeqSpan, SeqSpan)}, except that
   *  if spans don't overlap, will still return a SeqSpan
   *  that encompasses min and max of the seq spans.
   *  orientation of returned SeqSpan:
   *      forward if both spans are forward
   *      reverse if both spans are reverse
   *      (currently) same orientation as spanA, if spanA and spanB are in different orientations
   *
   *  Performance issues:
   *    new MutableSeqSpan creation -- to avoid, use encompass(span, span, mutspan) instead
   */
  public static SeqSpan encompass(SeqSpan spanA, SeqSpan spanB) {
    MutableSeqSpan dstSpan = new MutableDoubleSeqSpan();
    if (encompass(spanA, spanB, dstSpan)) {
      return dstSpan;
    }
    else {
      return null;
    }
  }

  /**
   *  More efficient method to retrieve "encompass" of two spans.
   *  returns the resulting span in dstSpan, and returns true if
   *     attempt to calculate encompass was a success
   *  (if couldn't calculate encompass, returns false, and dstSpan is unmodified)
   *  orientation of returned SeqSpan:
   *      forward if both spans are forward
   *      reverse if both spans are reverse
   *      (currently) same orientation as spanA, if spanA and spanB are in different orientations
   */
  public static boolean encompass(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan) {
    if (spanA.getBioSeq() != spanB.getBioSeq()) { return false; }
    double start, end;
    final boolean AForward = spanA.isForward();
    final boolean BForward = spanB.isForward();
    if (AForward && BForward) {
      start = Math.min(spanA.getStartDouble(), spanB.getStartDouble());
      end = Math.max(spanA.getEndDouble(), spanB.getEndDouble());
    }
    else if ((! AForward ) && (! BForward )) {
      start = Math.max(spanA.getStartDouble(), spanB.getStartDouble());
      end = Math.min(spanA.getEndDouble(), spanB.getEndDouble());
    }
    else {
      // for now, give priority to spanA...
      if (AForward) {
        start = Math.min(spanA.getStartDouble(), spanB.getEndDouble());
        end = Math.max(spanA.getEndDouble(), spanB.getStartDouble());
      }
      else {
        start = Math.max(spanA.getStartDouble(), spanB.getEndDouble());
        end = Math.min(spanA.getEndDouble(), spanB.getStartDouble());
      }
    }
    dstSpan.setStartDouble(start);
    dstSpan.setEndDouble(end);
    dstSpan.setBioSeq(spanA.getBioSeq());
    return true;
  }

  public static boolean encompass(boolean AForward, boolean BForward,
    double AMin, double AMax, double BMin, double BMax,
    BioSeq seq, MutableSeqSpan dstSpan) {

    double start, end;
    if (AForward) {
      start = Math.min(AMin, BMin);
      end = Math.max(AMax, BMax);
    }
    else {
      start = Math.max(AMax, BMax);
      end = Math.min(AMin, BMin);
    }
    dstSpan.setStartDouble(start);
    dstSpan.setEndDouble(end);
    dstSpan.setBioSeq(seq);
    return true;
  }

  /** NOT YET TESTED */
  public static MutableSeqSymmetry copyToMutable(SeqSymmetry sym) {
    MutableSeqSymmetry mut = new SimpleMutableSeqSymmetry();
    boolean success = copyToMutable(sym, mut);
    if (success) { return mut; }
    else { return null; }
  }

  /**
   *  Copies a SeqSymmetry.
   *  Note that this clears all previous data from the MutableSeqSymmetry.
   */
  public static boolean copyToMutable(SeqSymmetry sym, MutableSeqSymmetry mut) {
    mut.clear();
    int spanCount = sym.getSpanCount();
    for (int i=0; i<spanCount; i++) {
      SeqSpan span = sym.getSpan(i);
      SeqSpan newspan = new SimpleMutableSeqSpan(span);
      mut.addSpan(newspan);
    }
    int childCount = sym.getChildCount();
    for (int i=0; i<childCount; i++) {
      SeqSymmetry child = sym.getChild(i);
      MutableSeqSymmetry newchild = new SimpleMutableSeqSymmetry();
      copyToMutable(child, newchild);
      mut.addChild(newchild);
    }
    return true;
  }

  public static DerivedSeqSymmetry copyToDerived(SeqSymmetry sym) {
    DerivedSeqSymmetry mut = new SimpleDerivedSeqSymmetry();
    boolean success = copyToDerived(sym, mut);
    if (success) { return mut; }
    else { return null; }
  }

  public static boolean copyToDerived(SeqSymmetry sym, DerivedSeqSymmetry der) {
    der.clear();
    if (sym instanceof DerivedSeqSymmetry) {
      der.setOriginalSymmetry(((DerivedSeqSymmetry)sym).getOriginalSymmetry());
    }
    else {
      der.setOriginalSymmetry(sym);
    }
    int spanCount = sym.getSpanCount();
    for (int i=0; i<spanCount; i++) {
      SeqSpan span = sym.getSpan(i);
      // probably can just point to spans instead of create new ones ???
      //    maybe make this an option...
      SeqSpan newspan = new SimpleMutableSeqSpan(span);
      der.addSpan(newspan);
    }
    int childCount = sym.getChildCount();
    for (int i=0; i<childCount; i++) {
      SeqSymmetry child = sym.getChild(i);
      //      MutableSeqSymmetry newchild = new SimpleMutableSeqSymmetry();
      DerivedSeqSymmetry newchild = new SimpleDerivedSeqSymmetry();
      copyToDerived(child, newchild);
      der.addChild(newchild);
    }
    return true;
  }



  //  public boolean

  /** NOT YET TESTED */
  public static boolean sameOrientation(SeqSpan spanA, SeqSpan spanB) {
    return (! (spanA.isForward() ^ spanB.isForward()));  // NOT XOR isForward()
  }

  public static void printSpan(SeqSpan span) {
    System.out.println(spanToString(span));
  }

  public static void printSymmetry(SeqSymmetry sym) {
    printSymmetry(sym, "  ");
  }

  public static void printSymmetry(SeqSymmetry sym, String spacer) {
    printSymmetry(sym, spacer, false);
  }

  public static void printSymmetry(SeqSymmetry sym, String spacer, boolean print_props) {
    printSymmetry("", sym, spacer, print_props);
  }

  // not public.  Used for recursion
  static void printSymmetry(String indent, SeqSymmetry sym, String spacer, boolean print_props) {
    System.out.println(indent + symToString(sym));
    /*
    if (sym instanceof DerivedSeqSymmetry) {
      SeqSymmetry origsym = ((DerivedSeqSymmetry)sym).getOriginalSymmetry();
      System.out.println("  derived from: " + symToString(origsym));
    }
     */
    if (print_props && sym instanceof Propertied) {
      Propertied pp = (Propertied) sym;
      Map props = pp.getProperties();
      if (props != null) {
        Iterator iter = props.entrySet().iterator();
        while (iter.hasNext()) {
          Map.Entry entry = (Map.Entry) iter.next();
          Object key = entry.getKey();
          Object value = entry.getValue();
          System.out.println(indent + spacer + key + " --> " + value);
        }
      }
    }
    for (int i=0; i<sym.getSpanCount(); i++) {
      SeqSpan span = sym.getSpan(i);
      System.out.println(indent + spacer  + spanToString(span));
    }
    for (int j=0; j<sym.getChildCount(); j++) {
      SeqSymmetry child_sym = sym.getChild(j);
      printSymmetry(indent + spacer, child_sym, spacer, print_props);
    }
  }

  // This DecimalFormat is used to insert commas between every three characters.
  static final java.text.DecimalFormat span_format = new java.text.DecimalFormat("#,###.###");

  /** Provides a string representation of a SeqSpan.
   *  @see #USE_SHORT_FORMAT_FOR_SPANS
   */
  public static String spanToString(SeqSpan span) {
    if (USE_SHORT_FORMAT_FOR_SPANS) {
      if (span == null) { return "Span: null"; }
      BioSeq seq = span.getBioSeq();
      return ((seq == null ? "nullseq" : seq.getID()) + ": [" +
        span_format.format(span.getMin()) + " - " + span_format.format(span.getMax()) +
        "] (" +
        ( span.isForward() ? "+" : "-") + span_format.format(span.getLength()) + ")"
        );
    }
    else {
      if (span == null) { return "Span: null"; }
      return ("Span: " +
        "min = " + span_format.format(span.getMin()) +
        ", max = " + span_format.format(span.getMax()) +
        ", length = " + span_format.format(span.getLength()) +
        ", forward = " + span.isForward() +
        ", seq = " + span.getBioSeq().getID() +  " " + span.getBioSeq());
    }
  }

  /** Provides a string representation of a SeqSpan.
   *  @see #USE_SHORT_FORMAT_FOR_SYMS
   */
  public static String symToString(SeqSymmetry sym) {
    String result = "";
    String id = sym.getID();
    if (sym == null) {
      result = "SeqSymmetry == null";
    }
    else if (USE_SHORT_FORMAT_FOR_SYMS) {
      String sym_class = sym.getClass().getName();
      int n = sym_class.lastIndexOf('.');
      if (n>0 && n<sym_class.length()) {
        sym_class = sym_class.substring(n+1);
      }
      String hex = Integer.toHexString(sym.hashCode());
      if (id == null) {
        result = sym_class + " ("+hex+")";
      }
      else {
        result = id + " : " + sym_class + " ("+hex+")";
      }
    } else {
      if (id == null) {
        result = sym + ", children: " + sym.getChildCount()
          + ", depth: "+ getDepth(sym);
      }
      else {
        result = id + " : " + sym + ", children: " + sym.getChildCount()
          + ", depth: "+ getDepth(sym);
      }
    }
    return result;
  }

  /*
  public static String spanToString(SeqSpan span) {
    if (span == null) { return "Span: null"; }
    return ("Span: seq = " + span.getBioSeq().getID() +
    //    return ("Span: seq = " + span.getBioSeq() +
            ", start = " + span.getStartDouble() + ", end = " + span.getEndDouble() +
            ", length = " + span.getLengthDouble() +
            ",  forward = " + span.isForward());
  }
  */

  /**
   * NOT YET IMPLEMENTED.
   */
  public static SeqSpan stretchSpan(SeqSpan orig_span, double nstart, double nend) {
    System.out.println("in stretchSpan(span, double, double) method");
    System.out.println("WARNING: this method doesn't do anything!");
    return null;
  }

  /**
   *  THIS DOESN'T SEEM TO WORK PROPERLY.    GAH 10-8-2001
   *  FOR NOW, USE CALLS TO SeqUtils.encompass(span, span) INSTEAD
   *  WARNING
   *  stretchSpan has not yet been changed to handle non-integer spans
   */
  public static SeqSpan stretchSpan(SeqSpan orig_span, int new_start, int new_end) {
//    System.out.println("in stretchSpan(span, int, int) method");
    //      System.out.println("STRETCHING: start:{" + new_start + " " + orig_span.getStart() + "} "
    //                        + " end:{" + new_end + " " +  orig_span.getEnd() + "}");

    if (new_end < new_start) {
      int temp = new_start;
      new_start = new_end;
      new_end = temp;
    }

    int orig_start = orig_span.getStart();
    int orig_end = orig_span.getEnd();
    if (orig_end < orig_start) {
      int temp = orig_start;
      orig_start = orig_end;
      orig_end = temp;
    }

    if (new_start < orig_start || new_end > orig_end) {
      if (orig_span.getEnd() < orig_span.getStart()) {
        return new SimpleMutableSeqSpan(
            new_start < orig_start ? new_start : orig_start,
            new_end > orig_end ? new_end : orig_end,
            orig_span.getBioSeq());
      } else {
        return new SimpleMutableSeqSpan(
            new_end > orig_end ? new_end : orig_end,
            new_start < orig_start ? new_start : orig_start,
            orig_span.getBioSeq());
      }
    } else {
      return orig_span;
    }
  }

  /**
   *  Similar to {@link #getChildBounds(SeqSymmetry, BioSeq)} except recursively descends down to the leaves
   *    to calculate the bounds, rather than just the immediate children.
   *  currently always returns a forward span
   */
  public static SeqSpan getLeafBounds(SeqSymmetry parent, BioSeq seq) {
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    MutableSeqSpan result = new SimpleMutableSeqSpan(max, min, seq);
    getLeafBounds(parent, seq, result);
    return result;
  }

  /**
   *  Helper function for {@link #getLeafBounds(SeqSymmetry, BioSeq)}.
   *  Similar to {@link #getChildBounds(SeqSymmetry, BioSeq)} except
   *    recursively descends down to the leaves
   *    to calculate the bounds, rather than just the immediate children.
   *  Currently always returns a forward span
   */
  public static void getLeafBounds(SeqSymmetry parent, BioSeq seq, MutableSeqSpan result) {
    int child_count = parent.getChildCount();
    if (child_count <= 0) {
      SeqSpan span = parent.getSpan(seq);
      result.setStart(Math.min(span.getMin(), result.getStart()));
      result.setEnd(Math.max(span.getMax(), result.getEnd()));
    }
    else {
      for (int i=0; i<child_count; i++) {
        SeqSymmetry child = parent.getChild(i);
        getLeafBounds(child, seq, result);
      }
    }
  }

  public static int FORWARD = 5555;
  public static int REVERSE = 5556;
  public static int MAJORITY_RULE = 5557;

  public static SeqSpan getChildBounds(SeqSymmetry parent, BioSeq seq) {
    return getChildBounds(parent, seq, MAJORITY_RULE);
  }

  public static SeqSpan getChildBounds(SeqSymmetry parent, BioSeq seq, int orientation) {
    int rev_count = 0;
    int for_count = 0;
    SeqSpan cbSpan = null;
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    int childCount = parent.getChildCount();
    boolean bounds_set = false;
    for (int i=0; i<childCount; i++) {
      SeqSymmetry childSym = parent.getChild(i);
      SeqSpan childSpan = childSym.getSpan(seq);
      if (childSpan != null) {
        min = Math.min(min, childSpan.getMin());
        max = Math.max(max, childSpan.getMax());
        if (childSpan.isForward()) { for_count++; }
        else { rev_count++; }
        bounds_set = true;
      }
    }
    if (bounds_set) {
      // if majority of children are forward, then childBounds is forward,
      // if majority of children are reverse, then childBounds is reverse,
      if (orientation == MAJORITY_RULE) {
        if (for_count >= rev_count) {
          cbSpan = new SimpleSeqSpan(min, max, seq);
        }
        else {
          cbSpan = new SimpleSeqSpan(max, min, seq);
        }
      }
      else if (orientation == FORWARD) {
        cbSpan = new SimpleSeqSpan(min, max, seq);
      }
      else if (orientation == REVERSE) {
        cbSpan = new SimpleSeqSpan(max, min, seq);
      }
      else {
        throw new RuntimeException("orientation arg to SeqUtils.getChildBounds() must be " +
                                   "FORWARD, REVERSE, or MAJORITY_RULE");
      }
    }
    return cbSpan;
  }

  public static int[] collectCounts(AnnotatedBioSeq aseq) {
    int annotCount = aseq.getAnnotationCount();
    int[] countArray = {0, 0};
    for (int i=0; i<annotCount; i++) {
      SeqSymmetry sym = aseq.getAnnotation(i);
      countArray[0]++;
      collectCounts(sym, countArray);
    }
    return countArray;
  }

  public static void collectCounts(SeqSymmetry sym, int[] countArray) {
    int spanCount = sym.getSpanCount();
    countArray[1] += spanCount;
    int childCount = sym.getChildCount();
    countArray[0] += childCount;
    for (int i=0; i<childCount; i++) {
      SeqSymmetry child = sym.getChild(i);
      collectCounts(child, countArray);
    }
  }

  public static String getResidues(SeqSymmetry sym, BioSeq seq) {
    String result = null;
    int childcount = sym.getChildCount();
    if (childcount > 0) {
      result = "";
      for (int i=0; i<childcount; i++) {
        SeqSymmetry child = sym.getChild(i);
        String child_result = getResidues(child, seq);
        if (child_result == null) {
          result = null;
          break;
        }
        else {
          result += child_result;
        }
      }
    }
    else {
      SeqSpan span = sym.getSpan(seq);
      if ( (span != null) ) {
        result = seq.getResidues(span.getStart(), span.getEnd());
      }
    }
    return result;
  }

}
