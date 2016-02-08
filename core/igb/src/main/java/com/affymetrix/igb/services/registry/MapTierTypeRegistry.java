package com.affymetrix.igb.services.registry;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.igb.view.factories.MapTierGlyphFactoryI;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class MapTierTypeRegistry {

    private static final Table<String, FileTypeCategory, MapTierGlyphFactoryI> mapTierTypeReferenceTable = HashBasedTable.create();

    private static final Logger logger = LoggerFactory.getLogger(MapTierTypeRegistry.class);

    public static Collection<MapTierGlyphFactoryI> getPreprocessorsForType(FileTypeCategory category) {
        checkNotNull(category);
        if (mapTierTypeReferenceTable.columnMap().containsKey(category)) {
            return mapTierTypeReferenceTable.columnMap().get(category).values();
        }
        return Collections.<MapTierGlyphFactoryI>emptyList();
    }

    //TODO: remove this method when there is time to refactor
    public static MapTierGlyphFactoryI getDefaultFactoryFor(FileTypeCategory category) {
        //assume only 1 since this was the existing implementation
        for (MapTierGlyphFactoryI factory : getPreprocessorsForType(category)) {
            return factory;
        }
        logger.error("No factory registered with FileTypeCategory {}", category);
        throw new IllegalStateException("No factory registered for provided category");
    }

    //TODO: remove this method when there is time to refactor
    public static boolean supportsTwoTrack(FileTypeCategory category) {
        checkNotNull(category);
        MapTierGlyphFactoryI factory = getDefaultFactoryFor(category);
        if (factory == null) {
            return false;
        } else {
            return factory.supportsTwoTrack();
        }
    }

    @Reference(multiple = true, unbind = "removeViewFactory", dynamic = true)
    public void addViewFactory(MapTierGlyphFactoryI factory) {
        checkNotNull(factory);
        factory.getSupportedCategories().stream().filter(category -> !mapTierTypeReferenceTable.contains(factory.getName(), category)).forEach(category -> {
            mapTierTypeReferenceTable.put(factory.getName(), category, factory);
        });
    }

    public void removeViewFactory(MapTierGlyphFactoryI factory) {
        checkNotNull(factory);
        if (mapTierTypeReferenceTable.containsValue(factory)) {
            for (FileTypeCategory category : factory.getSupportedCategories()) {
                mapTierTypeReferenceTable.remove(factory.getName(), category);
            }
        }
    }

}
