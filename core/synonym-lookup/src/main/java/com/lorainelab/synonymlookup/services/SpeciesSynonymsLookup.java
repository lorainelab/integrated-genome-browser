/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.synonymlookup.services;

/**
 *
 * @author Tarun
 */
public interface SpeciesSynonymsLookup extends SynonymLookupService {
    
    public void load(SpeciesInfo speciesInfo);
    
    public String getCommonSpeciesName(String species);
    
    public String getSpeciesName(String version);
    
}
