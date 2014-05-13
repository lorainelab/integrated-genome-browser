package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;

/**
 *
 * @author hiralv
 */
public interface SupportsFileTypeCategory {
	
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory);
}
