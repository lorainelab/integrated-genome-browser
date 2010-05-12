package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.comparator.SeqSpanComparator;
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

	/** Controls the format used for printing spans in {@link #spanToString(SeqSpan)}. */
	private static final boolean USE_SHORT_FORMAT_FOR_SPANS= true;


	// This DecimalFormat is used to insert commas between every three characters.
	private static final java.text.DecimalFormat span_format = new java.text.DecimalFormat("#,###.###");

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
	 *  Compares two spans, and returns true if they both refer to the same BioSeq and
	 *      their starts and equal and their ends are equal.
	 */
	public static final boolean spansEqual(SeqSpan spanA, SeqSpan spanB) {
		return (spanA != null &&
				spanB != null &&
				spanA.getStartDouble() == spanB.getStartDouble() &&
				spanA.getEndDouble() == spanB.getEndDouble() &&
				spanA.getBioSeq() == spanB.getBioSeq());
	}



	public static final List<SeqSpan> getLeafSpans(SeqSymmetry sym, BioSeq seq) {
		List<SeqSpan> leafSpans = new ArrayList<SeqSpan>();
		collectLeafSpans(sym, seq, leafSpans);
		return leafSpans;
	}

	public static final void collectLeafSpans(SeqSymmetry sym, BioSeq seq, Boolean isForward, Collection<SeqSpan> leafs) {
		if (sym.getChildCount() == 0) {
			SeqSpan span = sym.getSpan(seq);
			if (span != null && span.isForward() == isForward) {
				leafs.add(span);
			}
		}
		else  {
			int childCount = sym.getChildCount();
			for (int i=0; i<childCount; i++) {
				collectLeafSpans(sym.getChild(i), seq, isForward, leafs);
			}
		}
	}

	public static final void collectLeafSpans(SeqSymmetry sym, BioSeq seq, Collection<SeqSpan> leafs) {
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
		List<SeqSymmetry> leafSyms = new ArrayList<SeqSymmetry>();
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


	private static void addInvertChildren(List<SeqSpan> mergedSpans, BioSeq seq, MutableSeqSymmetry invertedSym, int spanCount) {
		SeqSpan firstSpan = mergedSpans.get(0);
		if (firstSpan.getMin() > 0) {
			SeqSymmetry beforeSym = new MutableSingletonSeqSymmetry(0, firstSpan.getMin(), seq);
			invertedSym.addChild(beforeSym);
		}
		for (int i = 0; i < spanCount - 1; i++) {
			SeqSpan preSpan = mergedSpans.get(i);
			SeqSpan postSpan = mergedSpans.get(i + 1);
			SeqSymmetry gapSym = new MutableSingletonSeqSymmetry(preSpan.getMax(), postSpan.getMin(), seq);
			invertedSym.addChild(gapSym);
		}
		SeqSpan lastSpan = mergedSpans.get(spanCount - 1);
		if (lastSpan.getMax() < seq.getLength()) {
			SeqSymmetry afterSym = new MutableSingletonSeqSymmetry(lastSpan.getMax(), seq.getLength(), seq);
			invertedSym.addChild(afterSym);
		}
	}


	/**
	 *  Like a one-sided xor.
	 *  Creates a SeqSymmetry that contains children for regions covered by symA that
	 *     are not covered by symB.
	 */
	public static final SeqSymmetry exclusive(SeqSymmetry symA, SeqSymmetry symB, BioSeq seq) {
		SeqSymmetry xorSym = xor(symA, symB, seq);
		return SeqUtils.intersection(symA, xorSym, seq);
	}

	/**
	 *  "Logical" XOR of SeqSymmetries (relative to a particular BioSeq).
	 */
	private static final SeqSymmetry xor(SeqSymmetry symA, SeqSymmetry symB, BioSeq seq) {
		SeqSymmetry unionAB = union(symA, symB, seq);
		SeqSymmetry interAB = intersection(symA, symB, seq);
		SeqSymmetry inverseInterAB = inverse(interAB, seq);
		return intersection( unionAB, inverseInterAB, seq);
	}


	/**
	 *  "Logical" NOT of SeqSymmetry (relative to a particular BioSeq).
	 */
	private static final SeqSymmetry inverse(SeqSymmetry symA, BioSeq seq) {
		// put leaf syms in list
		List<SeqSpan> spans = SeqUtils.getLeafSpans(symA, seq);

		// merge any overlaps
		MutableSeqSymmetry mergedSym = spanMerger(spans);
		List<SeqSpan> mergedSpans = SeqUtils.getLeafSpans(mergedSym, seq);

		// order them based on start
		Collections.sort(mergedSpans, new SeqSpanComparator());

		// invert and add to new SeqSymmetry
		//   for now ignoring the ends...
		MutableSeqSymmetry invertedSym = new SimpleMutableSeqSymmetry();

		int spanCount = mergedSpans.size();
		if (spanCount > 0) {
			addInvertChildren(mergedSpans, seq, invertedSym, spanCount);
		}

		invertedSym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));

		return invertedSym;
	}



	/**
	 *  "Logical" OR of SeqSymmetries (relative to a particular BioSeq).
	 */
	public static final MutableSeqSymmetry union(SeqSymmetry symA, SeqSymmetry symB, BioSeq seq) {
		MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
		union(symA, symB, resultSym, seq);
		return resultSym;
	}

	/**
	 *  "Logical" OR of list of SeqSymmetries (relative to a particular BioSeq).
	 */
	public static final void union(List<SeqSymmetry> syms, MutableSeqSymmetry resultSym, BioSeq seq) {
		resultSym.clear();
		List<SeqSymmetry> leaves = new ArrayList<SeqSymmetry>();
		for (SeqSymmetry sym : syms) {
			SeqUtils.collectLeafSyms(sym, leaves);
		}
		int leafCount = leaves.size();
		List<SeqSpan> spans = new ArrayList<SeqSpan>(leafCount);
		for (SeqSymmetry sym : leaves) {
			spans.add(sym.getSpan(seq));
		}
		spanMerger(spans, resultSym);
	}



	/**
	 *  "Logical" OR of SeqSymmetries (relative to a particular BioSeq).
	 */
	private static final void union(SeqSymmetry symA, SeqSymmetry symB,
			MutableSeqSymmetry resultSym, BioSeq seq) {
		resultSym.clear();
		List<SeqSpan> spans = new ArrayList<SeqSpan>();
		collectLeafSpans(symA, seq, spans);
		collectLeafSpans(symB, seq, spans);
		spanMerger(spans, resultSym);
	}

	/**
	 *  "Logical" AND of SeqSymmetries (relative to a particular BioSeq).
	 */
	public static final MutableSeqSymmetry intersection(SeqSymmetry symA, SeqSymmetry symB, BioSeq seq) {
		MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
		intersection(symA, symB, resultSym, seq);
		return resultSym;
	}

	/**
	 *  "Logical" AND of SeqSymmetries (relative to a particular BioSeq).
	 */
	public static final boolean intersection(SeqSymmetry symA, SeqSymmetry symB,
			MutableSeqSymmetry resultSym,
			BioSeq seq) {
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
	private static final void spanMerger(List<SeqSpan> spans, MutableSeqSymmetry resultSym) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		// will probably be smaller, but specifying an initial capacity
		//   that won't be exceeded can be more efficient
		List<SeqSpan> merged_spans = new ArrayList<SeqSpan>(spans.size());

		SeqSpan span;
		while ((span = getFirstNonNullSpan(spans)) != null) {
			MutableSeqSpan mergeSpan = mergeHelp(spans, span);
			merged_spans.add(mergeSpan);
		}
		Collections.sort(merged_spans, new SeqSpanComparator());
		for (SeqSpan mergedSpan : merged_spans) {
			MutableSingletonSeqSymmetry childSym =
				new MutableSingletonSeqSymmetry(mergedSpan.getStart(), mergedSpan.getEnd(), mergedSpan.getBioSeq());
			min = Math.min(mergedSpan.getMin(), min);
			max = Math.max(mergedSpan.getMax(), max);
			resultSym.addChild(childSym);
		}
		BioSeq seq = merged_spans.isEmpty() ? null : merged_spans.get(0).getBioSeq();
		SeqSpan resultSpan = new SimpleSeqSpan(min, max, seq);
		resultSym.addSpan(resultSpan);
	}

	private static final SeqSpan getFirstNonNullSpan(List<SeqSpan> spans) {
		for (SeqSpan span : spans) {
			if (span != null) {
				return span;
			}
		}
		return null;
	}

	private static final MutableSeqSpan mergeHelp(List<SeqSpan> spans, SeqSpan curSpan) {
		MutableSeqSpan result = new SimpleMutableSeqSpan(curSpan);
		while (mergeHelp(spans, result)) {
		}
		return result;
	}

	private static final boolean mergeHelp(List<SeqSpan> spans, MutableSeqSpan result) {
		boolean changed = false;
		int spanCount = spans.size();
		for (int i=0; i<spanCount; i++) {
			SeqSpan curSpan = spans.get(i);
			if (curSpan == null) { continue; }
			//  Specifying that union should use loose overlap...
			if (SeqUtils.union(result, curSpan, result, false)) {
				changed = true;
				spans.set(i, null);
			}
		}
		return changed;
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
	 *  Returns the first BioSeq in the SeqSymmetry that is NOT
	 *  equivalent to the given BioSeq.  (Compared using '=='.)
	 */
	public static final BioSeq getOtherSeq(SeqSymmetry sym, BioSeq seq) {
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
	 *  More general version of transformSymmetry(resultSet, SeqSymmetry[] syms, BioSeq[] seqs).
	 *  In this version, try and calculate resultSet span(s) for all BioSeqs encountered in syms,
	 *  not just the BioSeqs found in an input seqs List.
	 *
	 *  WARNING!!! GAH 5-21-2003
	 *  There seems to be a bug somewhere in transform methods that ends up sometimes getting
	 *    strand oriention of spans in non-leaf symmetries wrong.  This can be fixed by calling
	 *    trim(resultSym) after the transformation.  But _really_ need to fix this in the
	 *    transform methods themselves rather than have a post-operative fix!
	 */
	public static final boolean transformSymmetry(MutableSeqSymmetry resultSet, SeqSymmetry[] symPath) {
		for (SeqSymmetry sym : symPath) {
			if (! transformSymmetry(resultSet, sym, true)) { return false; }
		}
		return true;
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
			int resChildCount = resultSym.getChildCount();
			for (int child_index = 0; child_index < resChildCount; child_index++) {
				MutableSeqSymmetry childResSym = (MutableSeqSymmetry) resultSym.getChild(child_index);
				transformSymmetry(childResSym, mapSym, src2dst_recurse);
			}
			//
			addParentSpans(resultSym, mapSym);
			return true;
		}
		// is resultSym leaf?  yes

		// is mapSym leaf?  no
		if (src2dst_recurse
				&& mapSym.getChildCount() > 0) {
			loopThruMapSymChildren(mapSym, resultSym);
			return true;
		} // is mapSym leaf?  yes


		// GAH 2006-03-28
		// changed transformSymmetry() step3 to force _all_ spans to be trimmed to interspan, even
		//   if they are already present in result sym.  This fixes bug encountered with shallow transforms
		//   (result depth = 1, mapping depth = 1)
		// Not sure how this will affect deep transformations, but so far appears to be working properly

		int spanCount = mapSym.getSpanCount();

		// WARNING: still need to switch to using mutable SeqSpan args for efficiency

		// find a linker span first -- a span in resSym that has same BioSeq as
		//    a span in mapSym
		SeqSpan linkSpan = null;
		SeqSpan mapSpan = null;
		for (int spandex = 0; spandex < spanCount; spandex++) {
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
			//System.out.println("Warning: Can't find a linker span...");
			// what should happen???
			return false;
		}
		return transformLeafSymmetry(spanCount, mapSym, resultSym, linkSpan, mapSpan);
	}

	private static void loopThruMapSymChildren(SeqSymmetry mapSym, MutableSeqSymmetry resultSym) {
		int map_childcount = mapSym.getChildCount();
		for (int index = 0; index < map_childcount; index++) {
			SeqSymmetry map_child_sym = mapSym.getChild(index);

			// "sit still"
			// find the subset of BioSeqs that are pointed to by SeqSpans in both
			//    the map_child_sym (spanX) and the resultSym (spanY)
			// for each seqA of these BioSeqs, calculate SeqSpan spanZ, the intersection of
			//        spanX and the spanY
			//    if no intersection, keep looping
			//    if intersection exists, then add to child_resultSym
			
			MutableSeqSymmetry childResult = addIntersectionsToChildResultSyms(map_child_sym, resultSym, mapSym);
			if (childResult == null) {
				continue;
			}

			// "roll back"
			// find the subset of BioSeqs that are pointed to by a SeqSpan spanX in resultSym
			//      but not by any SeqSpan in childResult
			// for each seqA of these BioSeqs, calculate spanY by using resultSym as a mapping
			//      symmetry and childResult as the resultSet (but don't recurse...):
			// ACTUALLY, don't have to find subset -- this will happen in transformSymmetry!
			//      (which will fall through to STEP 3??)
			transformSymmetry(childResult, resultSym, false);

			// "roll forward"
			//  find the subset of BioSeqs that are pointed to by a SeqSpan spanX in
			//     subMapSym but not by any SeqSpan in childResult (subResSym)
			//  for each SeqA of these BioSeqs, calculate spanY by using subMapSym as
			//     a mapping symmetry and childResult as the resultSet (with recursion)
			// ACTUALLY, don't have to find subset -- this will happen in transformSymmetry!
			//      (which will fall through to STEP 3?? -- not sure how well this plays with
			//       the recursion...)
			transformSymmetry(childResult, map_child_sym, true);

			resultSym.addChild(childResult);
		}

		addParentSpans(resultSym, mapSym);
	}


	private static MutableSeqSymmetry addIntersectionsToChildResultSyms(SeqSymmetry map_child_sym, MutableSeqSymmetry resultSym, SeqSymmetry mapSym) {
		MutableSeqSymmetry childResult = null;
		int spanCount = map_child_sym == null ? 0 : map_child_sym.getSpanCount();
		// WARNING: still need to switch to using mutable SeqSpan args for efficiency
		for (int spandex = 0; spandex < spanCount; spandex++) {
			SeqSpan mapspan = map_child_sym.getSpan(spandex);
			BioSeq seq = mapspan.getBioSeq();
			SeqSpan respan = resultSym.getSpan(seq);
			if (respan == null) {
				continue;
			} // shouldn't really matter, since later redoing intersectSpan based
			//     on respan orientation anyway...
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
		return childResult;
	}



	private static boolean transformLeafSymmetry(int spanCount, SeqSymmetry mapSym, MutableSeqSymmetry resultSym, SeqSpan linkSpan, SeqSpan mapSpan) {
		MutableSeqSpan interSpan = null;
		for (int spandex = 0; spandex < spanCount; spandex++) {
			// GAH 5-17-2003
			// problem here when linkSpan is not contained within mapSym.getSpan(linkSpan.getBioSeq())!!!
			// transformSpan will transform _as if_ above were the case, therefore giving incorrect results
			// trying to fix by always calculating intersection: (interSpan) of
			//  interSpan = intersection(linkSpan and mapSym.getSpan(linkSpan.getBioSeq())),
			//  and doing tranform with interSpan instead of linkSpan...
			//
			if (interSpan == null) {
				interSpan = (MutableSeqSpan) SeqUtils.intersection(linkSpan, mapSpan);
				if (interSpan == null) {
					return false;
				}
				// GAH 6-12-2003
				// problem with using intersection!
				// since intersection() obliterates orientation (span returned is _always_ forward),
				//   need to set intersect_span orientation afterwards to be same as linkSpan
				//   (so it has coorect orientation regardless of behavior of intersection());
				if (linkSpan.isForward()) {
					interSpan.setDouble(interSpan.getMinDouble(), interSpan.getMaxDouble(), interSpan.getBioSeq());
				} else {
					interSpan.setDouble(interSpan.getMaxDouble(), interSpan.getMinDouble(), interSpan.getBioSeq());
				}
			}

			SeqSpan newspan = mapSym.getSpan(spandex);
			BioSeq seq = newspan.getBioSeq();
			SeqSpan respan = resultSym.getSpan(seq);

			MutableSeqSpan newResSpan = respan == null ? new MutableDoubleSeqSpan() : (MutableSeqSpan) respan;
			
			if (!transformSpan(interSpan, newResSpan, seq, mapSym)) {
				return false;
			}
			if (respan == null) {
				// only add new span if respan was not reused for transformed span
				resultSym.addSpan(newResSpan);
			}
		}
		return true;
	}


	public static final List<SeqSymmetry> getOverlappingChildren(SeqSymmetry sym, SeqSpan ospan) {
		int childcount = sym.getChildCount();
		if (childcount == 0) {
			return null;
		}
		List<SeqSymmetry> results = null;
		BioSeq oseq = ospan.getBioSeq();
		for (int i = 0; i < childcount; i++) {
			SeqSymmetry child = sym.getChild(i);
			SeqSpan cspan = child.getSpan(oseq);
			if (SeqUtils.overlap(ospan, cspan)) {
				if (results == null) {
					results = new ArrayList<SeqSymmetry>();
				}
				results.add(child);
			}
		}
		return results;
	}


// breaking out STEP 2
	protected static final void addParentSpans(MutableSeqSymmetry resultSym, SeqSymmetry mapSym) {
		int resultChildCount = resultSym.getChildCount();
		// possibly want to add another branch here if resultSym has only one child --
		//      could "collapse up" by moving any spans in child that aren't in
		//      parent up to parent, and removing child
		if (resultChildCount == 0) {
			return;
		}
		// for now, only worry about SeqSpans corresponding to (having same BioSeq as)
		//    SeqSpans in _mapSym_ (rather than subMapSyms or subResSyms)
		int mapSpanCount = mapSym.getSpanCount();
		for (int spandex = 0; spandex < mapSpanCount; spandex++) {
			SeqSpan mapSpan = mapSym.getSpan(spandex);
			BioSeq mapSeq = mapSpan.getBioSeq();
			SeqSpan resSpan = resultSym.getSpan(mapSeq);

			if (resSpan != null) {
				continue;
			}
			// if no span in resultSym with same BioSeq, then need to create one based
			//    on encompass() of childResSym spans (if there are any...)

			int forCount = 0;
			// need to use NEGATIVE_INFINITY for doubles, since Double.MIN_VALUE is really smallest
			//   _positive_ value, and may be transforming into negative coords...
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			boolean bounds_set = false;
			for (int childIndex = 0; childIndex < resultChildCount; childIndex++) {
				SeqSymmetry childResSym = resultSym.getChild(childIndex);
				SeqSpan childResSpan = childResSym.getSpan(mapSeq);
				if (childResSpan != null) {
					min = Math.min(childResSpan.getMinDouble(), min);
					max = Math.max(childResSpan.getMaxDouble(), max);
					bounds_set = true;
					if (childResSpan.isForward()) {
						forCount++;
					} else {
						forCount--;
					}
				}
			}
			if (bounds_set) {  // only add parent span if bounds were set by child span...
				addParentSpan(mapSeq, forCount, min, max, resultSym);
			}
		}
	}


	private static void addParentSpan(BioSeq mapSeq, int forCount, double min, double max, MutableSeqSymmetry resultSym) {
		// only add parent span if bounds were set by child span...
		MutableSeqSpan newResSpan = new MutableDoubleSeqSpan();
		newResSpan.setBioSeq(mapSeq);
		if (forCount >= 0) {
			// new result span should be forward
			newResSpan.setStartDouble(min);
			newResSpan.setEndDouble(max);
		} else {
			// new result span should be reverse
			newResSpan.setStartDouble(max);
			newResSpan.setEndDouble(min);
		}
		resultSym.addSpan(newResSpan);
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
public static final boolean transformSpan(SeqSpan srcSpan, MutableSeqSpan dstSpan,
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
		return false;
	}

	dstSpan.setBioSeq(dstSeq);

	boolean opposite_spans = (span1.isForward() ^ span2.isForward());
	boolean resultForward = opposite_spans ^ srcSpan.isForward();
	double scale = span2.getLengthDouble() / span1.getLengthDouble();

	double vstart, vend;
	
	if (opposite_spans) {
		vstart = (scale * (span1.getStartDouble() - srcSpan.getStartDouble())) + span2.getStartDouble();
		vend = (scale * (span1.getEndDouble() - srcSpan.getEndDouble())) + span2.getEndDouble();
	} else {
		vstart = (scale * (srcSpan.getStartDouble() - span1.getStartDouble())) + span2.getStartDouble();
		vend = (scale * (srcSpan.getEndDouble() - span1.getEndDouble())) + span2.getEndDouble();
	}
	if (resultForward) {
		dstSpan.setStartDouble(Math.min(vstart, vend));
		dstSpan.setEndDouble(Math.max(vstart, vend));
	} else {
		dstSpan.setStartDouble(Math.max(vstart, vend));
		dstSpan.setEndDouble(Math.min(vstart, vend));
	}

	return true;

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
	}
	return BMin <= AMax;
}

/**
 *    Exactly like looseOverlap(spanA, spanB), except exact abutment is not considered overlap.
 *    (&gt; and &lt; rather than &gt;= and &lt;= used for comparisons...).
 */
private static final boolean strictOverlap(SeqSpan spanA, SeqSpan spanB) {
	double AMin = spanA.getMinDouble();
	double BMin = spanB.getMinDouble();
	if (AMin >= BMin) {
		double BMax = spanB.getMaxDouble();
		return AMin < BMax;
	}
	double AMax = spanA.getMaxDouble();
	return BMin < AMax;
}

private static final boolean strictOverlap(double AMin, double AMax, double BMin, double BMax) {
	if (AMin >= BMin) {
		return AMin < BMax;
	}
	return BMin < AMax;
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
private static final SeqSpan intersection(SeqSpan spanA, SeqSpan spanB) {
	if (! (overlap(spanA, spanB))) { return null; }
	MutableSeqSpan dstSpan = new MutableDoubleSeqSpan();
	if (intersection(spanA, spanB, dstSpan)) {
		return dstSpan;
	}
	return null;
}

/**
 *  Returns the resulting span in dstSpan, and returns true if
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
	encompass(AForward, AMin, AMax, BMin, BMax, spanA.getBioSeq(), dstSpan);
	return true;
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
public static final boolean encompass(SeqSpan spanA, SeqSpan spanB, MutableSeqSpan dstSpan) {
	if (spanA.getBioSeq() != spanB.getBioSeq()) { return false; }
	boolean AForward = spanA.isForward();
	boolean BForward = spanB.isForward();
	double start, end;
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

private static final void encompass(boolean AForward,
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
}

public static final DerivedSeqSymmetry copyToDerived(SeqSymmetry sym) {
	DerivedSeqSymmetry der = new SimpleDerivedSeqSymmetry();
	copyToDerived(sym, der);
	return der;
}

private static final void copyToDerived(SeqSymmetry sym, DerivedSeqSymmetry der) {
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
		DerivedSeqSymmetry newchild = new SimpleDerivedSeqSymmetry();
		copyToDerived(child, newchild);
		der.addChild(newchild);
	}
}


public static final SeqSpan getChildBounds(SeqSymmetry parent, BioSeq seq) {
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
			if (for_count >= rev_count) {
				cbSpan = new SimpleSeqSpan(min, max, seq);
			}
			else {
				cbSpan = new SimpleSeqSpan(max, min, seq);
			}

	}
	return cbSpan;
}

public static final String getResidues(SeqSymmetry sym, BioSeq seq) {
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
	if (print_props && sym instanceof SymWithProps) {
		SymWithProps pp = (SymWithProps) sym;
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


/** Provides a string representation of a SeqSpan.
 *  @see #USE_SHORT_FORMAT_FOR_SPANS
 */
	public static final String spanToString(SeqSpan span) {
		if (span == null) {
			return "Span: null";
		}
		if (USE_SHORT_FORMAT_FOR_SPANS) {
			BioSeq seq = span.getBioSeq();
			return ((seq == null ? "nullseq" : seq.getID()) + ": ["
					+ span_format.format(span.getMin()) + " - " + span_format.format(span.getMax())
					+ "] ("
					+ (span.isForward() ? "+" : "-") + span_format.format(span.getLength()) + ")");
		}
		return ("Span: "
				+ "min = " + span_format.format(span.getMin())
				+ ", max = " + span_format.format(span.getMax())
				+ ", length = " + span_format.format(span.getLength())
				+ ", forward = " + span.isForward()
				+ ", seq = " + span.getBioSeq().getID() + " " + span.getBioSeq());
	}

/** Provides a string representation of a SeqSpan.
 */
public static final String symToString(SeqSymmetry sym) {
	if (sym == null) {
		return "SeqSymmetry == null";
	}

	return "sym.getID() is not implemented.";

}

}
