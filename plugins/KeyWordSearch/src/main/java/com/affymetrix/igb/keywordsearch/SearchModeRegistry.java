/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.keywordsearch;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.igb.services.search.ISearchModeSym;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dcnorris
 */
@Component(name = SearchModeRegistry.COMPONENT_NAME, immediate = true)
public class SearchModeRegistry {

    public static final String COMPONENT_NAME = "SearchModeRegistry";
    private final List<ISearchModeSym> searchModes;

    public SearchModeRegistry() {
        searchModes = new ArrayList<>();
    }

    public void removeSearchModeService(ISearchModeSym searchMode) {
        searchModes.remove(searchMode);
    }

    @Reference(multiple = true, optional = true, unbind = "removeSearchModeService")
    public void addSearchModeService(ISearchModeSym searchMode) {
        searchModes.add(searchMode);
    }

    public List<ISearchModeSym> getSearchModes() {
        return searchModes;
    }
    
    
}
