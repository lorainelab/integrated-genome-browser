package com.lorainelab.externalsort.api;

import java.io.File;
import java.util.List;

/**
 *
 * @author jeckstei
 */
public interface ExternalSortService {

    public List<File> sortInBatch(File file, ComparatorMetadata comparatorMetadata, ExternalSortConfiguration conf);
}
