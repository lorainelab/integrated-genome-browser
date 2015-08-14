/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.services.synonymlookup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

/**
 *
 * @author Tarun
 */
public interface SynonymLookupService {

    /**
     * Add synonyms to thesaurus.
     *
     * @param row
     */
    void addSynonyms(Set<String> row);

    /**
     * Finds the first synonym in a list that matches the given synonym.
     * <p />
     * Case sensitive lookup of the synonym is done using the default behaviour.
     * The default case sensitive behaviour is
     * {@value #IS_CASE_SENSITIVE_SEARCH}.
     * <p />
     * Stripping '_random' from the synonym is done using the default behaviour.
     * The default strip random behaviour is
     * {@value #WILL_NOT_STRIP_RANDOM_SUFFIX}
     *
     * @param choices a list of possible synonyms that might match the given
     * synonym.
     * @param synonym the id you want to find a synonym for.
     * @return either null or a String synonym, where isSynonym(synonym,
     * synonym) is true.
     * @see #IS_CASE_SENSITIVE_SEARCH
     * @see #WILL_NOT_STRIP_RANDOM_SUFFIX
     */
    String findMatchingSynonym(Collection<String> choices, String synonym);

    /**
     * Finds the first synonym in a list that matches the given synonym.
     * <p />
     * The lookup of the synonym will be case sensitive if cs is true.
     * <p />
     * the lookup will strip '_random' from the synonyms if sr is true.
     *
     * @param choices a list of possible synonyms that might match the given
     * synonym.
     * @param synonym the id you want to find a synonym for.
     * @param cs set the case-sensitive behaviour of the synonym lookup.
     * @param sr set the strip random behaviour of the synonym lookup.
     * @return either String synonym, where isSynonym(synonym, synonym) is true
     * or the original synonym.
     */
    String findMatchingSynonym(Collection<String> choices, String synonym, boolean cs, boolean sr);

    /**
     * Find the second synonym, if it exists. Otherwise return the first.
     *
     * @param synonym
     * @return
     */
    String findSecondSynonym(String synonym);

    /**
     * Find the preferred name of the given synonym. Under the hood, this just
     * returns the first synonym in the list of available synonyms for the
     * input.
     * <p />
     * Will return the input synonym if no synonyms are known.
     * <p />
     * Case sensitive lookup of the synonym is done using the default behaviour.
     * The default case sensitive behaviour is
     * {@value #IS_CASE_SENSITIVE_SEARCH}.
     *
     * @param synonym the synonym to find the preferred name of.
     * @return the preferred name of the synonym.
     */
    String getPreferredName(String synonym);

    /**
     * Find the preferred name of the given synonym. Under the hood, this just
     * returns the first synonym in the list of available synonyms for the
     * input.
     * <p />
     * Will return the input synonym if no synonyms are known.
     * <p />
     * The lookup of the synonym will be case sensitive if cs is true.
     *
     * @param synonym the synonym to find the preferred name of.
     * @param cs set the case-sensitive behaviour of the synonym lookup.
     * @return the preferred name of the synonym.
     */
    String getPreferredName(String synonym, boolean cs);

    Set<String> getPreferredNames();

    /**
     * Return the Set of all known synonyms.
     *
     * @return Set of all known synonyms.
     */
    Set<String> getSynonyms();

    /**
     * Return all known synonyms for the given synonym. Will return an empty
     * list if the synonym is unknown.
     * <p />
     * Case sensitive lookup of the synonym is done using the default behaviour.
     * The default case sensitive behaviour is
     * {@value #IS_CASE_SENSITIVE_SEARCH}.
     *
     * @param synonym the synonym to find matching synonyms for.
     * @return the set of matching synonyms for the given synonym or an empty
     * set.
     * @see #IS_CASE_SENSITIVE_SEARCH
     */
    Set<String> getSynonyms(String synonym);

    /**
     * Return all known synonyms for the given synonym. Will return an empty
     * list if the synonym is unknown.
     * <p />
     * The lookup of the synonym will be case sensitive if cs is true.
     *
     * @param synonym the synonym to find the matching synonyms for.
     * @param cs set the case-sensitive behaviour of the synonym lookup.
     * @return the set of matching synonyms for the given synonym or an epmty
     * set.
     */
    Set<String> getSynonyms(String synonym, boolean cs);

    /**
     * Determine if two potential synonyms are synonymous using the default
     * lookup rules.
     * <p />
     * The default behaviour of case sensitivity is
     * {@value #IS_CASE_SENSITIVE_SEARCH}.
     * <p />
     * The default behaviour of strip random is {$value #DEFAULT_SR}.
     *
     * @param synonym1 the first potential synonym.
     * @param synonym2 the second potential synonym.
     * @return true if the two parameters are synonymous.
     * @see #IS_CASE_SENSITIVE_SEARCH
     * @see #WILL_NOT_STRIP_RANDOM_SUFFIX
     */
    boolean isSynonym(String synonym1, String synonym2);

    /**
     * Determine if two potential synonyms are synonymous.
     * <p />
     * The cs parameter specifies if the synonym comparison is case sensitive.
     * True if the comparison should be case sensitive, false otherwise.
     * <p />
     * The sr parameter specifies if the synonym comparison should strip
     * '_random' from the synonyms if the initial comparison is false. True if
     * random should be stripped from the potential synonyms.
     *
     * @param synonym1 the first potential synonym.
     * @param synonym2 the second potential synonym.
     * @param cs the case sensitivity of this query.
     * @param sr whether tailing '_random' of the synonyms should be stripped
     * before comparison.
     * @return true or false
     */
    boolean isSynonym(String synonym1, String synonym2, boolean cs, boolean sr);

    /**
     * Loads synonyms from the given input stream.
     *
     * @param istream the input stream to load synonyms from.
     * @throws java.io.IOException if the input stream is null or an error
     * occurs reading it.
     */
    void loadSynonyms(InputStream istream) throws IOException;

    void loadSynonyms(InputStream istream, boolean setPreferredNames) throws IOException;
    
}
