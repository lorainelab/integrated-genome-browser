package com.affymetrix.genometry.util;

import com.affymetrix.genometry.data.SpeciesInfo;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A special lookup designed for species. It is implemented using an internal
 * SynonymLookup. In the future, this may also consult the application-wide
 * SynonymLookup for unknown versions.
 *
 * @author sgblanch
 * @version $Id: SpeciesLookup.java 9976 2012-01-25 21:03:58Z dcnorris $
 */
public final class SpeciesLookup {

    private static final Logger logger = LoggerFactory.getLogger(SpeciesLookup.class);

    /**
     * Default behaviour of case sensitivity for synonym lookups. If true,
     * searches will be cases sensitive. The default is {@value}.
     */
    private static final boolean DEFAULT_CS = true;

    /**
     * Regex to find species names from versions named like G_Species_*
     */
    private static final Pattern STANDARD_REGEX = Pattern.compile("^([a-zA-Z]+_[a-zA-Z]+).*$");

    /**
     * Regex to find species named in the UCSC format. (eg. gs# or genSpe#)
     */
    private static final Pattern UCSC_REGEX = Pattern.compile("^([a-zA-Z]{2,6})[\\d]+$");

    /**
     * Regex to find species named in the UCSC format. (eg. gs# or genSpe#)
     */
    private static final Pattern NON_STANDARD_REGEX = Pattern.compile("^([a-zA-Z]+_[a-zA-Z]+_[a-zA-Z]+).*$");

    /**
     * lookup of generic species names
     */
    private static final SpeciesSynonymsLookup speciesLookup = new SpeciesSynonymsLookup();

    private static final SpeciesLookup singleton = new SpeciesLookup();

    public static SpeciesLookup getSpeciesLookup() {
        return singleton;
    }

    /**
     * Load the species file.
     *
     * @param genericSpecies InputStream of the species file.
     * @throws IOException if one of the files can not be read in.
     */
    public static void load(InputStream genericSpecies) throws IOException {
        speciesLookup.loadSynonyms(genericSpecies, true);
    }

    public static void load(SpeciesInfo speciesInfo) {
        speciesLookup.preferredNames.add(speciesInfo.getName());
        Set<String> row = Sets.newTreeSet();
        row.add(speciesInfo.getName());
        row.add(speciesInfo.getGenomeVersionNamePrefix());
        speciesLookup.addSynonyms(row);
    }

    /**
     * Return the common name of a species
     * using the default case sensitivity of this lookup.
     *
     * @return the user-friendly name of the species.
     */
    public static String getCommonSpeciesName(String species) {
        return speciesLookup.findSecondSynonym(species);
    }

    /**
     * Return the user-friendly name of a species for the given version using
     * the default case sensitivity of this lookup.
     *
     * @param version the version to find the species name of.
     * @return the user-friendly name of the species.
     */
    public static String getSpeciesName(String version) {
        return getSpeciesName(version, DEFAULT_CS);
    }

    /**
     * Return the user-friendly name of a species for the given version using
     * the specified case sensitivity.
     *
     * @param version the version to find the species name of.
     * @param cs true if this search should be case sensitive, false otherwise.
     * @return the user-friendly name of the species or the version if not found.
     */
    public static String getSpeciesName(String version, boolean cs) {
        String species = null;

        /* check to see if the synonym exists in the lookup */
        species = speciesLookup.getPreferredName(version, cs);

        /* attempt to decode standard format (G_species_*) */
        if (species.equals(version)) {
            species = getSpeciesName(version, STANDARD_REGEX, cs);
        }

//		/* attempt to decode standard format (G_species_XXX_*) */
        if (species == null) {
            species = getSpeciesName(version, NON_STANDARD_REGEX, cs);
        }

        /* attempt to decode UCSC format (gs# or genSpe#) */
        if (species == null) {
            species = getSpeciesName(version, UCSC_REGEX, cs);
        }

        /* I believe that this will always return version, but... */
        if (species == null) {
            species = speciesLookup.getPreferredName(version, cs);
        }

        //Added by Max
		/*
         String[] nameParts = species.split("_");
         if(nameParts.length == 5) {
         StringBuilder sb = new StringBuilder( nameParts[0]);
         species = sb.append("_").append(nameParts[1]).append("_").append(nameParts[2]).toString();
         } else if(nameParts.length == 1) {
         species = species.substring(0, (species.length() - 1));
         } else {
         StringBuilder sb = new StringBuilder( nameParts[0]);
         species = sb.append("_").append(nameParts[1]).toString();
         }
         */
        Pattern pattern = Pattern.compile("(\\S+)(?>_(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)_\\d{4})");
        Matcher m = pattern.matcher(species);
        if (m.find()) {
            species = m.group(1);
        } else {
            pattern = Pattern.compile("^([a-zA-Z]{2,6})[\\d]+$");
            m = pattern.matcher(species);
            if (m.find()) {
                species = species.replaceAll("[\\d]+", "");
            }
        }

        pattern = Pattern.compile("([a-zA-Z]+)((_[a-zA-Z]+)+)");
        m = pattern.matcher(species);
        if (m.find()) {
            species = m.group(1).toUpperCase() + m.group(2).toLowerCase();
        }
        //end of adding

        return species;
    }

    /**
     * Attempts to find the correct species name for the given version using the
     * specified regex to normalize the version into a generic species. Will
     * return null if the user-friendly name of the species was not found.
     *
     * @param version the version to find the species name of.
     * @param regex the Pattern to use to normalize the version into a species.
     * @param cs true if this search should be case sensitive, false otherwise.
     * @return the user-friendly name of the species or null.
     */
    private static String getSpeciesName(String version, Pattern regex, boolean cs) {
        Matcher matcher = regex.matcher(version);
        String matched = null;
        if (matcher.matches()) {
            matched = matcher.group(1);
        }

        if (matched == null || matched.isEmpty()) {
            return null;
        }

        String preferred = speciesLookup.getPreferredName(matched, cs);

        if (matched.equals(preferred)) {
            return null;
        } else {
            return preferred;
        }
    }

    public static boolean isSynonym(String synonym1, String synonym2) {
        return speciesLookup.isSynonym(synonym1, synonym2);
    }

    public static String getStandardName(String version) {
        Set<Pattern> patterns = new HashSet<>();
        patterns.add(STANDARD_REGEX);
        patterns.add(UCSC_REGEX);

        Matcher matcher;
        for (Pattern pattern : patterns) {
            matcher = pattern.matcher(version);
            if (matcher.matches()) {
                return formatSpeciesName(pattern, matcher.group(1));
            }
        }

        version = version.trim().replaceAll("\\s+", "_");

        return version;
    }

    public static Set<String> getAllSpeciesName() {
        return speciesLookup.getSynonyms();
    }

    public static String getPreferredName(String name) {
        return speciesLookup.getPreferredName(name);
    }

    private static String formatSpeciesName(Pattern pattern, String species) {
        if (STANDARD_REGEX.equals(pattern)) {
            return species.substring(0, 1).toUpperCase() + species.substring(1, species.length()).toLowerCase();
        }

        return species.toLowerCase();
    }
}
