package com.affymetrix.genometryImpl.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

/**
 * A way of mapping synonyms to each other.
 *
 * @version $Id: SynonymLookup.java 9461 2011-11-17 21:19:39Z jfvillal $
 */
public class SynonymLookup {

	/**
	 * Default behaviour of case sensitivity for synonym lookups. If true,
	 * searches will be cases sensitive. The default is {@value}.
	 */
	private static final boolean IS_CASE_SENSITIVE_SEARCH = true;

	/**
	 * Default behaviour for stripping '_random' from synonyms. If true, the
	 * string '_random' will be removed from the end of synonyms. The default
	 * value is {@value}.
	 */
	private static final boolean WILL_NOT_STRIP_RANDOM_SUFFIX = false;

	/**
	 * The default instance of this class, used by most code for synonym
	 * lookups.
	 */
	private static final SynonymLookup DEFAULT_LOOKUP = new SynonymLookup();

	/**
	 * The default instance of this class for chromosome, used by most code for
	 * synonym lookups.
	 */
	private static final SynonymLookup CHROM_LOOKUP = new SynonymLookup();

	/**
	 * HashMultimap to map every synonym to all equivalent synonyms.
	 */
	protected final SetMultimap<String, String> thesaurus
			= Multimaps.synchronizedSetMultimap(LinkedHashMultimap.<String, String>create());

	private final Set<String> preferredNames = Sets.<String>newConcurrentHashSet();

	/**
	 * Returns the default instance of SynonymLookup. This is used to share a
	 * common SynonymLookup across the entire code.
	 *
	 * @return the default instance of SynonymLookup.
	 */
	public static SynonymLookup getDefaultLookup() {
		return DEFAULT_LOOKUP;
	}

	/**
	 * Returns the default instance of SynonymLookup. This is used to share a
	 * common SynonymLookup across the entire code.
	 *
	 * @return the default instance of chromosome SynonymLookup.
	 */
	public static SynonymLookup getChromosomeLookup() {
		return CHROM_LOOKUP;
	}

	/**
	 * Loads synonyms from the given input stream.
	 *
	 * @param istream the input stream to load synonyms from.
	 * @throws java.io.IOException if the input stream is null or an error
	 * occurs reading it.
	 */
	public void loadSynonyms(InputStream istream) throws IOException {
		this.loadSynonyms(istream, false);
	}

	public void loadSynonyms(InputStream istream, boolean setPreferredNames) throws IOException {
		Reader reader = null;
		try {
			reader = new InputStreamReader(istream);
			Iterable<CSVRecord> records = CSVFormat.TDF
					.withCommentStart('#')
					.withIgnoreSurroundingSpaces(true)
					.withIgnoreEmptyLines(true)
					.parse(reader);
			for (CSVRecord record : records) {
				//TODO: look into why size should be greater than 2
				if (record.size() >= 2) {
					if (setPreferredNames) {
						if (StringUtils.isNotEmpty(record.get(0))) {
							preferredNames.add(record.get(0));
						}
					}
					Set<String> row = Sets.<String>newConcurrentHashSet();
					for (String entry : record) {
						if (StringUtils.isNotEmpty(entry)) {
							row.add(entry);
						}
					}
					addSynonyms(row);
				}
			}
		} finally {
			GeneralUtils.safeClose(reader);
		}
	}

	/**
	 * Add synonyms to thesaurus.
	 *
	 * @param row
	 */
	public void addSynonyms(Set<String> row) {
		updateRowWithExistingSynonyms(row);
		for (String synonymCandidate : row) {
			ImmutableSet<String> previousSynonymList = ImmutableSet.<String>builder().addAll(thesaurus.get(synonymCandidate)).build();
			if (thesaurus.get(synonymCandidate).addAll(row)) {
				for (String previousSynonym : previousSynonymList) {
					thesaurus.get(previousSynonym).addAll(row);
				}
			}
		}
	}

	private void updateRowWithExistingSynonyms(Set<String> row) {
		for (String synonymCandidate : row) {
			row.addAll(thesaurus.get(synonymCandidate));
		}
	}

