/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
/**
 * 
 * The mapping of a species to its common name is a one-to-one mapping.  There should not be
 * any discrepancy among institutions on what the common name of a species is 
 * <br>
 * This class inherits {@link SynonymLookup } to modify one method.
 * <br>
 * The modification is done to ensure there is only one synonym common species name for each species
 * 
 * the species.txt file is of the form
 * [scientific name][tab][common nmae][tab][genome_version_name_1][genome_version_name_2]
 * 
 * The first data source read determine the final value for the common name.  other 
 * data sources (quickload at this time) can add genome_version_names that map to 
 * the common name, but cannot modify the common name.  IGB's resources species.txt gets
 * first dibs.
 * 
 * 
 * @author jfvillal
 *
 */
public class SpeciesSynonymsLookup extends SynonymLookup {
	/**
	 * addSynonyms while making sure only one synonym exists for each species.
	 */
	@Override
	public synchronized void addSynonyms(String[] syns) {
		//we don't allow more than one common name.
		//we reject the common name if we  already have one.
		//wheather is the same (no typos) or different.
		Set<String> synonymList = null;
		String common_name = syns[0];
		Set<String> values = lookupHash.get( common_name );
		if( values != null){
			//this means we have common name from a previous species.txt
			//but we are still interested in the genome_versions_that can point to this 
			//common name 
			//so we allow the process to continue, just make sure the common name is not included
			//whether it is the same or not.
			synonymList = values;
			for( int i =0; i < syns.length;i++){
				if( i != 1){
					synonymList.add(syns[i]);
				}
			}
		}else{
			synonymList = new LinkedHashSet<String>(Arrays.asList(syns));
		}
		//from here down the same as parent class.
		Set<String> previousSynonymList;

		for (String newSynonym : syns) {
			if (newSynonym == null) {
				continue;
			}
			newSynonym = newSynonym.trim();
			if (newSynonym.length() == 0) {
				continue;
			}
			previousSynonymList = lookupHash.put(newSynonym, synonymList);

			if (previousSynonymList != null) {
				for (String existingSynonym : previousSynonymList) {
					if (synonymList.add(existingSynonym)) {
						// update lookupHash if existing synonym not
						// already in synonym list.
						lookupHash.put(existingSynonym, synonymList);
					}
				}
			}
		}
	}

}
