/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.synonymlookup.services.impl;

import aQute.bnd.annotation.component.Component;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.lorainelab.synonymlookup.services.SpeciesSynonymsLookup;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

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

    public static final String COMPONENT_NAME = "SpeciesSynonymsLookup";

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

}
