package org.lorainelab.igb.externalsort.api;

import java.io.File;
import java.util.Optional;

/**
 *
 * @author jeckstei
 */
public interface ExternalSortService {

    public Optional<File> merge(File input, String compressionName, ComparatorMetadata comparatorMetadata, ExternalSortConfiguration conf);
}