	/**
	 * Return the Set of all known synonyms.
	 *
	 * @return Set of all known synonyms.
	 */
	public Set<String> getSynonyms() {
		return thesaurus.keySet();
	}

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
	public Set<String> getSynonyms(String synonym) {
		return getSynonyms(synonym, IS_CASE_SENSITIVE_SEARCH);
	}

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
	public ImmutableSet<String> getSynonyms(String synonym, boolean cs) {
		if (synonym == null) {
			throw new IllegalArgumentException("str can not be null");
		}

		if (cs) {
			if (thesaurus.containsKey(synonym)) {
				Collection<String> synonyms = thesaurus.get(synonym);
				return new ImmutableSet.Builder<String>().addAll(synonyms).build();
			} else {
				return new ImmutableSet.Builder<String>().build();
			}
		} else {
			Set<String> synonyms = new LinkedHashSet<String>();

			for (String key : thesaurus.keySet()) {
				if (key.equalsIgnoreCase(synonym)) {
					synonyms.addAll(thesaurus.get(key));
				}
			}
			return new ImmutableSet.Builder<String>().addAll(synonyms).build();
		}
	}

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
	public boolean isSynonym(String synonym1, String synonym2) {
		return isSynonym(synonym1, synonym2, IS_CASE_SENSITIVE_SEARCH, WILL_NOT_STRIP_RANDOM_SUFFIX);
	}

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
	public boolean isSynonym(String synonym1, String synonym2, boolean cs, boolean sr) {
		if (synonym1 == null || synonym2 == null) {
			throw new IllegalArgumentException("synonyms can not be null");
		}

		Collection<String> synonyms = getSynonyms(synonym1, cs);
		if (sr && hasRandom(synonym1, cs) && hasRandom(synonym2, cs)) {
			return isSynonym(stripRandom(synonym1), stripRandom(synonym2), cs, sr);
		} else if (cs) {
			return synonyms.contains(synonym2);
		} else {
			for (String curstr : synonyms) {
				if (synonym2.equalsIgnoreCase(curstr)) {
					return true;
				}
			}
			return false;
		}
	}

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
	public String getPreferredName(String synonym) {
		return getPreferredName(synonym, IS_CASE_SENSITIVE_SEARCH);
	}

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
	public String getPreferredName(String synonym, boolean cs) {
		return this.findMatchingSynonym(preferredNames, synonym, cs, false);
	}

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
	public String findMatchingSynonym(Collection<String> choices, String synonym) {
		return findMatchingSynonym(choices, synonym, IS_CASE_SENSITIVE_SEARCH, WILL_NOT_STRIP_RANDOM_SUFFIX);
	}

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
	public String findMatchingSynonym(Collection<String> choices, String synonym, boolean cs, boolean sr) {
		for (String id : choices) {
			if (this.isSynonym(synonym, id, cs, sr)) {
				return id;
			}
		}
		return synonym;
	}

	/**
	 * Find the second synonym, if it exists. Otherwise return the first.
	 *
	 * @param synonym
	 * @return
	 */
	public String findSecondSynonym(String synonym) {
		Collection<String> synonymSet = thesaurus.get(synonym);
		if (synonymSet == null) {
			return synonym;
		}
		String firstSynonym = "";
		for (String id : thesaurus.get(synonym)) {
			if (firstSynonym.isEmpty()) {
				firstSynonym = id;
			} else {
				return id;	// second synonym
			}
		}
		return firstSynonym;
	}

	/**
	 * Determine if a synonym ends with '_random'. Detection will be case
	 * sensitive if cs is true.
	 *
	 * @param synonym the synonym to test for ending with '_random'.
	 * @param cs the case sensitivity of the comparison.
	 * @return true if the synonym ends with '_random'.
	 */
	private static boolean hasRandom(String synonym, boolean cs) {
		if (synonym == null) {
			throw new IllegalArgumentException("synonym can not be null");
		} else if (!cs && synonym.toLowerCase().endsWith("_random")) {
			return true;
		} else {
			return synonym.endsWith("_random");
		}
	}

	/**
	 * Strip the string '_random' from the end of the synonym. It is up to the
	 * caller to ensure that the synonym actually ends with '_random'.
	 *
	 * @param synonym the synonym to strip '_random' from.
	 * @return the synonym sans the '_random'.
	 */
	private static String stripRandom(String synonym) {
		if (!synonym.toLowerCase().endsWith("_random")) {
			throw new IllegalArgumentException("synonym must end with '_random'");
		}
		return synonym.substring(0, synonym.length() - 7);
	}
}
