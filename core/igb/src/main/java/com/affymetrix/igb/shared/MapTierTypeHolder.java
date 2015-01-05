package com.affymetrix.igb.shared;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All implementation of map view mode are stored here.
 *
 * @author hiralv
 */
@Component
public class MapTierTypeHolder {

    private static final Table<String, FileTypeCategory, MapTierGlyphFactoryI> mapTierTypeReferenceTable = HashBasedTable.create();

    private static final Logger logger = LoggerFactory.getLogger(MapTierTypeHolder.class);

    @Reference(multiple = true, unbind = "removeViewFactory", dynamic = true)
    public final void addViewFactory(MapTierGlyphFactoryI factory) {
        checkNotNull(factory);
        for (FileTypeCategory category : factory.getSupportedCategories()) {
            if (!mapTierTypeReferenceTable.contains(factory.getName(), category)) {
                mapTierTypeReferenceTable.put(factory.getName(), category, factory);
            }
        }
    }

    public final void removeViewFactory(MapTierGlyphFactoryI factory) {
        checkNotNull(factory);
        if (mapTierTypeReferenceTable.containsValue(factory)) {
            for (FileTypeCategory category : factory.getSupportedCategories()) {
                mapTierTypeReferenceTable.remove(factory.getName(), category);
            }
        }
    }

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
        MapTierGlyphFactoryI factory = getDefaultFactoryFor(category);
        if (factory == null) {
            return false;
        } else {
            return factory.supportsTwoTrack();
        }
    }

}
