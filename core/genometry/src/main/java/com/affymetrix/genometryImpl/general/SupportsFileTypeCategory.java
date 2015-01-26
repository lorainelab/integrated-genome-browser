package com.affymetrix.genometry.general;

import com.affymetrix.genometry.parsers.FileTypeCategory;

/**
 *
 * @author hiralv
 */
public interface SupportsFileTypeCategory {

    public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory);
}
