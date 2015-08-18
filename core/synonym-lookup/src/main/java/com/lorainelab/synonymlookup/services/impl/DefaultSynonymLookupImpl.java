/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.synonymlookup.services.impl;

import aQute.bnd.annotation.component.Component;
import com.lorainelab.synonymlookup.services.DefaultSynonymLookup;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tarun
 */
@Component(name = DefaultSynonymLookupImpl.COMPONENT_NAME, immediate = true, provide = DefaultSynonymLookup.class)
public class DefaultSynonymLookupImpl extends SynonymLookup implements DefaultSynonymLookup {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSynonymLookupImpl.class);
    public static final String COMPONENT_NAME = "DefaultSynonymLookupImpl";

    public DefaultSynonymLookupImpl() {
        InputStream resourceAsStream = DefaultSynonymLookupImpl.class.getClassLoader().getResourceAsStream("synonyms.txt");
        try {
            loadSynonyms(resourceAsStream, true);
        } catch (IOException ex) {
            logger.debug(ex.getMessage(), ex);
        }
    }

    
}
