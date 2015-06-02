package com.affymetrix.genometry.data;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = DataProviderFactoryManager.COMPONENT_NAME, immediate = true, provide = DataProviderFactoryManager.class)
public class DataProviderFactoryManager {

    public static final String COMPONENT_NAME = "DataProviderFactoryManager";
    private static final Logger logger = LoggerFactory.getLogger(DataProviderFactoryManager.class);

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

    @Reference(target = "(&(component.name=QuickloadFactory))")
    public void trackQuickloadDataProviderFactory(DataProviderFactory quickloadFactory) {
        logger.debug("QuickloadFactory now available.");
    }

    @Reference(target = "(&(component.name=DasDataProviderFactory))")
    public void trackDasDataProviderFactory(DataProviderFactory quickloadFactory) {
        logger.debug("Das Factory now available.");
    }

    @Reference(target = "(&(component.name=Das2DataProviderFactory))")
    public void trackDas2DataProviderFactory(DataProviderFactory quickloadFactory) {
        logger.debug("Das2 Factory now available.");
    }

    public Optional<DataProviderFactory> findFactoryByName(String factoryName) {
        return factories.stream().filter(factory -> factory.getFactoryName().equalsIgnoreCase(factoryName)).findFirst();
    }

    public Set<String> getAllAvailableFactoryTypeNames() {
        return factories.stream().map(factory -> factory.getFactoryName()).collect(Collectors.toSet());
    }

}
