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

package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.Propertied;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.MutableDoubleSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.MutableSingletonSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleDerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import java.util.*;

/**
 *  Holds many static methods for manipulating BioSeqs, SeqSpans, and SeqSymmetries
 *
 */
public abstract class SeqUtils {

	private static final boolean DEBUG = false;

	/** Controls the format used for printing spans in {@link #spanToString(SeqSpan)}. */
	private static final boolean USE_SHORT_FORMAT_FOR_SPANS= true;

	/**
	 * Get depth of the symmetry. (Longest number of recursive calls to getChild()
	 *  required to reach deepest descendant)
	 */
	public static final int getDepth(SeqSymmetry sym) {
		return getDepth(sym, 1);
	}

	//  non-public method that does the recursive work for the getDepth(sym) call.
	private static final int getDepth(SeqSymmetry sym, int current_depth) {
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
	 *  Compares two spans, and returns true if they both refer to the same MutableAnnotatedBioSeq and
	 *      their starts and equal and their ends are equal.
	 */
	public static final boolean spansEqual(SeqSpan spanA, SeqSpan spanB) {
		return (spanA != null &&
				spanB != null &&
				spanA.getStartDouble() == spanB.getStartDouble() &&
				spanA.getEndDouble() == spanB.getEndDouble() &&
				spanA.getBioSeq() == spanB.getBioSeq());
	}



	private static final int getFirstNonNull(List<SeqSpan> spans) {
		int spanCount = spans.size();
		for (int i=0; i<spanCount; i++) {
			if (spans.get(i) != null) { return i; }
		}
		return -1;
	}


	private static final MutableSeqSpan mergeHelp(List<SeqSpan> spans, int index) {
		SeqSpan curSpan = spans.get(index);
		MutableSeqSpan result = new SimpleMutableSeqSpan(curSpan);
		boolean changed = true;
		while (changed) {
			changed = mergeHelp(spans, result);
		}
		return result;
	}

	private static final boolean mergeHelp(List<SeqSpan> spans, MutableSeqSpan result) {
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


	public static final List<SeqSpan> getLeafSpans(SeqSymmetry sym, MutableAnnotatedBioSeq seq) {
		ArrayList<SeqSpan> leafSpans = new ArrayList<SeqSpan>();
		collectLeafSpans(sym, seq, leafSpans);
		return leafSpans;
	}

	public static final void collectLeafSpans(SeqSymmetry sym, MutableAnnotatedBioSeq seq, Collection<SeqSpan> leafs) {
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

	public static final List<SeqSymmetry> getLeafSyms(SeqSymmetry sym) {
		ArrayList<SeqSymmetry> leafSyms = new ArrayList<SeqSymmetry>();
		collectLeafSyms(sym, leafSyms);
		return leafSyms;
	}

	private static final void collectLeafSyms(SeqSymmetry sym, Collection<SeqSymmetry> leafs) {
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
	private static final SeqSymmetry inverse(SeqSymmetry symA, MutableAnnotatedBioSeq seq) {
		return inverse(symA, seq, true);
	}

	/**
	 *  "Logical" NOT of SeqSymmetry (relative to a particular BioSeq).
	 *
	 *  @param include_ends indicates whether to extend to ends of BioSeq
	 */
	private static final SeqSymmetry inverse(SeqSymmetry symA, MutableAnnotatedBioSeq seq, boolean include_ends) {
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
	 *  Like a one-sided xor.
	 *  Creates a SeqSymmetry that contains children for regions covered by symA that
	 *     are not covered by symB.
	 */
	public static final SeqSymmetry exclusive(SeqSymmetry symA, SeqSymmetry symB, MutableAnnotatedBioSeq seq) {
		SeqSymmetry xorSym = xor(symA, symB, seq);
		return SeqUtils.intersection(symA, xorSym, seq);
	}

	/**
	 *  "Logical" XOR of SeqSymmetries (relative to a particular BioSeq).
	 */
	private static final SeqSymmetry xor(SeqSymmetry symA, SeqSymmetry symB, MutableAnnotatedBioSeq seq) {
		SeqSymmetry unionAB = union(symA, symB, seq);
		SeqSymmetry interAB = intersection(symA, symB, seq);
		SeqSymmetry inverseInterAB = inverse(interAB, seq);
		return intersection( unionAB, inverseInterAB, seq);
	}

	/**
	 *  "Logical" OR of SeqSymmetries (relative to a particular BioSeq).
	 */
	public static final MutableSeqSymmetry union(SeqSymmetry symA, SeqSymmetry symB, MutableAnnotatedBioSeq seq) {
		MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
		union(symA, symB, resultSym, seq);
		return resultSym;
	}

	/*public static final MutableSeqSymmetry union(List<SeqSymmetry> syms, MutableAnnotatedBioSeq seq) {
		MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
		union(syms, resultSym, seq);
		return resultSym;
	}*/

	/**
	 *  "Logical" OR of list of SeqSymmetries (relative to a particular BioSeq).
	 */
	public static final boolean union(List<SeqSymmetry> syms, MutableSeqSymmetry resultSym, MutableAnnotatedBioSeq seq) {
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
	public static final boolean union(SeqSymmetry symA, SeqSymmetry symB,
			MutableSeqSymmetry resultSym, MutableAnnotatedBioSeq seq) {
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
	public static final MutableSeqSymmetry intersection(SeqSymmetry symA, SeqSymmetry symB, MutableAnnotatedBioSeq seq) {
		MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
		intersection(symA, symB, resultSym, seq);
		return resultSym;
	}

	/**
	 *  "Logical" AND of SeqSymmetries (relative to a particular BioSeq).
	 */
	public static final boolean intersection(SeqSymmetry symA, SeqSymmetry symB,
			MutableSeqSymmetry resultSym,
			MutableAnnotatedBioSeq seq) {
		// Need to merge spans of symA with each other
		// and symB with each other (in case symA has overlapping
		// spans within itself, for example)
		List<SeqSpan> tempA = getLeafSpans(symA, seq);
		List<SeqSpan> tempB= getLeafSpans(symB, seq);
		MutableSeqSymmetry mergesymA = spanMerger(tempA);
		MutableSeqSymmetry mergesymB = spanMerger(tempB);

		List<SeqSpan> leavesA = getLeafSpans(mergesymA, seq);
		List<SeqSpan> leavesB = getLeafSpans(mergesymB, seq);

		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (SeqSpan spanA : leavesA) {
			if (spanA == null) { continue; }
			for (SeqSpan spanB : leavesB) {
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

		// now set resultSym to max and min of its children...
		SeqSpan resultSpan = new SimpleSeqSpan(min, max, seq);
		resultSym.addSpan(resultSpan);
		return true;
	}

	/** Inner class helper for inverse() method. */
	private static final class StartSorter implements Comparator<SeqSpan> {
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

	private static final MutableSeqSymmetry spanMerger(List<SeqSpan> spans) {
		MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
		spanMerger(spans, resultSym);
		return resultSym;
	}

	/**
	 * Merges spans into a SeqSymmetry.
	 * now ensures that spanMerger returns a resultSym whose children
	 *    are sorted relative to span.getBioSeq()
	 */
	private static final boolean spanMerger(List<SeqSpan> spans, MutableSeqSymmetry resultSym) {
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
		for (SeqSpan span : merged_spans) {
			MutableSingletonSeqSymmetry childSym =
				new MutableSingletonSeqSymmetry(span.getStart(), span.getEnd(), span.getBioSeq());
			min = Math.min(span.getMin(), min);
			max = Math.max(span.getMax(), max);
			resultSym.addChild(childSym);
		}
		MutableAnnotatedBioSeq seq;
		if (merged_spans.size() > 0) { seq = (merged_spans.get(0)).getBioSeq(); }
		else { seq = null; }
		SeqSpan resultSpan = new SimpleSeqSpan(min, max, seq);
		resultSym.addSpan(resultSpan);
		return true;
	}


	/**
	 * Return the first span encountered in sym that is _not_ same a given span.
	 */
	public static final SeqSpan getOtherSpan(SeqSymmetry sym, SeqSpan span) {
		int spanCount = sym.getSpanCount();
		for (int i=0; i<spanCount; i++) {
			if (! spansEqual(span, sym.getSpan(i))) {
				return sym.getSpan(i);
			}
		}
		return null;
	}

	/**
	 *  Returns the first MutableAnnotatedBioSeq in the SeqSymmetry that is NOT
	 *  equivalent to the given BioSeq.  (Compared using '=='.)
	 */
	public static final MutableAnnotatedBioSeq getOtherSeq(SeqSymmetry sym, MutableAnnotatedBioSeq seq) {
		int spanCount = sym.getSpanCount();
		for (int i=0; i<spanCount; i++) {
			MutableAnnotatedBioSeq testseq = sym.getSpan(i).getBioSeq();
			if (testseq != seq) {
				return testseq;
			}
		}
		return null;
	}


	/*public static final MutableSeqSymmetry flattenSymmetry(SeqSymmetry sym) {
		List<SeqSymmetry> leafSyms = SeqUtils.getLeafSyms(sym);
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
			SeqSymmetry child = leafSyms.get(k);
			result.addChild(child);
		}
		return result;
	}*/


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
	public static final boolean transformSymmetry(MutableSeqSymmetry resultSet, SeqSymmetry[] symPath) {
		// for each SeqSymmetry mapSym in SeqSymmetry[] symPathy
		for (int i=0; i<symPath.length; i++) {
			SeqSymmetry sym = symPath[i];
			boolean success = transformSymmetry(resultSet, sym, true);
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
public static final boolean transformSymmetry(MutableSeqSymmetry resultSym, SeqSymmetry mapSym) {
	//    return transformSymmetry(resultSym, mapSym, true);
	return transformSymmetry(resultSym, mapSym, true);
}

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
public static final boolean transformSymmetry(MutableSeqSymmetry resultSym, SeqSymmetry mapSym,
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
			loopThruMapSymChildren(mapSym, resultSym);
		}

		// is mapSym leaf?  yes
		else {
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

			// find a linker span first -- a span in resSym that has same MutableAnnotatedBioSeq as
			//    a span in mapSym
			SeqSpan linkSpan = null;
			SeqSpan mapSpan = null;
			for (int spandex=0; spandex < spanCount; spandex++) {
				//          SeqSpan mapspan = mapSym.getSpan(spandex);
				mapSpan = mapSym.getSpan(spandex);
				MutableAnnotatedBioSeq seq = mapSpan.getBioSeq();
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
			//    MutableAnnotatedBioSeq seqY
			else {  // have a linker span
				MutableSeqSpan interSpan = null;
				for (int spandex=0; spandex < spanCount; spandex++) {
					//            SeqSpan mapSpan = mapSym.getSpan(spandex);
					SeqSpan newspan = mapSym.getSpan(spandex);
					MutableAnnotatedBioSeq seq = newspan.getBioSeq();
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

					if (DEBUG) {
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
						//unsuccessful_count++;
						return false;
					}
				}
			}
		}

	}
	return true;
}



	private static void loopThruMapSymChildren(SeqSymmetry mapSym, MutableSeqSymmetry resultSym) {
		if (DEBUG) {
			System.out.println("looping through mapSym children");
		}
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
			for (int spandex = 0; spandex < spanCount; spandex++) {
				SeqSpan mapspan = map_child_sym.getSpan(spandex);
				MutableAnnotatedBioSeq seq = mapspan.getBioSeq();
				//            System.out.println(seq.getID());
				if (DEBUG) {
					System.out.print("MapSpan -- ");
					SeqUtils.printSpan(mapspan);
				}
				SeqSpan respan = resultSym.getSpan(seq);
				if (DEBUG) {
					System.out.print("ResSpan -- ");
					SeqUtils.printSpan(respan);
				}
				if (respan != null) {
					// GAH 6-12-2003 flipped intersection() span args around
					// shouldn't really matter, since later redoing intersectSpan based
					//     on respan orientation anyway...
					// SeqSpan intersectSpan = intersection(mapspan, respan);
					//              MutableSeqSpan intersectSpan = (MutableSeqSpan)intersection(mapspan, respan);
					MutableSeqSpan interSpan = (MutableSeqSpan) intersection(respan, mapspan);
					if (interSpan != null) {
						// GAH 6-12-2003
						// ensuring that orientation of intersectSpan is same as respan
						// (regardless of behavior of intersection());
						if (respan.isForward()) {
							interSpan.setDouble(interSpan.getMinDouble(), interSpan.getMaxDouble(), interSpan.getBioSeq());
						} else {
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
								((DerivedSeqSymmetry) childResult).setOriginalSymmetry(resultSym);
							} else {
								childResult = new SimpleMutableSeqSymmetry();
							}
						}
						childResult.addSpan(interSpan);
					}
				}
			}
			if (childResult == null) {
				if (DEBUG) {
					System.out.println("NO INTERSECTION, SKIPPING REST OF STEP 1 LOOP");
				}
				//            break STEP1_LOOP;
				continue;
			}
			if (DEBUG) {
				System.out.print("" + index + ", after Step 1a -- ");
				SeqUtils.printSymmetry(childResult);
			}
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
			if (DEBUG) {
				System.out.print("" + index + ", after Step 1b -- ");
				SeqUtils.printSymmetry(childResult);
			}
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
			if (DEBUG) {
				System.out.print("" + index + ", after Step 1c -- ");
				SeqUtils.printSymmetry(childResult);
			}
			resultSym.addChild(childResult);
		}
		// STEP 2
		addParentSpans(resultSym, mapSym);
	}


public static final List<SeqSymmetry> getOverlappingChildren(SeqSymmetry sym, SeqSpan ospan) {
	int childcount = sym.getChildCount();
	if (childcount == 0) { return null; }
	else {
		List<SeqSymmetry> results = null;
		MutableAnnotatedBioSeq oseq = ospan.getBioSeq();
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
protected static final boolean addParentSpans(MutableSeqSymmetry resultSym, SeqSymmetry mapSym) {
	int resultChildCount = resultSym.getChildCount();
	if (DEBUG)  { System.out.println("result child count = " + resultChildCount); }
	if (DEBUG) { System.out.print("resSym -- "); SeqUtils.printSymmetry(resultSym); }
	if (DEBUG) { System.out.print("mapSym -- "); SeqUtils.printSymmetry(mapSym); }
	// possibly want to add another branch here if resultSym has only one child --
	//      could "collapse up" by moving any spans in child that aren't in
	//      parent up to parent, and removing child
	if (resultChildCount > 0) {
		// for now, only worry about SeqSpans corresponding to (having same MutableAnnotatedBioSeq as)
		//    SeqSpans in _mapSym_ (rather than subMapSyms or subResSyms)
		int mapSpanCount = mapSym.getSpanCount();
		for (int spandex=0; spandex < mapSpanCount; spandex++) {
			SeqSpan mapSpan = mapSym.getSpan(spandex);
			MutableAnnotatedBioSeq mapSeq = mapSpan.getBioSeq();
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


/**
 *  Given a source SeqSpan srcSpan (which refers to a MutableAnnotatedBioSeq srcSeq), a destination MutableAnnotatedBioSeq dstSeq,
 *     and a SeqSymmetry sym that maps a span on srcSeq to a span on dstSeq, calculate and
 *     return the SeqSpan dstSpan on MutableAnnotatedBioSeq dstSeq that corresponds to the SeqSpan srcSpan on
 *     MutableAnnotatedBioSeq srcSeq.
 *
 *  Assumptions:
 *       no splitting of one span into multiple spans -- this is handled by transformSymmetry() methods
 *       never more than one SeqSpan with a given MutableAnnotatedBioSeq in the same SeqSymmetry
 *           (no MutableAnnotatedBioSeq duplications in SeqSymmetry)
 *       linearity
 *
 *  note that srcSpan and dstSpan may well be the same object...
 */
public static final boolean transformSpan(SeqSpan srcSpan, MutableSeqSpan dstSpan,
		MutableAnnotatedBioSeq dstSeq, SeqSymmetry sym) {

	// to increase efficiency of "compressed" SeqSymmetry implementations,
	//    should probably really do span = sym.getSpan(seq, scratch_mutable_span)...
	SeqSpan span1 = sym.getSpan(srcSpan.getBioSeq());
	SeqSpan span2 = sym.getSpan(dstSeq);

	// check to make sure that appropriate spans were actually found in the SeqSymmetry
	if (span1 == null || span2 == null) { return false; }

	// check to see that the span being transformed overlaps the span with
	//   same MutableAnnotatedBioSeq in the given SeqSymmetry
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
public static final boolean overlap(SeqSpan spanA, SeqSpan spanB) {
	return strictOverlap(spanA, spanB);
}

/**
 *  Return true if input spans overlap;
 *    orientation of spans is ignored.
 *    Loose means that abutment is considered overlap.
 *    WARNING: assumes both are on same sequence
 */
public static final boolean looseOverlap(SeqSpan spanA, SeqSpan spanB) {
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

private static final boolean looseOverlap(double AMin, double AMax, double BMin, double BMax) {
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
public static final boolean strictOverlap(SeqSpan spanA, SeqSpan spanB) {
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

private static final boolean strictOverlap(double AMin, double AMax, double BMin, double BMax) {
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
public static final boolean contains(SeqSpan spanA, SeqSpan spanB) {
	return ( (spanA.getMinDouble() <= spanB.getMinDouble()) &&
			(spanA.getMaxDouble() >= spanB.getMaxDouble()) );
}

/*public static final boolean contains(SeqSpan spanA, double point) {
	return ((spanA.getMinDouble() <= point) &&
			(spanA.getMaxDouble() >= point));
}*/


/**
 *  Semantic sugar atop overlap().
 */
public static final boolean intersects(SeqSpan spanA, SeqSpan spanB) {
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
public static final SeqSpan intersection(SeqSpan spanA, SeqSpan spanB) {
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
public static final boolean intersection(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan) {
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
/*public static final SeqSpan union(SeqSpan spanA, SeqSpan spanB) {
	return union(spanA, spanB, true);
}*/

/*public static final SeqSpan union(SeqSpan spanA, SeqSpan spanB, boolean use_strict_overlap) {
	//    MutableSeqSpan dstSpan = new SimpleMutableSeqSpan();
	MutableSeqSpan dstSpan = new MutableDoubleSeqSpan();
	if (union(spanA, spanB, dstSpan)) {
		return dstSpan;
	}
	else {
		return null;
	}
}*/



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
/*public static final boolean union(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan) {
	return union(spanA, spanB, dstSpan, true);
}*/

/**
 * Variant of making union of two spans,
 *   this one taking an additional boolean argument specifying whether to use
 *   strictOverlap() or looseOverlap().
 *   In other words, specifying whether "abutting but not overlapping" spans be merged.
 */
public static final boolean union(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan,
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
/*public static final SeqSpan encompass(SeqSpan spanA, SeqSpan spanB) {
	MutableSeqSpan dstSpan = new MutableDoubleSeqSpan();
	if (encompass(spanA, spanB, dstSpan)) {
		return dstSpan;
	}
	else {
		return null;
	}
}*/

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
public static final boolean encompass(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan) {
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

public static final boolean encompass(boolean AForward, boolean BForward,
		double AMin, double AMax, double BMin, double BMax,
		MutableAnnotatedBioSeq seq, MutableSeqSpan dstSpan) {

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

/**
 *  Copies a SeqSymmetry.
 *  Note that this clears all previous data from the MutableSeqSymmetry.
 */
public static final boolean copyToMutable(SeqSymmetry sym, MutableSeqSymmetry mut) {
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

public static final DerivedSeqSymmetry copyToDerived(SeqSymmetry sym) {
	DerivedSeqSymmetry mut = new SimpleDerivedSeqSymmetry();
	boolean success = copyToDerived(sym, mut);
	if (success) { return mut; }
	else { return null; }
}

public static final boolean copyToDerived(SeqSymmetry sym, DerivedSeqSymmetry der) {
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


public static final void printSpan(SeqSpan span) {
	System.out.println(spanToString(span));
}

public static final void printSymmetry(SeqSymmetry sym) {
	printSymmetry(sym, "  ");
}

public static final void printSymmetry(SeqSymmetry sym, String spacer) {
	printSymmetry(sym, spacer, false);
}

public static final void printSymmetry(SeqSymmetry sym, String spacer, boolean print_props) {
	printSymmetry("", sym, spacer, print_props);
}

// not public.  Used for recursion
private static final void printSymmetry(String indent, SeqSymmetry sym, String spacer, boolean print_props) {
	System.out.println(indent + symToString(sym));
	/*
	   if (sym instanceof DerivedSeqSymmetry) {
	   SeqSymmetry origsym = ((DerivedSeqSymmetry)sym).getOriginalSymmetry();
	   System.out.println("  derived from: " + symToString(origsym));
	   }
	   */
	if (print_props && sym instanceof Propertied) {
		Propertied pp = (Propertied) sym;
		Map<String,Object> props = pp.getProperties();
		if (props != null) {
			for (Map.Entry<String,Object> entry : props.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				System.out.println(indent + spacer + key + " --> " + value);
			}
		}
		else { System.out.println(indent + spacer + " no properties"); }
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
private static final java.text.DecimalFormat span_format = new java.text.DecimalFormat("#,###.###");

/** Provides a string representation of a SeqSpan.
 *  @see #USE_SHORT_FORMAT_FOR_SPANS
 */
public static final String spanToString(SeqSpan span) {
	if (USE_SHORT_FORMAT_FOR_SPANS) {
		if (span == null) { return "Span: null"; }
		MutableAnnotatedBioSeq seq = span.getBioSeq();
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
public static final String symToString(SeqSymmetry sym) {
	if (sym == null) {
		return "SeqSymmetry == null";
	}

	return "sym.getID() is not implemented.";
	
}


private static final int FORWARD = 5555;
private static final int REVERSE = 5556;
private static final int MAJORITY_RULE = 5557;

public static final SeqSpan getChildBounds(SeqSymmetry parent, MutableAnnotatedBioSeq seq) {
	return getChildBounds(parent, seq, MAJORITY_RULE);
}

private static final SeqSpan getChildBounds(SeqSymmetry parent, MutableAnnotatedBioSeq seq, int orientation) {
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

/*public static final int[] collectCounts(MutableAnnotatedBioSeq aseq) {
	int annotCount = aseq.getAnnotationCount();
	int[] countArray = {0, 0};
	for (int i=0; i<annotCount; i++) {
		SeqSymmetry sym = aseq.getAnnotation(i);
		countArray[0]++;
		collectCounts(sym, countArray);
	}
	return countArray;
}*/

/*private static final void collectCounts(SeqSymmetry sym, int[] countArray) {
	int spanCount = sym.getSpanCount();
	countArray[1] += spanCount;
	int childCount = sym.getChildCount();
	countArray[0] += childCount;
	for (int i=0; i<childCount; i++) {
		SeqSymmetry child = sym.getChild(i);
		collectCounts(child, countArray);
	}
}*/

public static final String getResidues(SeqSymmetry sym, MutableAnnotatedBioSeq seq) {
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
