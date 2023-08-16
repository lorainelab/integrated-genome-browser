/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.services.search;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = SearchModeRegistry.COMPONENT_NAME, immediate = true, service = SearchModeRegistry.class)
public class SearchModeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SearchModeRegistry.class);
    public static final String COMPONENT_NAME = "SearchModeRegistry";
    private static final Set<ISearchMode> searchModes = Sets.newConcurrentHashSet();

    public SearchModeRegistry() {
    }

    public void removeSearchModeService(ISearchMode searchMode) {
        searchModes.remove(searchMode);
    }

    @Reference( cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, unbind = "removeSearchModeService")
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
