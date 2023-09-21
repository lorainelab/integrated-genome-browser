package com.affymetrix.igb.shared;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.lorainelab.igb.services.visualization.SeqSymmetryPreprocessorI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author dcnorris
 */
@Component(name = PreprocessorRegistry.COMPONENT_NAME, immediate = true)
public class PreprocessorRegistry {

    public static final String COMPONENT_NAME = "PreprocessorRegistryImpl";
    private final static Table<String, FileTypeCategory, SeqSymmetryPreprocessorI> preprocessorTypeReferenceTable = HashBasedTable.create();

    public static Collection<SeqSymmetryPreprocessorI> getPreprocessorsForType(FileTypeCategory category) {
        checkNotNull(category);
        if (preprocessorTypeReferenceTable.columnMap().containsKey(category)) {
            return preprocessorTypeReferenceTable.columnMap().get(category).values();
        }
        return Collections.<SeqSymmetryPreprocessorI>emptyList();
    }

    @Reference( cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removePreprocessor")
    public void addPreprocessor(SeqSymmetryPreprocessorI factory) {
        checkNotNull(factory);
        if (!preprocessorTypeReferenceTable.contains(factory.getName(), factory.getCategory())) {
            preprocessorTypeReferenceTable.put(factory.getName(), factory.getCategory(), factory);
        }
    }

    public void removePreprocessor(SeqSymmetryPreprocessorI factory) {
        checkNotNull(factory);
        if (preprocessorTypeReferenceTable.containsValue(factory)) {
            preprocessorTypeReferenceTable.remove(factory.getName(), factory.getCategory());
        }
    }

}
