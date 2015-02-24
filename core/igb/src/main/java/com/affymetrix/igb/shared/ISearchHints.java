package com.affymetrix.igb.shared;

import java.util.Set;

/**
 *
 * @author hiralv
 */
public interface ISearchHints {

    //Maximum mumber of search items
    public static int MAX_HITS = 20;

    /**
     * Searches for parameter search_term and returns a set of String
     *
     * @param search_term
     * @return
     */
    public Set<String> search(String search_term);
}
