/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry;

import aQute.bnd.annotation.component.Reference;
import com.lorainelab.igb.synonymlookup.services.ChromosomeSynonymLookup;
import com.lorainelab.igb.synonymlookup.services.DefaultSynonymLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tarun
 */
public class GenometryServiceDependencyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(GenometryServiceDependencyManager.class);
    
    @Reference
    public void trackDefaultSynonymLookupService(DefaultSynonymLookup defSynLookup) {
        logger.debug("DefaultSynonymLookup instantiated");
    }
    
    public void trackChromosomeSynonymLookupService(ChromosomeSynonymLookup chrSynLookup) {
        logger.debug("ChromosomeSynonymLookup instantiated");
    }
}
