package com.affymetrix.genometry.data;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author dcnorris
 */
@Component(name = DataProviderFactoryManager.COMPONENT_NAME, immediate = true, provide = DataProviderFactoryManager.class)
public class DataProviderFactoryManager {

    public static final String COMPONENT_NAME = "DataProviderFactoryManager";

    public final Set<DataProviderFactory> factories;

    public DataProviderFactoryManager() {
        factories = Sets.newConcurrentHashSet();
    }

    @Reference(optional = false, multiple = true, unbind = "removeDataProviderFactory", dynamic = true)
    public void addDataProviderFactory(DataProviderFactory factory) {
        factories.add(factory);
    }

    public void removeDataProviderFactory(DataProviderFactory factory) {
        factories.remove(factory);
    }

    public Optional<DataProviderFactory> findFactoryByName(String factoryName) {
        return factories.stream().filter(factory -> factory.getFactoryName().equalsIgnoreCase(factoryName)).findFirst();
    }

    public Set<String> getAllAvailableFactoryTypeNames() {
        return factories.stream().map(factory -> factory.getFactoryName()).collect(Collectors.toSet());
    }

}
