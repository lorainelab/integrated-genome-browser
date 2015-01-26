package com.affymetrix.igb.shared;



import com.affymetrix.genometry.parsers.FileTypeCategory;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.affymetrix.igb.service.api.SeqSymmetryPreprocessorI;
import java.util.Collection;
import java.util.Collections;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

/**
 *
 * @author dcnorris
 */
@Component
public class PreprocessorRegistryImpl implements PreprocessorRegistry {

    private final static Table<String, FileTypeCategory, SeqSymmetryPreprocessorI> preprocessorTypeReferenceTable= HashBasedTable.create();
   
    @ServiceDependency(removed = "removePreprocessor")
    @Override
    public void addPreprocessor(SeqSymmetryPreprocessorI factory) {
        checkNotNull(factory);
        if (!preprocessorTypeReferenceTable.contains(factory.getName(), factory.getCategory())) {
            preprocessorTypeReferenceTable.put(factory.getName(), factory.getCategory(), factory);
        }
    }

    @Override
    public void removePreprocessor(SeqSymmetryPreprocessorI factory) {
        checkNotNull(factory);
        if (preprocessorTypeReferenceTable.containsValue(factory)) {
            preprocessorTypeReferenceTable.remove(factory.getName(), factory.getCategory());
        }
    }

    public static Collection<SeqSymmetryPreprocessorI> getPreprocessorsForType(FileTypeCategory category) {
        checkNotNull(category);
        if (preprocessorTypeReferenceTable.columnMap().containsKey(category)) {
            return preprocessorTypeReferenceTable.columnMap().get(category).values();
        }
        return Collections.<SeqSymmetryPreprocessorI>emptyList();
    }

}
