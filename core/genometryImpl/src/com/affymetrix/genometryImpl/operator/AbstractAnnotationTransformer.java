
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;

/**
 *
 * @author hiralv
 */
public abstract class AbstractAnnotationTransformer implements Operator {
	private final FileTypeCategory fileTypeCategory;

	public AbstractAnnotationTransformer(FileTypeCategory fileTypeCategory) {
		this.fileTypeCategory = fileTypeCategory;
	}
	
	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == this.fileTypeCategory ? 1 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == this.fileTypeCategory ? 1 : 0;
	}

	@Override
	public java.util.Map<String, Class<?>> getParameters() {
		return null;
	}

	@Override
	public boolean setParameters(java.util.Map<String, Object> obj) {
		return false;
	}

	@Override
	public boolean supportsTwoTrack() {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Annotation;
	}
	
}
