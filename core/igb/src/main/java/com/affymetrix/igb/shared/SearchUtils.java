package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.BioSeqUtils;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jnicol
 */
public final class SearchUtils {

    /**
     * Due to disagreements between group ID search and BioSeq ID search, do
     * both and combine their results.
     */
    public static List<SeqSymmetry> findLocalSyms(AnnotatedSeqGroup group, BioSeq chrFilter, Pattern regex, boolean search_props) {

        Set<SeqSymmetry> syms = new HashSet<SeqSymmetry>();
        if (search_props) {
            if (chrFilter == null) {
                group.searchProperties(syms, regex, -1);
            } else {
                chrFilter.searchProperties(syms, regex, -1);
            }
        } else {
            if (chrFilter == null) {
                group.search(syms, regex, -1);
            } else {
                chrFilter.search(syms, regex, -1);
            }
        }

//		List<BioSeq> chrs;
//		if (chrFilter != null) {
//			chrs = new ArrayList<BioSeq>();
//			chrs.add(chrFilter);
//		} else {
//			chrs = group.getSeqList();
//		}
//		
//		Matcher match = regex.matcher("");
//		SymWithProps sym = null;
//		Thread current_thread = Thread.currentThread();
//		
//		for (BioSeq chr : chrs) {
//			if(current_thread.isInterrupted())
//				break;
//			
//			int annotCount = chr.getAnnotationCount();
//			for (int i=0;i<annotCount;i++) {
//				sym = (SymWithProps)chr.getAnnotation(i);
//				findIDsInSym(syms, sym, match);
//				
//				if(current_thread.isInterrupted())
//					break;
//			}
//		}
        return new ArrayList<SeqSymmetry>(syms);
    }

    /**
     * Recursively search for symmetries that match regex.
     *
     * @param syms
     * @param sym
     * @param match
     */
    private static void findIDsInSym(Set<SeqSymmetry> syms, SeqSymmetry sym, Matcher match) {
        if (sym == null) {
            return;
        }
        if (!(sym instanceof TypeContainerAnnot)) {
            if (sym.getID() != null && match.reset(sym.getID()).matches()) {
                syms.add(sym);	// ID matches
                // If parent matches, then don't list children
                return;
            } else if (sym instanceof SymWithProps) {
                Optional<String> method = BioSeqUtils.determineMethod(sym);
                if (method.isPresent() && match.reset(method.get()).matches()) {
                    syms.add(sym);	// method matches
                    // If parent matches, then don't list children
                    return;
                }
            }
        }
        int childCount = sym.getChildCount();
        Thread current_thread = Thread.currentThread();
        for (int i = 0; i < childCount; i++) {
            if (current_thread.isInterrupted()) {
                break;
            }

            findIDsInSym(syms, sym.getChild(i), match);
        }
    }

    public static Set<String> findLocalSyms(AnnotatedSeqGroup group, Pattern regex, boolean search_props, int limit) {
        String[] props_to_search;
        Set<SeqSymmetry> syms = new HashSet<SeqSymmetry>();
        if (search_props) {
            group.searchProperties(syms, regex, limit);
            props_to_search = new String[]{"id", "gene name", "description"};
        } else {
            group.search(syms, regex, -1);
            props_to_search = new String[]{"id"};
        }

        final Matcher matcher = regex.matcher("");
        Set<String> results = new HashSet<String>(limit);
        SymWithProps swp;
        String match;
        Object value;
        for (SeqSymmetry seq : syms) {
            if (seq instanceof SymWithProps) {
                swp = (SymWithProps) seq;

                // Iterate through each properties.
                for (String prop : props_to_search) {
                    value = swp.getProperty(prop);
                    if (value != null) {
                        match = value.toString();
                        matcher.reset(match);
                        if (matcher.matches()) {
                            results.add(match);
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * Binary search that either looks for the exact key or the closest key
     *
     * @param list, list to search
     * @param key, key to search
     * @param compare, Comparator to compare key and list values
     * @return key position
     */
    public static <T> int binarySearch(List<T> list, T key, Comparator<T> compare) {
        int low, high, med, c;
        T temp;
        high = list.size();
        low = 0;
        med = (high + low) / 2;

        while (high != low + 1) {
            temp = list.get(med);
            c = compare.compare(temp, key);

            if (c == 0) {
                return med;
            } else if (c < 0) {
                low = med;
            } else {
                high = med;
            }

            med = (high + low) / 2;
        }

        return med;
    }
}
