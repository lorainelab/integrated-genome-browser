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

    //This isn't strictly necessary, but waiting for this will prevent brief opportunity in the ui for users to click on genome icons before providers are initialized
    @Reference(target = "(&(component.name=QuickloadFactory))")
    public void trackQuickloadDataProviderFactory(DataProviderFactory quickloadFactory) {
        logger.info("QuickloadFactory now available.");
    }

    //This isn't strictly necessary, but waiting for this will prevent brief opportunity in the ui for users to click on genome icons before providers are initialized
    @Reference(target = "(&(component.name=DasDataProviderFactory))")
    public void trackDasDataProviderFactory(DataProviderFactory quickloadFactory) {
        logger.info("Das Factory now available.");
    }

    public Optional<DataProviderFactory> findFactoryByName(String factoryName) {
        return factories.stream().filter(factory -> factory.getFactoryName().equalsIgnoreCase(factoryName)).findFirst();
    }

    public Set<String> getAllAvailableFactoryTypeNames() {
        return factories.stream().map(factory -> factory.getFactoryName()).collect(Collectors.toSet());
    }

}
