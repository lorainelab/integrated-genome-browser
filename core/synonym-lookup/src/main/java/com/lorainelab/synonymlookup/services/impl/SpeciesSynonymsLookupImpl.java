/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.synonymlookup.services.impl;

import aQute.bnd.annotation.component.Component;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.lorainelab.igb.synonymlookup.services.SpeciesInfo;
import org.lorainelab.igb.synonymlookup.services.SpeciesSynonymsLookup;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * The mapping of a species to its common name is a one-to-one mapping. There should not be any discrepancy among
 * institutions on what the common name of a species is
 * <br>
 * This class inherits {@link SynonymLookup } to modify one method.
 * <br>
 * The modification is done to ensure there is only one synonym common species name for each species
 *
 * the species.txt file is of the form [scientific name][tab][common
 * nmae][tab][genome_version_name_1][genome_version_name_2]
 *
 * The first data source read determine the final value for the common name. other data sources (quickload at this time)
 * can add genome_version_names that map to the common name, but cannot modify the common name. IGB's resources
 * species.txt gets first dibs.
 *
 *
 * @author jfvillal
 * @author dcnorris
 *
 */
@Component(name = SpeciesSynonymsLookupImpl.COMPONENT_NAME, immediate = true, provide = SpeciesSynonymsLookup.class)
public class SpeciesSynonymsLookupImpl extends SynonymLookup implements SpeciesSynonymsLookup {

    private static final Logger logger = LoggerFactory.getLogger(SpeciesSynonymsLookupImpl.class);

    public static final String COMPONENT_NAME = "SpeciesSynonymsLookup";
    
    private static final boolean DEFAULT_CS = true;
    private static final Pattern STANDARD_REGEX = Pattern.compile("^([a-zA-Z]+_[a-zA-Z]+).*$");
    private static final Pattern UCSC_REGEX = Pattern.compile("^([a-zA-Z]{2,6})[\\d]+$");
    private static final Pattern NON_STANDARD_REGEX = Pattern.compile("^([a-zA-Z]+_[a-zA-Z]+_[a-zA-Z]+).*$");

    private static final String SPECIES_SYNONYM_FILE = "species.txt";

    public SpeciesSynonymsLookupImpl() {
        InputStream resourceAsStream = SpeciesSynonymsLookupImpl.class.getClassLoader().getResourceAsStream(SPECIES_SYNONYM_FILE);
        try {
            loadSynonyms(resourceAsStream, true);
        } catch (IOException ex) {
            logger.debug(ex.getMessage(), ex);
        }
    }

    /**
     * addSynonyms while making sure only one synonym exists for each species.
     *
     * @param row
     */
    @Override
    public synchronized void addSynonyms(Set<String> row) {
        //we don't allow more than one common name.
        //we reject the common name if we  already have one.
        Set<String> synonymList = Sets.newLinkedHashSet();

        String common_name = row.iterator().next();
        Collection<String> values = thesaurus.get(common_name);
        if (!values.isEmpty()) {
            //this means we have common name from a previous species.txt
            //but we are still interested in the genome_versions_that can point to this
            //common name
            //so we allow the process to continue, just make sure the common name is not included
            //whether it is the same or not.
            synonymList.addAll(values);
            Iterator<String> itr = row.iterator();
            if (itr.hasNext()) {
                itr.next();
                while (itr.hasNext()) {
                    String entry = itr.next();
                    if (StringUtils.isNotBlank(entry)) {
                        synonymList.add(entry);
                    }
                }
            }
        } else {
            synonymList = Sets.newLinkedHashSet();
            for (String entry : row) {
                synonymList.add(entry);
            }
        }

        for (String synonymCandidate : row) {
            ImmutableSet<String> previousSynonymList = ImmutableSet.<String>builder().addAll(thesaurus.get(synonymCandidate)).build();
            if (thesaurus.get(synonymCandidate).addAll(row)) {
                for (String previousSynonym : previousSynonymList) {
                    thesaurus.get(previousSynonym).addAll(synonymList);
                }
            }
        }
    }

    @Override
    public String getCommonSpeciesName(String species) {
        return findSecondSynonym(species);
    }

    @Override
    public String getSpeciesName(String version) {
        return getSpeciesName(version, DEFAULT_CS);
    }

    @Override
    public void load(SpeciesInfo speciesInfo) {
        getPreferredNames().add(speciesInfo.getName());
        Set<String> row = Sets.newLinkedHashSet();
        row.add(speciesInfo.getName());
        row.add(speciesInfo.getCommonName());
        row.add(speciesInfo.getGenomeVersionNamePrefix());
        addSynonyms(row);
    }

    private String getSpeciesName(String version, boolean cs) {
        String species;

        /* check to see if the synonym exists in the lookup */
        species = getPreferredName(version, cs);

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
            species = getPreferredName(version, cs);
        }

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

        pattern = Pattern.compile("([a-zA-Z]+)((_[a-zA-Z]+).*)");
        m = pattern.matcher(species);
        if (m.find()) {
            species = m.group(1).toUpperCase() + m.group(2).toLowerCase();
        }
        //end of adding
        species = getPreferredName(species, false);
        return species;
    }
    
    private String getSpeciesName(String version, Pattern regex, boolean cs) {
        Matcher matcher = regex.matcher(version);
        String matched = null;
        if (matcher.matches()) {
            matched = matcher.group(1);
        }

        if (matched == null || matched.isEmpty()) {
            return null;
        }

        String preferred = getPreferredName(matched, cs);

        if (matched.equals(preferred)) {
            return null;
        } else {
            return preferred;
        }
    }
    
}
