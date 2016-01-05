/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.services.search;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = SearchModeRegistry.COMPONENT_NAME, immediate = true, provide = SearchModeRegistry.class)
public class SearchModeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SearchModeRegistry.class);
    public static final String COMPONENT_NAME = "SearchModeRegistry";
    private static final Set<ISearchMode> searchModes = Sets.newConcurrentHashSet();

    public SearchModeRegistry() {
    }

    public void removeSearchModeService(ISearchMode searchMode) {
        searchModes.remove(searchMode);
    }

    @Reference(multiple = true, optional = false, dynamic = true, unbind = "removeSearchModeService")
    public void addSearchModeService(ISearchMode searchMode) {
        searchModes.add(searchMode);
    }

    public static Set<ISearchMode> getSearchModes() {
        return searchModes;
    }

    public static Set<ISearchModeSym> getSearchModeSyms() {
        return searchModes.stream()
                .filter(searchMode -> searchMode instanceof ISearchModeSym)
                .map(ISearchModeSym.class::cast).collect(Collectors.toSet());
    }

}
